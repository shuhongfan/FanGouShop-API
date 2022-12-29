package com.shf.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shf.common.model.system.SystemRole;
import com.shf.common.request.PageParamRequest;
import com.shf.common.request.SystemRoleRequest;
import com.shf.common.request.SystemRoleSearchRequest;
import com.shf.common.response.RoleInfoResponse;
import com.shf.common.vo.CategoryTreeVo;

import java.util.List;

/**
 * SystemRoleService 接口
 */
public interface SystemRoleService extends IService<SystemRole> {

    /**
     * 获取所有角色
     * @return List
     */
    List<SystemRole> getAllList();

    /**
     * 列表
     * @param request 请求参数
     * @param pageParamRequest 分页类参数
     * @return List<SystemRole>
     */
    List<SystemRole> getList(SystemRoleSearchRequest request, PageParamRequest pageParamRequest);

    /**
     * 根据id集合获取对应权限列表
     * @param ids id集合
     * @return 对应的权限列表
     */
    List<SystemRole> getListInIds(List<Integer> ids);

    /**
     * 管理员菜单权限
     */
    List<CategoryTreeVo> menu();

    /**
     * 修改身份状态
     */
    Boolean updateStatus(Integer id, Boolean status);

    /**
     * 添加身份
     * @param systemRoleRequest 身份参数
     * @return Boolean
     */
    Boolean add(SystemRoleRequest systemRoleRequest);

    /**
     * 修改身份管理表
     * @param systemRoleRequest 修改参数
     */
    Boolean edit(SystemRoleRequest systemRoleRequest);

    /**
     * 删除角色
     * @param id 角色id
     * @return Boolean
     */
    Boolean delete(Integer id);

    /**
     * 获取角色详情
     * @param id 角色id
     * @return RoleInfoResponse
     */
    RoleInfoResponse getInfo(Integer id);
}
