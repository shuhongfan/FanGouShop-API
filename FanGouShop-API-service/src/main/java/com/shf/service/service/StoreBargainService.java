package com.shf.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shf.common.request.BargainFrontRequest;
import com.shf.common.response.*;
import com.shf.common.vo.MyRecord;
import com.shf.common.request.PageParamRequest;
import com.github.pagehelper.PageInfo;
import com.shf.common.model.bargain.StoreBargain;
import com.shf.common.request.StoreBargainRequest;
import com.shf.common.request.StoreBargainSearchRequest;

import java.util.HashMap;
import java.util.List;

/**
 * 砍价 Service
 */
public interface StoreBargainService extends IService<StoreBargain> {

    /**
     * 分页显示砍价商品列表
     */
    PageInfo<StoreBargainResponse> getList(StoreBargainSearchRequest request, PageParamRequest pageParamRequest);

    /**
     * 新增砍价商品
     */
    boolean saveBargain(StoreBargainRequest storeBargainRequest);

    /**
     * 删除砍价商品
     */
    boolean deleteById(Integer id);

    /**
     * 修改砍价商品
     */
    boolean updateBargain(StoreBargainRequest storeBargainRequest);

    /**
     * 修改砍价商品状态
     */
    boolean updateBargainStatus(Integer id, boolean status);

    /**
     * 查询砍价商品详情
     */
    StoreProductInfoResponse getAdminDetail(Integer id);

    /**
     * H5 砍价商品列表
     * @return PageInfo<StoreBargainDetailResponse>
     */
    PageInfo<StoreBargainDetailResponse> getH5List(PageParamRequest pageParamRequest);

    /**
     * H5 获取砍价商品详情信息
     * @return BargainDetailResponse
     */
    BargainDetailH5Response getH5Detail(Integer id);

    /**
     * 获取当前时间的砍价商品
     * @return List<StoreBargain>
     */
    List<StoreBargain> getCurrentBargainByProductId(Integer productId);

    /**
     * 创建砍价活动
     * @return MyRecord
     */
    MyRecord start(BargainFrontRequest bargainFrontRequest);

    /**
     * 后台任务批量操作库存
     */
    void consumeProductStock();

    /**
     * 砍价活动结束后处理
     */
    void stopAfterChange();

    /**
     * 商品是否存在砍价活动
     * @param productId 商品编号
     */
    Boolean isExistActivity(Integer productId);

    /**
     * 查询带异常
     * @param id 砍价商品id
     * @return StoreBargain
     */
    StoreBargain getByIdException(Integer id);

    /**
     * 添加/扣减库存
     * @param id 秒杀商品id
     * @param num 数量
     * @param type 类型：add—添加，sub—扣减
     */
    Boolean operationStock(Integer id, Integer num, String type);

    /**
     * 砍价首页信息
     * @return BargainIndexResponse
     */
    BargainIndexResponse getIndexInfo();

    /**
     * 获取砍价列表header
     * @return BargainHeaderResponse
     */
    BargainHeaderResponse getHeader();

    /**
     * 根据id数组获取砍价商品map
     * @param bargainIdList 砍价商品id数组
     * @return HashMap<Integer, StoreBargain>
     */
    HashMap<Integer, StoreBargain> getMapInId(List<Integer> bargainIdList);
}
