package com.shf.service.dao;

import com.shf.common.request.StoreNearRequest;
import com.shf.common.vo.SystemStoreNearVo;
import com.shf.common.model.system.SystemStore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * 门店自提 Mapper 接口
 */
public interface SystemStoreDao extends BaseMapper<SystemStore> {

    List<SystemStoreNearVo> getNearList(StoreNearRequest request);
}

