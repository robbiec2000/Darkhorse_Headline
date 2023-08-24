package com.heima.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.user.dtos.AuthDto;
import com.heima.model.user.pojos.ApUserRealname;

public interface ApUserRealnameService extends IService<ApUserRealname> {

    /**
     * 分页查询用户
     * @param dto
     * @return
     */
    ResponseResult list(AuthDto dto);

    /**
     * 通过审核
     * @param dto
     * @return
     */
    ResponseResult authPass(AuthDto dto);

    /**
     * 驳回审核
     * @param dto
     * @return
     */
    ResponseResult authFail(AuthDto dto);
}
