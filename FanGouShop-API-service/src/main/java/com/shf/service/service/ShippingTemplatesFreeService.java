package com.shf.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shf.common.model.express.ShippingTemplatesFree;
import com.shf.common.request.ShippingTemplatesFreeRequest;

import java.util.List;

/**
* ShippingTemplatesFreeService 接口
*/
public interface ShippingTemplatesFreeService extends IService<ShippingTemplatesFree> {

    void saveAll(List<ShippingTemplatesFreeRequest> shippingTemplatesFreeRequestList, Integer type, Integer id);

    List<ShippingTemplatesFreeRequest> getListGroup(Integer tempId);

    /**
     * 删除
     * @param tempId 运费模板id
     */
    Boolean delete(Integer tempId);

    /**
     * 根据模板编号、城市ID查询
     * @param tempId 模板编号
     * @param cityId 城市ID
     * @return 运费模板
     */
    ShippingTemplatesFree getByTempIdAndCityId(Integer tempId, Integer cityId);
}
