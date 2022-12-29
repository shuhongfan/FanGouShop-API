package com.shf.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shf.common.request.PageParamRequest;
import com.shf.common.vo.UserFundsMonitor;
import com.github.pagehelper.PageInfo;
import com.shf.common.request.BrokerageRecordRequest;
import com.shf.common.model.user.UserBrokerageRecord;

/**
*  UserRechargeService 接口
*/
public interface UserFundsMonitorService extends IService<UserFundsMonitor> {

    /**
     * 佣金记录
     * @param request 筛选条件
     * @param pageParamRequest 分页参数
     * @return PageInfo
     */
    PageInfo<UserBrokerageRecord> getBrokerageRecord(BrokerageRecordRequest request, PageParamRequest pageParamRequest);
}
