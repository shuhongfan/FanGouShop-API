package com.shf.service.dao;

import com.shf.common.model.express.ShippingTemplatesFree;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shf.common.request.ShippingTemplatesFreeRequest;

import java.util.List;

/**
 *  Mapper 接口
 */
public interface ShippingTemplatesFreeDao extends BaseMapper<ShippingTemplatesFree> {

    List<ShippingTemplatesFreeRequest> getListGroup(Integer tempId);
}
