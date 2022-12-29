package com.shf.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shf.common.model.order.StoreOrder;
import com.shf.common.request.StoreOrderRefundRequest;


/**
 * StoreOrderRefundService 接口
 */
public interface StoreOrderRefundService extends IService<StoreOrder> {

    void refund(StoreOrderRefundRequest request, StoreOrder storeOrder);
}
