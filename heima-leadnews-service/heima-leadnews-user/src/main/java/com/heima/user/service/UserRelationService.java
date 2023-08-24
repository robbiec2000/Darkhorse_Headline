package com.heima.user.service;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.user.dtos.UserRelationDto;

public interface UserRelationService {

    /**
     * 用户关注行为
     * @param dto
     * @return
     */
    ResponseResult follow(UserRelationDto dto);
}
