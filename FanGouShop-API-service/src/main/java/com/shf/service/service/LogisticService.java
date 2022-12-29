package com.shf.service.service;


import com.shf.common.vo.LogisticsResultVo;

/**
* ExpressService 接口
*/
public interface LogisticService {
    LogisticsResultVo info(String expressNo, String type, String com, String phone);
}
