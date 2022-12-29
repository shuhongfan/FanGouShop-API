package com.shf.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shf.common.model.user.User;
import com.shf.common.response.UserSpreadPeopleItemResponse;

import java.util.List;
import java.util.Map;

/**
 * 用户表 Mapper 接口
 */
public interface UserDao extends BaseMapper<User> {

    List<UserSpreadPeopleItemResponse> getSpreadPeopleList(Map<String, Object> map);

    List<User> findAdminList(Map<String, Object> map);
}
