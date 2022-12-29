package com.shf.service.service;

import com.shf.common.model.order.StoreOrder;
import com.shf.common.request.OrderPayRequest;
import com.shf.common.response.OrderPayResultResponse;

/**
 * 订单支付
 */
public interface OrderPayService{

    /**
     * 支付成功处理
     * @param storeOrder 订单
     */
    Boolean paySuccess(StoreOrder storeOrder);

    /**
     * 订单支付
     * @param orderPayRequest 支付参数
     * @param ip    ip
     * @return OrderPayResultResponse
     */
    OrderPayResultResponse payment(OrderPayRequest orderPayRequest, String ip);
}
