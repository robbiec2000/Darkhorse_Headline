package com.heima.article.service.Impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.article.mapper.ApArticleConfigMapper;
import com.heima.article.mapper.ApArticleContentMapper;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.ApArticleService;
import com.heima.article.service.ArticleFreemarkerService;
import com.heima.common.constants.ArticleConstants;
import com.heima.common.constants.BehaviorConstants;
import com.heima.common.constants.HotArticleConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.dtos.ArticleInfoDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleConfig;
import com.heima.model.article.pojos.ApArticleContent;
import com.heima.model.article.vos.HotArticleVo;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.mess.ArticleVisitStreamMess;
import com.heima.model.user.pojos.ApUser;
import com.heima.utils.thread.AppThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class ApArticleServiceImpl extends ServiceImpl<ApArticleMapper, ApArticle> implements ApArticleService {

    @Autowired
    private ApArticleMapper apArticleMapper;

    private final static short MAX_PAGE_SIZE = 50;
    /**
     * 加载文章列表实现
     * @param dto
     * @param type 1 加载更多 2 加载更新
     * @return
     */
    @Override
    public ResponseResult load(ArticleHomeDto dto, Short type) {
        //校验参数
        Integer size = dto.getSize();
        if(size == null || size == 0){
            size = 10;
        }
        size = Math.min(size, MAX_PAGE_SIZE);

        if(!type.equals(ArticleConstants.LOADTYPE_LOAD_MORE) && !type.equals(ArticleConstants.LOADTYPE_LOAD_NEW)){
            type = ArticleConstants.LOADTYPE_LOAD_NEW;
        }

        if(StringUtils.isBlank(dto.getTag())){
            dto.setTag(ArticleConstants.DEFAULT_TAG);
        }

        if(dto.getMaxBehotTime() == null){
            dto.setMaxBehotTime(new Date());
        }

        if(dto.getMinBehotTime() == null){
            dto.setMinBehotTime(new Date());
        }

        List<ApArticle> articleList = apArticleMapper.loadArticleList(dto, type);
        return ResponseResult.okResult(articleList);
    }

    @Autowired
    private ApArticleConfigMapper apArticleConfigMapper;
    @Autowired
    private ApArticleContentMapper apArticleContentMapper;

    @Autowired
    private ArticleFreemarkerService articleFreemarkerService;

    @Override
    public ResponseResult saveArticle(ArticleDto dto) {

        /*try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }*/
        if(dto == null){
            ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        ApArticle article = new ApArticle();
        BeanUtils.copyProperties(dto, article);
        //判断是否存在id
        if(dto.getId() == null){
            //不存在id, 保存文章 文章配置 文章内容
            save(article);

            ApArticleConfig config = new ApArticleConfig(article.getId());
            apArticleConfigMapper.insert(config);

            ApArticleContent content = new ApArticleContent();
            content.setArticleId(article.getId());
            content.setContent(dto.getContent());
            apArticleContentMapper.insert(content);
        }else{
            //存在id 修改 文章 文章内容
            updateById(article);
            ApArticleContent content = apArticleContentMapper.selectOne(Wrappers.<ApArticleContent>lambdaQuery()
                    .eq(ApArticleContent::getArticleId, dto.getId()));

            content.setContent(dto.getContent());
            apArticleContentMapper.updateById(content);
        }

        //异步调用 生成静态文件到minio
        articleFreemarkerService.buildArticleToMinIO(article, dto.getContent());

        return ResponseResult.okResult(article.getId());
    }

    @Autowired
    private CacheService cacheService;
    /**
     * 文章行为 数据回显
     * @param dto
     * @return
     */
    @Override
    public ResponseResult loadArticleInfo(ArticleInfoDto dto) {
        if(dto == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        ApUser user = AppThreadLocalUtil.getUser();
        if(user == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }
        Integer userId = user.getId();

        Boolean isLike = false;
        Boolean isUnlike = false;
        Boolean isFollow = false;
        Boolean isCollection = false;

        if(dto.getArticleId() != null){
            if(cacheService.hExists(BehaviorConstants.LIKE_BEHAVIOR + dto.getArticleId(), userId.toString())){
                isLike = true;
            }
            if(cacheService.hExists(BehaviorConstants.UN_LIKE_BEHAVIOR + dto.getArticleId(), userId.toString())){
                isUnlike = true;
            }
            if(cacheService.hExists(BehaviorConstants.COLLECTION_BEHAVIOR + dto.getArticleId(), userId.toString())){
                isCollection = true;
            }
        }

        if(dto.getAuthorId() != null){
            Double score = cacheService.zScore(BehaviorConstants.APUSER_FOLLOW_RELATION + userId, dto.getAuthorId().toString());
            if(score != null){
                isFollow = true;
            }
        }

        Map<String, Boolean> map = new HashMap<>();
        map.put("islike", isLike);
        map.put("isunlike", isUnlike);
        map.put("isfollow", isFollow);
        map.put("iscollection", isCollection);

        return ResponseResult.okResult(map);
    }

    @Override
    public void updateScore(ArticleVisitStreamMess msg) {
        //更新文章点赞, 收藏, 评论的数量
        ApArticle apArticle = updateArticle(msg);
        //计算文章分值
        Integer score = computeScore(apArticle) * 3;
        //替换当前文章对应热点数据
        replaceDataToRedis(apArticle, score, ArticleConstants.HOT_ARTICLE_FIRST_PAGE+apArticle.getChannelId());
        //替换推荐对应热点数据
        replaceDataToRedis(apArticle, score, ArticleConstants.HOT_ARTICLE_FIRST_PAGE+ArticleConstants.DEFAULT_TAG);

    }

    /**
     * 替换redis数据缓存
     * @param apArticle
     * @param score
     */
    private void replaceDataToRedis(ApArticle apArticle, Integer score, String key){
        String articleList = cacheService.get(key);
        if(StringUtils.isNotBlank(articleList)){
            List<HotArticleVo> hotArticleVoList = JSON.parseArray(articleList, HotArticleVo.class);
            boolean exist = false;

            for (HotArticleVo hotArticleVo : hotArticleVoList) {
                //如果缓存中存在该文章, 更新分值
                if(hotArticleVo.getId().equals(apArticle.getId())){
                    hotArticleVo.setScore(score);
                    exist = true;
                    break;
                }
            }
            //如果不存在, 查询分值最小的一条数据
            if(!exist){
                if(hotArticleVoList.size() >= 30){
                    hotArticleVoList = hotArticleVoList.stream().sorted(Comparator.comparing(HotArticleVo::getScore).reversed()).collect(Collectors.toList());
                    HotArticleVo last = hotArticleVoList.get(hotArticleVoList.size() - 1);
                    if(last.getScore() < score){
                        hotArticleVoList.remove(last);
                        HotArticleVo hotArticleVo = new HotArticleVo();
                        BeanUtils.copyProperties(apArticle, hotArticleVo);
                        hotArticleVo.setScore(score);
                        hotArticleVoList.add(hotArticleVo);
                    }


                }else{
                    HotArticleVo hotArticleVo = new HotArticleVo();
                    BeanUtils.copyProperties(apArticle, hotArticleVo);
                    hotArticleVo.setScore(score);
                    hotArticleVoList.add(hotArticleVo);
                }
            }

            hotArticleVoList = hotArticleVoList.stream().sorted(Comparator.comparing(HotArticleVo::getScore).reversed()).collect(Collectors.toList());
            cacheService.set(key, JSON.toJSONString(hotArticleVoList));
        }
    }

    /**
     * 更新文章行为数量
     * @param msg
     */
    private ApArticle updateArticle(ArticleVisitStreamMess msg) {
        ApArticle apArticle = getById(msg.getArticleId());

        apArticle.setCollection(apArticle.getCollection()==null?0:apArticle.getCollection() + msg.getCollect());
        apArticle.setComment(apArticle.getComment()==null?0:apArticle.getComment() + msg.getComment());
        apArticle.setLikes(apArticle.getLikes()==null?0:apArticle.getLikes() + msg.getLike());
        apArticle.setViews(apArticle.getViews()==null?0:apArticle.getViews() + msg.getView());

        updateById(apArticle);

        return apArticle;
    }

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
