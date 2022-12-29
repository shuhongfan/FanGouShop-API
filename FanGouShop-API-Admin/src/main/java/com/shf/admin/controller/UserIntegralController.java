package com.shf.admin.controller;

import com.shf.common.page.CommonPage;
import com.shf.common.response.CommonResult;
import com.shf.common.request.PageParamRequest;
import com.shf.common.request.AdminIntegralSearchRequest;
import com.shf.common.response.UserIntegralRecordResponse;
import com.shf.service.service.UserIntegralRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户积分管理控制器
 */
@Slf4j
@RestController
@RequestMapping("api/admin/user/integral")
@Api(tags = "用户积分管理")
public class UserIntegralController {

    @Autowired
    private UserIntegralRecordService integralRecordService;

    /**
     * 积分分页列表
     * @param request 搜索条件
     * @param pageParamRequest 分页参数
     */
    @PreAuthorize("hasAuthority('admin:user:integral:list')")
    @ApiOperation(value = "积分分页列表")
    @RequestMapping(value = "/list", method = RequestMethod.POST)
    public CommonResult<CommonPage<UserIntegralRecordResponse>> getList(@RequestBody @Validated AdminIntegralSearchRequest request, @Validated PageParamRequest pageParamRequest) {
        CommonPage<UserIntegralRecordResponse> restPage = CommonPage.restPage(integralRecordService.findAdminList(request, pageParamRequest));
        return CommonResult.success(restPage);
    }


}
