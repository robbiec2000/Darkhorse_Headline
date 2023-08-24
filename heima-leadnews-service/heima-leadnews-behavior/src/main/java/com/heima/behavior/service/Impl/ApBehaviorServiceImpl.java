package com.heima.behavior.service.Impl;

import com.alibaba.fastjson.JSON;
import com.heima.behavior.service.ApBehaviorService;
import com.heima.common.constants.BehaviorConstants;
import com.heima.common.constants.HotArticleConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.behavior.dtos.CollectionBehaviorDto;
import com.heima.model.behavior.dtos.LikesBehaviorDto;
import com.heima.model.behavior.dtos.ReadBehaviorDto;
import com.heima.model.behavior.dtos.UnLikesBehaviorDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.mess.UpdateArticleMess;
import com.heima.utils.thread.AppThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class ApBehaviorServiceImpl implements ApBehaviorService {

    @Autowired
    private CacheService cacheService;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    /**
     * 用户阅读行为
     * @param dto
     * @return
     */
    @Override
    public ResponseResult read(ReadBehaviorDto dto) {
        if(AppThreadLocalUtil.getUser() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }

        if(dto.getArticleId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        String key = BehaviorConstants.READ_BEHAVIOR + dto.getArticleId();
        Integer userId = AppThreadLocalUtil.getUser().getId();
        String readBehavior = (String) cacheService.hGet(key, userId.toString());
        if(StringUtils.isNotBlank(readBehavior)){
            ReadBehaviorDto readBehaviorDto = JSON.parseObject(readBehavior, ReadBehaviorDto.class);
            dto.setCount((short) (readBehaviorDto.getCount() + dto.getCount()));
        }
        cacheService.hPut(key, userId.toString(), JSON.toJSONString(dto));
        //发送消息, 数据聚合
        UpdateArticleMess msg = new UpdateArticleMess();
        msg.setType(UpdateArticleMess.UpdateArticleType.VIEWS);
        msg.setArticleId(dto.getArticleId());
        msg.setAdd(1);
        kafkaTemplate.send(HotArticleConstants.HOT_ARTICLE_SCORE_TOPIC, JSON.toJSONString(msg));

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 用户不喜欢行为
     * @param dto
     * @return
     */
    @Override
    public ResponseResult dislike(UnLikesBehaviorDto dto) {

        if(AppThreadLocalUtil.getUser() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }

        if(dto.getArticleId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        Integer userId = AppThreadLocalUtil.getUser().getId();
        String key = BehaviorConstants.UN_LIKE_BEHAVIOR + dto.getArticleId();
        if(dto.getType() == 0){
            //不喜欢
            cacheService.hPut(key , userId.toString(), "1");
        }else if(dto.getType() == 1){
            //取消不喜欢
            cacheService.hDelete(key, userId.toString());
        }

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 用户点赞行为
     * @param dto
     * @return
     */
    @Override
    public ResponseResult like(LikesBehaviorDto dto) {
        if(dto.getArticleId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        if(AppThreadLocalUtil.getUser() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }

        UpdateArticleMess msg = new UpdateArticleMess();
        msg.setType(UpdateArticleMess.UpdateArticleType.LIKES);
        msg.setArticleId(dto.getArticleId());


        Integer userId = AppThreadLocalUtil.getUser().getId();
        String key = BehaviorConstants.LIKE_BEHAVIOR + dto.getArticleId();
        if(dto.getOperation() == 0){
            //点赞
            cacheService.hPut(key, userId.toString(), "1");
            msg.setAdd(1);
        }else if(dto.getOperation() == 1){
            //取消点赞
            cacheService.hDelete(key, userId.toString());
            msg.setAdd(-1);
        }

        //发送消息, 数据聚合
        kafkaTemplate.send(HotArticleConstants.HOT_ARTICLE_SCORE_TOPIC, JSON.toJSONString(msg));


        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }


}
