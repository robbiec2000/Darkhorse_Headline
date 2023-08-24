package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.model.admin.pojos.AdChannel;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.ChannelDto;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.utils.thread.WmThreadLocalUtil;
import com.heima.wemedia.mapper.WmChannelMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.service.WmChannelService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@Transactional
@Slf4j
public class WmChannelServiceImpl extends ServiceImpl<WmChannelMapper, WmChannel> implements WmChannelService {

    /**
     * 查询所有频道
     * @return
     */
    @Override
    public ResponseResult findAll() {
        return ResponseResult.okResult(list());
    }

    /**
     * 分页条件查询频道
     * @param dto
     * @return
     */
    @Override
    public ResponseResult list(ChannelDto dto) {
        dto.checkParam();

        IPage page = new Page(dto.getPage(), dto.getSize());
        LambdaQueryWrapper<WmChannel> lambdaQueryWrapper = new LambdaQueryWrapper<>();

        //关键字模糊查询
        if(StringUtils.isNotBlank(dto.getName())){
            lambdaQueryWrapper.like(WmChannel::getName, dto.getName());
        }

        lambdaQueryWrapper.orderByAsc(WmChannel::getOrd);

        page = page(page, lambdaQueryWrapper);

        ResponseResult res = new PageResponseResult(dto.getPage(), dto.getSize(), (int) page.getTotal());

        res.setData(page.getRecords());

        return res;

    }

    /**
     * 添加频道
     * @param adChannel
     * @return
     */
    @Override
    public ResponseResult add(AdChannel adChannel) {
        if(adChannel.getName() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        WmChannel channel = getOne(Wrappers.<WmChannel>lambdaQuery().eq(WmChannel::getName, adChannel.getName()));
        if(channel != null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_EXIST, "频道名称已存在");
        }
        WmChannel wmChannel = new WmChannel();
        if(adChannel.getOrd() == null){
            adChannel.setOrd(0);
        }
        if(adChannel.getStatus() == null){
            adChannel.setStatus(true);
        }
        BeanUtils.copyProperties(adChannel, wmChannel);
        wmChannel.setCreatedTime(new Date());
        wmChannel.setIsDefault(true);
        save(wmChannel);

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 删除频道
     * @param id
     * @return
     */
    @Override
    public ResponseResult del(Integer id) {
        if(id == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        WmChannel wmChannel = getById(id);
        //如果频道未禁用 不能删除
        if(wmChannel.getStatus().equals(true)){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "删除失败 频道未禁用");
        }
        removeById(id);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }


    @Autowired
    private WmNewsMapper wmNewsMapper;
    /**
     * 修改频道
     * @param adChannel
     * @return
     */
    @Override
    public ResponseResult modify(AdChannel adChannel) {
        if(StringUtils.isBlank(adChannel.getName()) || adChannel.getId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        WmChannel channel = getOne(Wrappers.<WmChannel>lambdaQuery().eq(WmChannel::getId, adChannel.getId()));
        if(channel == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST, "修改失败, 数据不存在");
        }
        if(!channel.getName().equals(adChannel.getName())){
            WmChannel repeat = getOne(Wrappers.<WmChannel>lambdaQuery().eq(WmChannel::getName, adChannel.getName()));
            if(repeat != null){
                return ResponseResult.errorResult(AppHttpCodeEnum.DATA_EXIST, "修改失败, 频道名已占用");
            }
        }

        if(adChannel.getStatus().equals(false)){
            //查询是否被引用
            Integer count = wmNewsMapper.selectCount(Wrappers.<WmNews>lambdaQuery().eq(WmNews::getChannelId, adChannel.getId()));
            if(count > 0){
                return ResponseResult.errorResult(AppHttpCodeEnum.DATA_EXIST, "禁用失败, 频道已被引用");
            }
        }
        WmChannel wmChannel = new WmChannel();
        BeanUtils.copyProperties(adChannel, wmChannel);
        updateById(wmChannel);

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}