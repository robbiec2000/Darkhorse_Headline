package com.heima.article.service.Impl;

import com.alibaba.fastjson.JSON;
import com.heima.apis.wemedia.IWemediaClient;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.HotArticleService;
import com.heima.common.constants.ArticleConstants;
import com.heima.common.constants.BehaviorConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.vos.HotArticleVo;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.utils.thread.AppThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.DateTimeLiteralExpression;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class HotArticleServiceImpl implements HotArticleService {

    @Autowired
    private ApArticleMapper apArticleMapper;
    /**
     * 热点文章计算
     */
    @Override
    public void computeHotArticle() {
        //查询5天文章数据
        Date date = DateTime.now().minusDays(5).toDate();
        List<ApArticle> articleList = apArticleMapper.findArticleListByLast5Days(date);
        //计算文章分值
        List<HotArticleVo> hotArticleVoList = computeHotArticleScore(articleList);
        //为每个频道缓存30条分值较高的文章
        cacheTagToRedis(hotArticleVoList);
    }

    @Autowired
    private IWemediaClient wemediaClient;
    /**
     * 为每个频道缓存30条分值较高的文章
     * @param hotArticleVoList
     */
    private void cacheTagToRedis(List<HotArticleVo> hotArticleVoList) {
        ResponseResult res = wemediaClient.getChannels();
        if(res.getCode().equals(200)){
            String json = JSON.toJSONString(res.getData());
            List<WmChannel> wmChannels = JSON.parseArray(json, WmChannel.class);
            if(wmChannels != null && wmChannels.size() > 0){
                for (WmChannel wmChannel : wmChannels) {
                    List<HotArticleVo> hotArticleVos = hotArticleVoList.stream().filter(x -> x.getChannelId().equals(wmChannel.getId())).collect(Collectors.toList());
                    //给文章进行排序, 存入redis
                    sortAndCache(hotArticleVos, ArticleConstants.HOT_ARTICLE_FIRST_PAGE + wmChannel.getId());
                }
            }
        }
        //设置推荐数据
        sortAndCache(hotArticleVoList, ArticleConstants.HOT_ARTICLE_FIRST_PAGE + ArticleConstants.DEFAULT_TAG);

    }

    /**
     * 排序并且缓存数据
     * @param hotArticleVoList
     */
    private void sortAndCache(List<HotArticleVo> hotArticleVoList, String key){
        hotArticleVoList = hotArticleVoList.stream()
                .sorted(Comparator.comparing(HotArticleVo::getScore).reversed())
                .collect(Collectors.toList());
        if(hotArticleVoList.size() > 30){
            hotArticleVoList = hotArticleVoList.subList(0, 30);
        }
        cacheService.set(key, JSON.toJSONString(hotArticleVoList));
    }

    /**
     * 计算文章分值
     * @param articleList
     * @return
     */
    private List<HotArticleVo> computeHotArticleScore(List<ApArticle> articleList) {

        List<HotArticleVo> hotArticleVoList = new ArrayList<>();

        if(articleList != null || articleList.size() > 0){
            for (ApArticle article : articleList) {
                HotArticleVo hot = new HotArticleVo();
                BeanUtils.copyProperties(article, hot);
                Integer score = computeScore(article);
                hot.setScore(score);
                hotArticleVoList.add(hot);
            }
        }

        return hotArticleVoList;
    }

    @Autowired
    private CacheService cacheService;

    private Integer computeScore(ApArticle apArticle) {
        Integer score = 0;
        if(apArticle.getLikes() != null){
            score += apArticle.getLikes() * ArticleConstants.HOT_ARTICLE_LIKE_WEIGHT;
        }
        if(apArticle.getViews() != null){
            score += apArticle.getViews();
        }
        if(apArticle.getComment() != null){
            score += apArticle.getComment() * ArticleConstants.HOT_ARTICLE_COMMENT_WEIGHT;
        }
        if(apArticle.getCollection() != null){
            score += apArticle.getCollection() * ArticleConstants.HOT_ARTICLE_COLLECTION_WEIGHT;
        }

        return score;
    }


}
