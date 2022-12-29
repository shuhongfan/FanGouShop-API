package com.shf.common.request;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 后台管理员表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value="SystemAdminRequest对象", description="后台管理员请求对象")
public class SystemAdminRequest implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "后台管理员姓名")
    private String realName;

    @ApiModelProperty(value = "后台管理员权限(menus_id)")
    private String roles;

    @ApiModelProperty(value = "后台管理员状态 1有效0无效")
    private Boolean status;
}
