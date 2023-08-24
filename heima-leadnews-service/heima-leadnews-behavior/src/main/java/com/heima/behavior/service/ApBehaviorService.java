package com.heima.behavior.service;

import com.heima.model.behavior.dtos.CollectionBehaviorDto;
import com.heima.model.behavior.dtos.LikesBehaviorDto;
import com.heima.model.behavior.dtos.ReadBehaviorDto;
import com.heima.model.behavior.dtos.UnLikesBehaviorDto;
import com.heima.model.common.dtos.ResponseResult;

public interface ApBehaviorService {
    /**
     * 用户阅读行为
     * @param dto
     * @return
     */
    ResponseResult read(ReadBehaviorDto dto);

    /**
     * 用户不喜欢行为
     * @param dto
     * @return
     */
    ResponseResult dislike(UnLikesBehaviorDto dto);

    /**
     * 用户点赞行为
     * @param dto
     * @return
     */
    ResponseResult like(LikesBehaviorDto dto);


}
