package com.shf.front.controller;

import com.shf.common.request.OrderPayRequest;
import com.shf.common.response.CommonResult;
import com.shf.common.response.OrderPayResultResponse;
import com.shf.common.utils.CrmebUtil;
import com.shf.service.service.AliPayService;
import com.shf.service.service.OrderPayService;
import com.shf.service.service.WeChatPayService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 微信缓存表 前端控制器
 */

@RestController
@RequestMapping("api/front/pay")
@Api(tags = "支付管理")
public class PayController {

    @Autowired
    private WeChatPayService weChatPayService;

    @Autowired
    private OrderPayService orderPayService;

    @Autowired
    private AliPayService aliPayService;

    /**
     * 订单支付
     */
    @ApiOperation(value = "订单支付")
    @RequestMapping(value = "/payment", method = RequestMethod.POST)
    public CommonResult<OrderPayResultResponse> payment(@RequestBody @Validated OrderPayRequest orderPayRequest, HttpServletRequest request) {
        String ip = CrmebUtil.getClientIp(request);
        return CommonResult.success(orderPayService.payment(orderPayRequest, ip));
    }

    /**
     * 查询支付结果
     *
     * @param orderNo |订单编号|String|必填
     */
    @ApiOperation(value = "查询支付结果")
    @RequestMapping(value = "/queryPayResult", method = RequestMethod.GET)
    public CommonResult<Boolean> queryPayResult(@RequestParam String orderNo) {
        return CommonResult.success(weChatPayService.queryPayResult(orderNo));
    }

    @ApiOperation("支付回调")
    @PostMapping("/trade/notify")
    public void tradeNotify(@RequestParam Map<String, String> params) {
        aliPayService.tradeNotify(params);
    }

    @ApiOperation("退款")
    @PostMapping("/trade/refund")
    public void tradeRefund(@RequestParam String orderNo) {
        aliPayService.tradeRefund(orderNo);
    }

    @ApiOperation("查询支付宝支付结果")
    public void queryAliPayResult(String orderNo) {
        aliPayService.queryAliPayResult(orderNo);
    }
}
