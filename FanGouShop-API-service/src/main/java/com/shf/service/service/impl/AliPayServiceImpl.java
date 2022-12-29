package com.shf.service.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.*;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shf.common.constants.Constants;
import com.shf.common.constants.TaskConstants;
import com.shf.common.exception.CrmebException;
import com.shf.common.model.combination.StoreCombination;
import com.shf.common.model.combination.StorePink;
import com.shf.common.model.finance.UserRecharge;
import com.shf.common.model.order.StoreOrder;
import com.shf.common.model.user.User;
import com.shf.common.model.user.UserBill;
import com.shf.common.utils.RedisUtil;
import com.shf.service.service.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class AliPayServiceImpl implements AliPayService {
    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private UserRechargeService userRechargeService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserBillService userBillService;

    @Autowired
    private StoreOrderService storeOrderService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private StorePinkService storePinkService;

    @Autowired
    private StoreCombinationService storeCombinationService;

    @Autowired
    private RedisUtil redisUtil;

    private final ReentrantLock lock = new ReentrantLock();


    @Override
    @Transactional
    public String tradeNotify(Map<String, String> params) {
        String appId = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_ALIPAY_APP_ID);
        String sellerId = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_ALIPAY_SELLER_ID);
        String gatewayUrl = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_ALIPAY_GATEWAY_URL);
        String merchantPrivateKey = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_ALIPAY_MERCHANT_PRIVATE_KEY);
        String publicKey = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_ALIPAY_PUBLIC_KEY);
        String contentKey = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_ALIPAY_CONTENT_KEY);
        String returnUrl = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_ALIPAY_RETURN_URL);
        String notifyUrl = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_ALIPAY_NOTIFY_URL);

        log.info("支付通知正在执行");
        log.info("通知参数 ===> {}", params);

        String result = "failure";

        /**
         * 在对业务数据进行检测和处理前，要采用锁进行并发控制，要避免函数重入导致数据混乱
         */
        if (lock.tryLock()) {
            try {
                //异步通知验签
                boolean signVerified = AlipaySignature.rsaCheckV1(
                        params,
                        publicKey,
                        AlipayConstants.CHARSET_UTF8,
                        AlipayConstants.SIGN_TYPE_RSA2); //调用SDK验证签名

                if (!signVerified) {
                    //验签失败则记录异常日志，并在response中返回failure.
                    log.error("支付成功异步通知验签失败！");
                    return result;
                }

                // 验签成功后
                log.info("支付成功异步通知验签成功！");

                //按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，
                //1 商户需要验证该通知数据中的 out_trade_no 是否为商户系统中创建的订单号
                String outTradeNo = params.get("out_trade_no");

//            充值订单
                if (!outTradeNo.startsWith("order")) {
                    QueryWrapper<UserRecharge> wrapper = new QueryWrapper<UserRecharge>().eq("order_id", outTradeNo);
                    UserRecharge order = userRechargeService.getOne(wrapper);
                    if (order == null) {
                        log.error("订单不存在");
                        return result;
                    }

                    //2 判断 total_amount 是否确实为该订单的实际金额（即商户订单创建时的金额）
                    BigDecimal totalAmount = new BigDecimal(params.get("total_amount"));
                    if (!order.getPrice().equals(totalAmount)) {
                        log.error("金额校验失败");
                        return result;
                    }

                    //3 校验通知中的 seller_id（或者 seller_email) 是否为 out_trade_no 这笔单据的对应的操作方
                    String sellId = params.get("seller_id");
                    if (!sellId.equals(sellerId)) {
                        log.error("商家pid校验失败");
                        return result;
                    }

                    //4 验证 app_id 是否为该商户本身
                    String appIdParam = params.get("app_id");
                    if (!appIdParam.equals(appId)) {
                        log.error("appid校验失败");
                        return result;
                    }

                    //在支付宝的业务通知中，只有交易通知状态为 TRADE_SUCCESS时，
                    // 支付宝才会认定为买家付款成功。
                    String tradeStatus = params.get("trade_status");
                    if (!"TRADE_SUCCESS".equals(tradeStatus)) {
                        log.error("支付未成功");
                        return result;
                    }

                    //处理业务 修改订单状态 记录支付日志

                    order.setPaid(true);
                    order.setPayTime(new Date());
                    userRechargeService.updateById(order);

                    Integer uid = order.getUid();
                    User user = userService.getById(uid);
                    user.setNowMoney(user.getNowMoney().add(totalAmount).add(order.getGivePrice()));
                    userService.updateById(user);

                    UserBill userBill = new UserBill();
                    userBill.setUid(uid);
                    userBill.setPm(1);
                    userBill.setTitle("充值" + totalAmount + ",送" + order.getGivePrice());
                    userBill.setCategory("now_money");
                    userBill.setType("充值");
                    userBill.setNumber(totalAmount);

                    BigDecimal resMoney = totalAmount.add(order.getGivePrice());
                    userBill.setBalance(resMoney);
                    userBill.setMark("");
                    userBill.setStatus(1);
                    userBill.setCreateTime(new Date());
                    userBill.setUpdateTime(new Date());
                    userBillService.insert(userBill);

                    //校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
                    result = "success";
                } else {
//                商品支付订单
                    //按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，
                    //1 商户需要验证该通知数据中的 out_trade_no 是否为商户系统中创建的订单号
                    StoreOrder order = storeOrderService.getByOderId(outTradeNo);
                    if (order == null) {
                        log.error("订单不存在");
                        return result;
                    }

                    //2 判断 total_amount 是否确实为该订单的实际金额（即商户订单创建时的金额）
                    String totalAmount = params.get("total_amount");
                    if (!new BigDecimal(totalAmount).equals(order.getPayPrice())) {
                        log.error("金额校验失败");
                        return result;
                    }

                    //3 校验通知中的 seller_id（或者 seller_email) 是否为 out_trade_no 这笔单据的对应的操作方
                    String sellId = params.get("seller_id");
                    if (!sellId.equals(sellerId)) {
                        log.error("商家pid校验失败");
                        return result;
                    }

                    //4 验证 app_id 是否为该商户本身
                    String appIdParams = params.get("app_id");
                    if (!appId.equals(appIdParams)) {
                        log.error("appid校验失败");
                        return result;
                    }

                    //在支付宝的业务通知中，只有交易通知状态为 TRADE_SUCCESS时，
                    // 支付宝才会认定为买家付款成功。
                    String tradeStatus = params.get("trade_status");
                    if (!"TRADE_SUCCESS".equals(tradeStatus)) {
                        log.error("支付未成功");
                        return result;
                    }

                    //处理业务 修改订单状态 记录支付日志
                    order.setPaid(true);
                    order.setPayTime(new Date());
                    storeOrderService.updateById(order);

                    User user = userService.getById(order.getUid());

                    Boolean updatePaid = transactionTemplate.execute(e -> {

                        if (order.getUseIntegral() > 0) {
                            userService.updateIntegral(user, order.getUseIntegral(), "sub");
                        }
                        // 处理拼团
                        if (order.getCombinationId() > 0) {
                            // 判断拼团团长是否存在
                            StorePink headPink = new StorePink();
                            Integer pinkId = order.getPinkId();
                            if (pinkId > 0) {
                                headPink = storePinkService.getById(pinkId);
                                if (ObjectUtil.isNull(headPink) || headPink.getIsRefund().equals(true) || headPink.getStatus() == 3) {
                                    pinkId = 0;
                                }
                            }
                            StoreCombination storeCombination = storeCombinationService.getById(order.getCombinationId());
                            // 如果拼团人数已满，重新开团
                            if (pinkId > 0) {
                                Integer count = storePinkService.getCountByKid(pinkId);
                                if (count >= storeCombination.getPeople()) {
                                    pinkId = 0;
                                }
                            }
                            // 生成拼团表数据
                            StorePink storePink = new StorePink();
                            storePink.setUid(user.getUid());
                            storePink.setAvatar(user.getAvatar());
                            storePink.setNickname(user.getNickname());
                            storePink.setOrderId(order.getOrderId());
                            storePink.setOrderIdKey(order.getId());
                            storePink.setTotalNum(order.getTotalNum());
                            storePink.setTotalPrice(order.getTotalPrice());
                            storePink.setCid(storeCombination.getId());
                            storePink.setPid(storeCombination.getProductId());
                            storePink.setPeople(storeCombination.getPeople());
                            storePink.setPrice(storeCombination.getPrice());
                            Integer effectiveTime = storeCombination.getEffectiveTime();// 有效小时数
                            DateTime dateTime = cn.hutool.core.date.DateUtil.date();
                            storePink.setAddTime(dateTime.getTime());
                            if (pinkId > 0) {
                                storePink.setStopTime(headPink.getStopTime());
                            } else {
                                DateTime hourTime = cn.hutool.core.date.DateUtil.offsetHour(dateTime, effectiveTime);
                                long stopTime = hourTime.getTime();
                                if (stopTime > storeCombination.getStopTime()) {
                                    stopTime = storeCombination.getStopTime();
                                }
                                storePink.setStopTime(stopTime);
                            }
                            storePink.setKId(pinkId);
                            storePink.setIsTpl(false);
                            storePink.setIsRefund(false);
                            storePink.setStatus(1);
                            storePinkService.save(storePink);
                            // 如果是开团，需要更新订单数据
                            order.setPinkId(storePink.getId());
                            storeOrderService.updateById(order);
                        }
                        return Boolean.TRUE;
                    });
                    if (!updatePaid) {
                        throw new CrmebException("支付成功更新订单失败");
                    }
                    // 添加支付成功task
                    redisUtil.lPush(TaskConstants.ORDER_TASK_PAY_SUCCESS_AFTER, order.getId());

                    //校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
                    result = "success";
                }


            } catch (AlipayApiException e) {
                throw new CrmebException(e.getMessage());
            } finally {
//                主动释放锁
                lock.unlock();
            }
        }

        return result;
    }

    /**
     * 退款
     *
     * @param orderNo
     */
    @Override
    public Boolean tradeRefund(String orderNo) {
        try {
            log.info("调用退款API");

            StoreOrder order = storeOrderService.getByOderId(orderNo);
            if (order == null) {
                throw new CrmebException("订单编号不正确");
            }

            //调用统一收单交易退款接口
            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();

            //组装当前业务方法的请求参数
            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", orderNo);//订单编号
            bizContent.put("refund_amount", order.getPayPrice());//退款金额：不能大于支付金额
            bizContent.put("refund_reason", order.getRefundReasonWap());//退款原因(可选)

            request.setBizContent(bizContent.toString());

            AlipayClient alipayClient = getAlipayClient();

            //执行请求，调用支付宝接口
            AlipayTradeRefundResponse response = alipayClient.execute(request);

            if (response.isSuccess()) {
                log.info("调用成功，返回结果 ===> " + response.getBody());

                //更新订单状态
                order.setRefundStatus(2);
                storeOrderService.updateById(order);

                //更新退款单
//                refundsInfoService.updateRefundForAliPay(
//                        refundInfo.getRefundNo(),
//                        response.getBody(),
//                        AliPayTradeState.REFUND_SUCCESS.getType()); //退款成功
                return true;
            } else {
                log.info("调用失败，返回码 ===> " + response.getCode() + ", 返回描述 ===> " + response.getMsg());
                throw new CrmebException("调用失败，返回码 ===> " + response.getCode() + ", 返回描述 ===> " + response.getMsg());
                //更新订单状态
//                orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.REFUND_ABNORMAL);

                //更新退款单
//                refundsInfoService.updateRefundForAliPay(
//                        refundInfo.getRefundNo(),
//                        response.getBody(),
//                        AliPayTradeState.REFUND_ERROR.getType()); //退款失败
            }


        } catch (AlipayApiException e) {
            throw new CrmebException("创建退款申请失败");
        }
    }

    /**
     * 查询支付宝支付结果
     * @param orderNo
     */
    @Override
    public Boolean queryAliPayResult(String orderNo) {
        try {
            log.info("查单接口调用 ===> {}", orderNo);

            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", orderNo);
            request.setBizContent(bizContent.toString());

            AlipayClient alipayClient = getAlipayClient();
            AlipayTradeQueryResponse response = alipayClient.execute(request);
            if(response.isSuccess()){
                log.info("调用成功，返回结果 ===> " + response.getBody());
                return true;
            } else {
                throw new CrmebException("调用失败，返回码 ===> " + response.getCode() + ", 返回描述 ===> " + response.getMsg());
//                return null;//订单不存在
            }

        } catch (AlipayApiException e) {
            e.printStackTrace();
            throw new RuntimeException("查单接口的调用失败");
        }
    }

    public AlipayClient getAlipayClient() {
        String appId = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_ALIPAY_APP_ID);
        String sellerId = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_ALIPAY_SELLER_ID);
        String gatewayUrl = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_ALIPAY_GATEWAY_URL);
        String merchantPrivateKey = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_ALIPAY_MERCHANT_PRIVATE_KEY);
        String publicKey = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_ALIPAY_PUBLIC_KEY);
        String contentKey = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_ALIPAY_CONTENT_KEY);
        String returnUrl = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_ALIPAY_RETURN_URL);
        String notifyUrl = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_ALIPAY_NOTIFY_URL);
        AlipayConfig alipayConfig = new AlipayConfig();
        //设置网关地址
        alipayConfig.setServerUrl(gatewayUrl);
        //设置应用Id
        alipayConfig.setAppId(appId);
        //设置应用私钥
        alipayConfig.setPrivateKey(merchantPrivateKey);
        //设置请求格式，固定值json
        alipayConfig.setFormat(AlipayConstants.FORMAT_JSON);
        //设置字符集
        alipayConfig.setCharset(AlipayConstants.CHARSET_UTF8);
        //设置支付宝公钥
        alipayConfig.setAlipayPublicKey(publicKey);
        //设置签名类型
        alipayConfig.setSignType(AlipayConstants.SIGN_TYPE_RSA2);
        //构造client
        AlipayClient alipayClient = null;
        try {
            alipayClient = new DefaultAlipayClient(alipayConfig);
        } catch (AlipayApiException e) {
            throw new RuntimeException(e);
        }
        return alipayClient;
    }
}
