package com.heima.article.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.dtos.ArticleInfoDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.mess.ArticleVisitStreamMess;
import org.apache.commons.net.nntp.Article;

public interface ApArticleService extends IService<ApArticle> {

    /**
     * 加载文章列表
     * @param dto
     * @param type 1 加载更多 2 加载更新
     * @return
     */
    ResponseResult load(ArticleHomeDto dto, Short type);

    /**
     * 保存app端相关文章
     * @param dto
     * @return
     */
    ResponseResult saveArticle(ArticleDto dto);

    /**
     * 文章行为 数据回显
     * @param dto
     * @return
     */
    ResponseResult loadArticleInfo(ArticleInfoDto dto);


    /**
     * 更新文章分值, 替换缓存中热点数据
     * @param msg
     */
    void updateScore(ArticleVisitStreamMess msg);
}
