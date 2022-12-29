package com.shf.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shf.common.model.article.Article;
import com.shf.common.model.category.Category;
import com.shf.common.request.ArticleRequest;
import com.shf.common.request.ArticleSearchRequest;
import com.shf.common.request.PageParamRequest;
import com.shf.common.response.ArticleResponse;
import com.shf.common.vo.ArticleVo;
import com.github.pagehelper.PageInfo;

import java.util.List;

/**
*  ArticleService 接口
*/
public interface ArticleService extends IService<Article> {

    /**
     * 文章列表
     * @param cid 文章分类id
     * @param pageParamRequest 分页类参数
     * @return PageInfo<Article>
     */
    PageInfo<ArticleResponse> getList(String cid, PageParamRequest pageParamRequest);

    /**
     * 获取文章列表
     * @param request 请求参数
     * @param pageParamRequest 分页参数
     * @return PageInfo
     */
    PageInfo<ArticleVo> getAdminList(ArticleSearchRequest request, PageParamRequest pageParamRequest);

    /**
     * 文章详情
     * @param id 文章id
     * @return ArticleVo
     */
    ArticleResponse getVoByFront(Integer id);

    /**
     * 获取移动端banner列表
     * @return List<Article>
     */
    List<Article> getBannerList();

    /**
     * 获取移动端热门列表
     * @return List<ArticleResponse>
     */
    List<ArticleResponse> getHotList();

    /**
     * 获取文章分类列表
     * @return List<Category>
     */
    List<Category> getCategoryList();

    /**
     * 文章新增
     * @param articleRequest 文章新增参数
     * @return Boolean
     */
    Boolean create(ArticleRequest articleRequest);

    /**
     * 文章删除
     * @param id 文章id
     * @return Boolean
     */
    Boolean deleteById(Integer id);

    /**
     * 文章修改
     * @param id 文章id
     * @param articleRequest 文章修改参数
     */
    Boolean updateArticle(Integer id, ArticleRequest articleRequest);

    /**
     * 获取文章详情
     * @param id 文章id
     * @return Article
     */
    Article getDetail(Integer id);
}
