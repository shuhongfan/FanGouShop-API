package com.shf.admin.controller;

import cn.hutool.core.collection.CollUtil;
import com.shf.common.request.StoreBargainSearchRequest;
import com.shf.common.request.StoreCombinationSearchRequest;
import com.shf.common.request.StoreOrderSearchRequest;
import com.shf.common.request.StoreProductSearchRequest;
import com.shf.common.response.CommonResult;
import com.shf.service.service.ExcelService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;


/**
 * Excel导出 前端控制器
 */
@Slf4j
@RestController
@RequestMapping("api/admin/export/excel")
@Api(tags = "导出 -- Excel")
public class ExcelController {

    @Autowired
    private ExcelService excelService;

    /**
     * 商品导出
     * @param request 搜索条件
     */
    @PreAuthorize("hasAuthority('admin:export:excel:product')")
    @ApiOperation(value = "产品")
    @RequestMapping(value = "/product", method = RequestMethod.GET)
    public CommonResult<HashMap<String, String>> export(@Validated StoreProductSearchRequest request) {
        String fileName = excelService.exportProduct(request);
        HashMap<String, String> map = CollUtil.newHashMap();
        map.put("fileName", fileName);
        return CommonResult.success(map);
    }

    /**
     * 砍价商品导出
     * @param request 搜索条件
     */
    @PreAuthorize("hasAuthority('admin:export:excel:bargain')")
    @ApiOperation(value = "砍价商品导出")
    @RequestMapping(value = "/bargain/product", method = RequestMethod.GET)
    public CommonResult<HashMap<String, String>> exportBargainProduct(@Validated StoreBargainSearchRequest request) {
        String fileName = excelService.exportBargainProduct(request);
        HashMap<String, String> map = CollUtil.newHashMap();
        map.put("fileName", fileName);
        return CommonResult.success(map);
    }

    /**
     * 拼团商品导出
     * @param request 搜索条件
     */
    @PreAuthorize("hasAuthority('admin:export:excel:combiantion')")
    @ApiOperation(value = "拼团商品导出")
    @RequestMapping(value = "/combiantion/product", method = RequestMethod.GET)
    public CommonResult<HashMap<String, String>> exportCombinationProduct(@Validated StoreCombinationSearchRequest request) {
        String fileName = excelService.exportCombinationProduct(request);
        HashMap<String, String> map = CollUtil.newHashMap();
        map.put("fileName", fileName);
        return CommonResult.success(map);
    }

    /**
     * 订单导出
     * @param request 搜索条件
     */
    @PreAuthorize("hasAuthority('admin:export:excel:order')")
    @ApiOperation(value = "订单导出")
    @RequestMapping(value = "/order", method = RequestMethod.GET)
    public CommonResult<HashMap<String, String>> exportOrder(@Validated StoreOrderSearchRequest request){
        String fileName = excelService.exportOrder(request);
        HashMap<String, String> map = CollUtil.newHashMap();
        map.put("fileName", fileName);
        return CommonResult.success(map);
    }

}



