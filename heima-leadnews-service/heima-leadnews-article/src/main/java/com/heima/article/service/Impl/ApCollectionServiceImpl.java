package com.heima.article.service.Impl;

import com.heima.article.service.ApCollectionService;
import com.heima.common.constants.BehaviorConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.behavior.dtos.CollectionBehaviorDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.utils.thread.AppThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class ApCollectionServiceImpl implements ApCollectionService {

    @Autowired
    private CacheService cacheService;

    /**
     * 文章收藏
     * @param dto
     * @return
     */
    @Override
    public ResponseResult addCollection(CollectionBehaviorDto dto) {
        if(dto == null || dto.getEntryId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        if(AppThreadLocalUtil.getUser() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }
        Integer userId = AppThreadLocalUtil.getUser().getId();
        if(dto.getOperation() == 0){
            cacheService.hPut(BehaviorConstants.COLLECTION_BEHAVIOR + dto.getEntryId(),
                    userId.toString(),  dto.toString());
        }else if(dto.getOperation() == 1){
            cacheService.hDelete(BehaviorConstants.COLLECTION_BEHAVIOR + dto.getEntryId(), userId.toString());
        }

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
