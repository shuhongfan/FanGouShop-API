package com.shf.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shf.common.request.PageParamRequest;
import com.shf.common.response.UserSignInfoResponse;
import com.shf.common.vo.SystemGroupDataSignConfigVo;
import com.shf.common.model.user.UserSign;
import com.shf.common.vo.UserSignMonthVo;
import com.shf.common.vo.UserSignVo;

import java.util.HashMap;
import java.util.List;

/**
 * UserSignService 接口实现
 */
public interface UserSignService extends IService<UserSign> {

    /**
     * 用户积分列表
     * @param pageParamRequest 分页参数
     * @return List
     */
    List<UserSignVo> getList(PageParamRequest pageParamRequest);

    List<UserSign> getListByCondition(UserSign sign, PageParamRequest pageParamRequest);

    /**
     * 签到
     * @return SystemGroupDataSignConfigVo
     */
    SystemGroupDataSignConfigVo sign();

    /**
     * 今日记录详情
     * @return HashMap
     */
    HashMap<String, Object> get();

    /**
     * 签到配置
     * @return List<SystemGroupDataSignConfigVo>
     */
    List<SystemGroupDataSignConfigVo> getSignConfig();

    /**
     * 积分月度列表
     * @param pageParamRequest 分页参数
     * @return List
     */
    List<UserSignMonthVo> getListGroupMonth(PageParamRequest pageParamRequest);

    /**
     * 获取用户签到信息
     * @return UserSignInfoResponse
     */
    UserSignInfoResponse getUserSignInfo();
}
