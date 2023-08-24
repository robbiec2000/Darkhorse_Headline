package com.heima.search.service;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.search.UserSearchDto;

import java.io.IOException;

public interface ArticleSearchService {

    /**
     * es文章分页查询检索
     * @param dto
     * @return
     */
    ResponseResult search(UserSearchDto dto) throws IOException;
}
