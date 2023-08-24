package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.NewsAuthDto;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmNews;

public interface WmNewsService extends IService<WmNews> {

    /**
     * 条件查询文章列表
     * @param dto
     * @return
     */
    ResponseResult findList(WmNewsPageReqDto dto);

    /**
     * 平台管理查询文章
     * @param dto
     * @return
     */
    ResponseResult list(NewsAuthDto dto);


    /**
     * 根据id查询文章
     * @param id
     * @return
     */
    ResponseResult findOne(Integer id);

    /**
     * 发布修改文章或保存为草稿
     * @param dto
     * @return
     */
    ResponseResult submitNews(WmNewsDto dto);

    /**
     * 文章上下架
     * @param dto
     * @return
     */
    ResponseResult downOrUp(WmNewsDto dto);


    /**
     * 人工审核修改文章状态
     * @return
     */
    ResponseResult updateStatus(NewsAuthDto dto, Short status);

}
