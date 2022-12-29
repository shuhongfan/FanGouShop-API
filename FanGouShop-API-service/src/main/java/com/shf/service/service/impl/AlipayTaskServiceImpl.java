package com.shf.service.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import com.shf.common.constants.Constants;
import com.shf.common.constants.TaskConstants;
import com.shf.common.exception.CrmebException;
import com.shf.common.model.combination.StoreCombination;
import com.shf.common.model.combination.StorePink;
import com.shf.common.model.order.StoreOrder;
import com.shf.common.model.user.User;
import com.shf.common.utils.RedisUtil;
import com.shf.service.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Date;

@Service
public class AlipayTaskServiceImpl implements AlipayTaskService {

    @Autowired
    private AliPayService aliPayService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private StoreOrderService storeOrderService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private UserService userService;

    @Autowired
    private StorePinkService storePinkService;

    @Autowired
    private StoreCombinationService storeCombinationService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void queryAliPayResult() {
        String redisKey = Constants.ORDER_AUTO_CANCEL_KEY;
        Long size = redisUtil.getListSize(redisKey);
        if (size < 1) {
            return;
        }
        for (int i = 0; i < size; i++) {
            Object data = redisUtil.getRightPop(redisKey, 10L);
            String outTradeNo = data.toString();
            if (null == data) {
                continue;
            }
            try {
                Boolean isSuccess = aliPayService.queryAliPayResult(data.toString());
                if (isSuccess) {
                    StoreOrder order = storeOrderService.getByOderId(outTradeNo);
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
                }
                if (!isSuccess) {
                    redisUtil.lPush(redisKey, data);
                }
            } catch (Exception e) {
                e.printStackTrace();
                redisUtil.lPush(redisKey, data);
            }
        }
    }
}
