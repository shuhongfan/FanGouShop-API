package com.shf.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shf.common.request.PageParamRequest;
import com.shf.common.request.SystemStoreStaffRequest;
import com.shf.common.response.SystemStoreStaffResponse;
import com.github.pagehelper.PageInfo;
import com.shf.common.model.system.SystemStoreStaff;

/**
 * SystemStoreStaffService 接口
 */
public interface SystemStoreStaffService extends IService<SystemStoreStaff> {

    /**
     * 分页显示门店核销员列表
     * @param storeId 门店id
     * @param pageParamRequest 分页参数
     */
    PageInfo<SystemStoreStaffResponse> getList(Integer storeId, PageParamRequest pageParamRequest);

    /**
     *      添加核销员 唯一验证
     * @param request  当前添加参数
     * @return                  添加结果
     */
    Boolean saveUnique(SystemStoreStaffRequest request);

    /**
     * 更新核销员信息
     * @param id 核销员id
     * @param systemStoreStaffRequest 更新参数
     */
    Boolean edit(Integer id, SystemStoreStaffRequest systemStoreStaffRequest);

    /**
     * 修改核销员状态
     * @param id 核销员id
     * @param status 状态
     * @return Boolean
     */
    Boolean updateStatus(Integer id, Integer status);
}
