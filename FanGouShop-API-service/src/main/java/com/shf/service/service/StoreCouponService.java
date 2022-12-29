package com.shf.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shf.common.request.PageParamRequest;
import com.shf.common.request.SearchAndPageRequest;
import com.shf.common.model.coupon.StoreCoupon;
import com.shf.common.request.StoreCouponRequest;
import com.shf.common.request.StoreCouponSearchRequest;
import com.shf.common.response.StoreCouponFrontResponse;
import com.shf.common.response.StoreCouponInfoResponse;

import java.util.List;

/**
 * StoreCouponService 接口
 */
public interface StoreCouponService extends IService<StoreCoupon> {

    List<StoreCoupon> getList(StoreCouponSearchRequest request, PageParamRequest pageParamRequest);

    boolean create(StoreCouponRequest request);

    StoreCoupon getInfoException(Integer id);

    StoreCouponInfoResponse info(Integer id);

    /**
     * 根据优惠券id获取
     * @param ids 优惠券id集合
     * @return 优惠券列表
     */
    List<StoreCoupon> getByIds(List<Integer> ids);

    /**
     * 扣减数量
     * @param id 优惠券id
     * @param num 数量
     * @param isLimited 是否限量
     */
    Boolean deduction(Integer id, Integer num, Boolean isLimited);

    /**
     * 获取用户注册赠送新人券
     * @return 优惠券列表
     */
    List<StoreCoupon> findRegisterList();

    /**
     * 发送优惠券列表
     * @param searchAndPageRequest 搜索分页参数
     * @return 优惠券列表
     */
    List<StoreCoupon> getSendList(SearchAndPageRequest searchAndPageRequest);

    /**
     * 删除优惠券
     * @param id 优惠券id
     * @return Boolean
     */
    Boolean delete(Integer id);

    /**
     * 移动端优惠券列表
     * @param type 类型，1-通用，2-商品，3-品类
     * @param productId 产品id，搜索产品指定优惠券
     * @param pageParamRequest 分页参数
     * @return List<StoreCouponFrontResponse>
     */
    List<StoreCouponFrontResponse> getH5List(Integer type, Integer productId, PageParamRequest pageParamRequest);

    /**
     * 修改优惠券状态
     * @param id 优惠券id
     * @param status 状态
     */
    Boolean updateStatus(Integer id, Boolean status);
}
