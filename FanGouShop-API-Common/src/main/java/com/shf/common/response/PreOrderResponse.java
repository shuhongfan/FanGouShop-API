package com.shf.common.response;

import com.shf.common.vo.OrderInfoVo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 预下单响应对象
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value="PreOrderResponse对象", description="预下单响应对象")
public class PreOrderResponse implements Serializable {

    private static final long serialVersionUID = 7282892323898493847L;

    @ApiModelProperty(value = "订单详情对象")
    private OrderInfoVo orderInfoVo;

    @ApiModelProperty(value = "预下单订单号")
    private String preOrderNo;

    @ApiModelProperty(value = "门店自提是否开启")
    private String storeSelfMention;

    @ApiModelProperty(value = "余额支付 1 开启 2 关闭")
    private String yuePayStatus;

    @ApiModelProperty(value = "微信支付 1 开启 0 关闭")
    private String payWeixinOpen;

    @ApiModelProperty(value = "支付宝支付 1 开启 0 关闭")
    private String aliPayStatus;

}