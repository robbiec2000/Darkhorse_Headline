package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.admin.pojos.AdChannel;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.ChannelDto;
import com.heima.model.wemedia.pojos.WmChannel;

public interface WmChannelService extends IService<WmChannel> {

    ResponseResult findAll();

    ResponseResult list(ChannelDto dto);


    ResponseResult add(AdChannel adChannel);

    ResponseResult del(Integer id);

    ResponseResult modify(AdChannel adChannel);
}