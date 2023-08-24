package com.heima.user.service.impl;

import com.heima.common.constants.BehaviorConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.dtos.UserRelationDto;
import com.heima.model.user.pojos.ApUser;
import com.heima.user.service.UserRelationService;
import com.heima.utils.thread.AppThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserRelationServiceImpl implements UserRelationService {

    @Autowired
    private CacheService cacheService;
    /**
     * 用户关注行为
     * @param dto
     * @return
     */
    @Override
    public ResponseResult follow(UserRelationDto dto) {
        if(dto == null || dto.getAuthorId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        ApUser user = AppThreadLocalUtil.getUser();
        if(user == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }
        Integer userId = user.getId();
        //关注
        if(dto.getOperation() == 0){
            //用户关注作者
            cacheService.zAdd(BehaviorConstants.APUSER_FOLLOW_RELATION + userId,
                    dto.getAuthorId().toString(), System.currentTimeMillis());
            //作者增加粉丝
            cacheService.zAdd(BehaviorConstants.APUSER_FANS_RELATION + dto.getAuthorId(),
                    userId.toString(), System.currentTimeMillis());
        }else if(dto.getOperation() == 1){//取消关注
            //用户取消关注作者
            cacheService.zRemove(BehaviorConstants.APUSER_FOLLOW_RELATION + userId, dto.getAuthorId().toString());
            //作者减少粉丝
            cacheService.zRemove(BehaviorConstants.APUSER_FANS_RELATION + dto.getArticleId(), user.toString());
        }

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
