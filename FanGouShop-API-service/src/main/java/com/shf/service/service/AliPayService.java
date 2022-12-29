package com.shf.service.service;

import java.util.Map;

public interface AliPayService {
    /**
     * 支付回调
     * @param params
     * @return
     */
    String tradeNotify(Map<String, String> params);

    /**
     * 退款
     * @param orderNo
     */
    Boolean tradeRefund(String orderNo);

    /**
     * 查询支付宝支付结果
     * @param orderNo
     */
    Boolean queryAliPayResult(String orderNo);
}
