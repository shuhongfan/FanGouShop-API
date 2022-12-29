package com.shf.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shf.common.request.PageParamRequest;
import com.shf.common.model.user.UserAddress;
import com.shf.common.request.UserAddressRequest;

import java.util.List;

/**
 * UserAddressService 接口实现
 */
public interface UserAddressService extends IService<UserAddress> {

    /**
     * 用户地址列表
     * @param pageParamRequest 分页参数
     * @return List<UserAddress>
     */
    List<UserAddress> getList(PageParamRequest pageParamRequest);

    /**
     * 添加用户地址
     * @param request 地址请求参数
     * @return UserAddress
     */
    UserAddress create(UserAddressRequest request);

    /**
     * 设置默认地址
     * @param id 地址id
     * @return Boolean
     */
    Boolean def(Integer id);

    /**
     * 删除用户地址
     * @param id 地址id
     * @return Boolean
     */
    Boolean delete(Integer id);

    UserAddress getDefault();

    UserAddress getById(Integer addressId);

    /**
     * 获取地址详情
     * @param id 地址id
     * @return UserAddress
     */
    UserAddress getDetail(Integer id);

    /**
     * 获取默认地址
     * @return UserAddress
     */
    UserAddress getDefaultByUid(Integer uid);
}
