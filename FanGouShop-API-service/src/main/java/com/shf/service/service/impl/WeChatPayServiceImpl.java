package com.shf.service.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.shf.common.constants.Constants;
import com.shf.common.constants.PayConstants;
import com.shf.common.constants.TaskConstants;
import com.shf.common.exception.CrmebException;
import com.shf.common.model.combination.StoreCombination;
import com.shf.common.model.combination.StorePink;
import com.shf.common.model.finance.UserRecharge;
import com.shf.common.model.order.StoreOrder;
import com.shf.common.model.user.User;
import com.shf.common.model.user.UserToken;
import com.shf.common.model.wechat.WechatPayInfo;
import com.shf.common.utils.*;
import com.shf.common.vo.*;
import com.shf.service.service.*;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;


/**
 * 微信支付
 */
@Data
@Service
public class WeChatPayServiceImpl implements WeChatPayService {
    private static final Logger logger = LoggerFactory.getLogger(WeChatPayServiceImpl.class);

    @Autowired
    private RestTemplateUtil restTemplateUtil;

    @Autowired
    private UserTokenService userTokenService;

    @Autowired
    private StoreOrderService storeOrderService;

    @Autowired
    private StoreOrderInfoService storeOrderInfoService;

    @Autowired
    private SystemConfigService systemConfigService;

    private String signKey;

    private CreateOrderRequestVo createOrderRequestVo;

    private CreateOrderResponseVo createOrderResponseVo = null;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRechargeService userRechargeService;

    @Autowired
    private RechargePayService rechargePayService;

    @Autowired
    private StoreCombinationService storeCombinationService;

    @Autowired
    private StorePinkService storePinkService;

    @Autowired
    private WechatNewService wechatNewService;

    @Autowired
    private WechatPayInfoService wechatPayInfoService;

    /**
     * 查询支付结果
     *
     * @param orderNo 订单编号
     * @return
     */
    @Override
    public Boolean queryPayResult(String orderNo) {
        if (StrUtil.isBlank(orderNo)) {
            throw new CrmebException("订单编号不能为空");
        }
        // 切割字符串，判断是支付订单还是充值订单
        String pre = StrUtil.subPre(orderNo, 5);
        if (pre.equals("order")) {// 支付订单
            StoreOrder storeOrder = storeOrderService.getByOderId(orderNo);
            if (ObjectUtil.isNull(storeOrder)) {
                throw new CrmebException("订单不存在");
            }
            if (storeOrder.getIsDel()) {
                throw new CrmebException("订单已被删除");
            }
            if (!storeOrder.getPayType().equals(PayConstants.PAY_TYPE_WE_CHAT)) {
                throw new CrmebException("不是微信支付类型订单，请重新选择支付方式");
            }

            if (storeOrder.getPaid()) {
                return Boolean.TRUE;
            }

            WechatPayInfo wechatPayInfo = wechatPayInfoService.getByNo(storeOrder.getOutTradeNo());
            if (ObjectUtil.isNull(wechatPayInfo)) {
                throw new CrmebException("未找到对应微信订单");
            }

            User user = userService.getById(storeOrder.getUid());
            if (ObjectUtil.isNull(user)) throw new CrmebException("用户不存在");


            // 获取appid、mch_id
            // 微信签名key
            String appId = "";
            String mchId = "";
            String signKey = "";
            if (storeOrder.getIsChannel() == 0) {// 公众号
                appId = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_WE_CHAT_APP_ID);
                mchId = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_WE_CHAT_MCH_ID);
                signKey = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_WE_CHAT_APP_KEY);
            }
            if (storeOrder.getIsChannel() == 1) {// 小程序
                appId = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_ROUTINE_APP_ID);
                mchId = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_ROUTINE_MCH_ID);
                signKey = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_ROUTINE_APP_KEY);
            }
            if (storeOrder.getIsChannel() == 2) {// H5
                appId = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_WE_CHAT_APP_ID);
                mchId = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_WE_CHAT_MCH_ID);
                signKey = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_WE_CHAT_APP_KEY);
            }

            // 生成查询订单对象
            Map<String, String> payVo = getWxChantQueryPayVo(wechatPayInfo.getOutTradeNo(), appId, mchId, signKey);
            // 查询订单信息
            MyRecord record = wechatNewService.payOrderQuery(payVo);

            wechatPayInfo.setIsSubscribe(record.getStr("is_subscribe"));
            wechatPayInfo.setTradeState(record.getStr("trade_state"));
            wechatPayInfo.setBankType(record.getStr("bank_type"));
            wechatPayInfo.setCashFee(record.getInt("cash_fee"));
            wechatPayInfo.setCouponFee(record.getInt("coupon_fee"));
            wechatPayInfo.setTransactionId(record.getStr("transaction_id"));
            wechatPayInfo.setTimeEnd(record.getStr("time_end"));
            wechatPayInfo.setTradeStateDesc(record.getStr("trade_state_desc"));

            Boolean updatePaid = transactionTemplate.execute(e -> {
                storeOrderService.updatePaid(orderNo);
                wechatPayInfoService.updateById(wechatPayInfo);
                if (storeOrder.getUseIntegral() > 0) {
                    userService.updateIntegral(user, storeOrder.getUseIntegral(), "sub");
                }
                // 处理拼团
                if (storeOrder.getCombinationId() > 0) {
                    // 判断拼团团长是否存在
                    StorePink headPink = new StorePink();
                    Integer pinkId = storeOrder.getPinkId();
                    if (pinkId > 0) {
                        headPink = storePinkService.getById(pinkId);
                        if (ObjectUtil.isNull(headPink) || headPink.getIsRefund().equals(true) || headPink.getStatus() == 3) {
                            pinkId = 0;
                        }
                    }
                    StoreCombination storeCombination = storeCombinationService.getById(storeOrder.getCombinationId());
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
                    storePink.setOrderId(storeOrder.getOrderId());
                    storePink.setOrderIdKey(storeOrder.getId());
                    storePink.setTotalNum(storeOrder.getTotalNum());
                    storePink.setTotalPrice(storeOrder.getTotalPrice());
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
                    storeOrder.setPinkId(storePink.getId());
                    storeOrderService.updateById(storeOrder);
                }
                return Boolean.TRUE;
            });
            if (!updatePaid) {
                throw new CrmebException("支付成功更新订单失败");
            }
            // 添加支付成功task
            redisUtil.lPush(TaskConstants.ORDER_TASK_PAY_SUCCESS_AFTER, orderNo);
            return Boolean.TRUE;
        }

        // 充值订单
        UserRecharge userRecharge = new UserRecharge();
        userRecharge.setOrderId(orderNo);
        userRecharge = userRechargeService.getInfoByEntity(userRecharge);
        if (ObjectUtil.isNull(userRecharge)) {
            throw new CrmebException("没有找到订单信息");
        }
        if (userRecharge.getPaid()) {
            return Boolean.TRUE;
        }
        // 查询订单
        // 获取appid、mch_id
        // 微信签名key
        String appId = "";
        String mchId = "";
        String signKey = "";
        if (userRecharge.getRechargeType().equals("public")) {// 公众号
            appId = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_WE_CHAT_APP_ID);
            mchId = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_WE_CHAT_MCH_ID);
            signKey = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_WE_CHAT_APP_KEY);
        }
        if (userRecharge.getRechargeType().equals("routine")) {// 小程序
            appId = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_ROUTINE_APP_ID);
            mchId = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_ROUTINE_MCH_ID);
            signKey = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_ROUTINE_APP_KEY);
        }
        // 生成查询订单对象
        Map<String, String> payVo = getWxChantQueryPayVo(orderNo, appId, mchId, signKey);
        // 查询订单信息
        MyRecord record = wechatNewService.payOrderQuery(payVo);
        // 支付成功处理
        Boolean rechargePayAfter = rechargePayService.paySuccess(userRecharge);
        if (!rechargePayAfter) {
            throw new CrmebException("wechat pay error : 数据保存失败==》" + orderNo);
        }
        return rechargePayAfter;
    }

    /**
     * 微信充值预下单接口
     *
     * @param userRecharge 充值订单
     * @param clientIp     ip
     * @return
     */
    @Override
    public Map<String, String> unifiedRecharge(UserRecharge userRecharge, String clientIp) {
        if (ObjectUtil.isNull(userRecharge)) {
            throw new CrmebException("订单不存在");
        }
        // 获取用户openId
        // 根据订单支付类型来判断获取公众号openId还是小程序openId
        UserToken userToken = new UserToken();
        if (userRecharge.getRechargeType().equals(PayConstants.PAY_CHANNEL_WE_CHAT_PUBLIC)) {// 公众号
            userToken = userTokenService.getTokenByUserId(userRecharge.getUid(), 1);
        }
        if (userRecharge.getRechargeType().equals(PayConstants.PAY_CHANNEL_WE_CHAT_PROGRAM)) {// 小程序
            userToken = userTokenService.getTokenByUserId(userRecharge.getUid(), 2);
        }
        if (userRecharge.getRechargeType().equals(PayConstants.PAY_CHANNEL_WE_CHAT_H5)) {// H5
            userToken.setToken("");
        }

        if (ObjectUtil.isNull(userToken)) {
            throw new CrmebException("该用户没有openId");
        }

        // 获取appid、mch_id
        // 微信签名key
        String appId = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_ALIPAY_APP_ID);
        String sellerId = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_ALIPAY_SELLER_ID);
        ;
        String gatewayUrl = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_ALIPAY_GATEWAY_URL);
        ;
        String merchantPrivateKey = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_ALIPAY_MERCHANT_PRIVATE_KEY);
        ;
        String publicKey = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_ALIPAY_PUBLIC_KEY);
        ;
        String contentKey = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_ALIPAY_CONTENT_KEY);
        ;
        String returnUrl = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_ALIPAY_RETURN_URL);
        ;
        String notifyUrl = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_ALIPAY_NOTIFY_URL);
        ;

//        if (userRecharge.getRechargeType().equals(PayConstants.PAY_CHANNEL_WE_CHAT_PUBLIC)) {// 公众号
//            appId = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_WE_CHAT_APP_ID);
//            mchId = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_WE_CHAT_MCH_ID);
//            signKey = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_WE_CHAT_APP_KEY);
//        }
//        if (userRecharge.getRechargeType().equals(PayConstants.PAY_CHANNEL_WE_CHAT_PROGRAM)) {// 小程序
//            appId = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_ROUTINE_APP_ID);
//            mchId = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_ROUTINE_MCH_ID);
//            signKey = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_ROUTINE_APP_KEY);
//        }
//        if (userRecharge.getRechargeType().equals(PayConstants.PAY_CHANNEL_WE_CHAT_H5)) {// H5,使用公众号的
//            appId = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_WE_CHAT_APP_ID);
//            mchId = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_WE_CHAT_MCH_ID);
//            signKey = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_WE_CHAT_APP_KEY);
//        }


        // 获取微信预下单对象
        CreateOrderRequestVo unifiedorderVo = getUnifiedorderVo(userRecharge, userToken.getToken(), clientIp, appId, sellerId, signKey);
        // 预下单
//        CreateOrderResponseVo responseVo = unifiedOrder(unifiedorderVo);

        // 组装前端预下单参数
        Map<String, String> map = new HashMap<>();
        map.put("appId", appId);
        map.put("notify-url", notifyUrl);
        map.put("out_trade_no", unifiedorderVo.getOut_trade_no());
        map.put("return-url", returnUrl);
        BigDecimal total_amount = new BigDecimal(unifiedorderVo.getTotal_fee()).divide(new BigDecimal(100));
        map.put("total_amount", String.valueOf(total_amount));
        map.put("subject", unifiedorderVo.getBody());
        map.put("product_code", "QUICK_WAP_WAY");

        Long currentTimestamp = WxPayUtil.getCurrentTimestamp();
        map.put("timeStamp", Long.toString(currentTimestamp));

        return map;
    }

    /**
     * 生成微信查询订单对象
     *
     * @return
     */
    private Map<String, String> getWxChantQueryPayVo(String orderNo, String appId, String mchId, String signKey) {
        Map<String, String> map = CollUtil.newHashMap();
        map.put("appid", appId);
        map.put("mch_id", mchId);
        map.put("out_trade_no", orderNo);
        map.put("nonce_str", WxPayUtil.getNonceStr());
        map.put("sign_type", PayConstants.WX_PAY_SIGN_TYPE_MD5);
        map.put("sign", WxPayUtil.getSign(map, signKey));
        return map;
    }

    /**
     * 获取微信预下单对象
     *
     * @return
     */
    private CreateOrderRequestVo getUnifiedorderVo(UserRecharge userRecharge, String openid, String ip, String appId, String mchId, String signKey) {

        // 获取域名
        String domain = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_SITE_URL);
        String apiDomain = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_API_URL);

        AttachVo attachVo = new AttachVo(Constants.SERVICE_PAY_TYPE_RECHARGE, userRecharge.getUid());
        CreateOrderRequestVo vo = new CreateOrderRequestVo();

        vo.setAppid(appId);
        vo.setMch_id(mchId);
        vo.setNonce_str(WxPayUtil.getNonceStr());
        vo.setSign_type(PayConstants.WX_PAY_SIGN_TYPE_MD5);
        vo.setBody(PayConstants.PAY_BODY);
        vo.setAttach(JSONObject.toJSONString(attachVo));
        vo.setOut_trade_no(userRecharge.getOrderId());
        // 订单中使用的是BigDecimal,这里要转为Integer类型
        vo.setTotal_fee(userRecharge.getPrice().multiply(BigDecimal.TEN).multiply(BigDecimal.TEN).intValue());
        vo.setSpbill_create_ip(ip);
        vo.setNotify_url(apiDomain + PayConstants.WX_PAY_NOTIFY_API_URI);
        vo.setTrade_type(PayConstants.WX_PAY_TRADE_TYPE_JS);
        vo.setOpenid(openid);
        if (userRecharge.getRechargeType().equals(PayConstants.PAY_CHANNEL_WE_CHAT_H5)) {// H5
            vo.setTrade_type(PayConstants.WX_PAY_TRADE_TYPE_H5);
            vo.setOpenid(null);
        }
        if (userRecharge.getRechargeType().equals(PayConstants.PAY_CHANNEL_WE_CHAT_APP_IOS) || userRecharge.getRechargeType().equals(PayConstants.PAY_CHANNEL_WE_CHAT_APP_ANDROID)) {
            vo.setTrade_type("APP");
            vo.setOpenid(null);
        }
        CreateOrderH5SceneInfoVo createOrderH5SceneInfoVo = new CreateOrderH5SceneInfoVo(
                new CreateOrderH5SceneInfoDetailVo(
                        domain,
                        systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_SITE_NAME)
                )
        );
        vo.setScene_info(JSONObject.toJSONString(createOrderH5SceneInfoVo));
        String sign = WxPayUtil.getSign(vo, signKey);
        vo.setSign(sign);
        return vo;
    }

    /**
     * 作用：统一下单<br>
     * 场景：公共号支付、扫码支付、APP支付
     *
     * @param vo 向wxpay post的请求数据
     * @return API返回数据
     */
    private CreateOrderResponseVo unifiedOrder(CreateOrderRequestVo vo) {
        try {
            String url = PayConstants.WX_PAY_API_URL + PayConstants.WX_PAY_API_URI;
            String request = XmlUtil.objectToXml(vo);
            String xml = restTemplateUtil.postXml(url, request);
            HashMap<String, Object> map = XmlUtil.xmlToMap(xml);
            if (null == map) {
                throw new CrmebException("微信下单失败！");
            }
            CreateOrderResponseVo responseVo = CrmebUtil.mapToObj(map, CreateOrderResponseVo.class);
            if (responseVo.getReturnCode().toUpperCase().equals("FAIL")) {
                throw new CrmebException("微信下单失败1！" + responseVo.getReturnMsg());
            }

            if (responseVo.getResultCode().toUpperCase().equals("FAIL")) {
                throw new CrmebException("微信下单失败2！" + responseVo.getErrCodeDes());
            }

            responseVo.setExtra(vo.getScene_info());
            return responseVo;
        } catch (Exception e) {
            e.printStackTrace();
            throw new CrmebException(e.getMessage());
        }
    }

}
