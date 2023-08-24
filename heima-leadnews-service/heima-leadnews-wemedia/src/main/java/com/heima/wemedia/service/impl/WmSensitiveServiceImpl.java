package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.model.admin.dtos.AdSensitive;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.SensitiveDto;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.model.wemedia.pojos.WmSensitive;
import com.heima.wemedia.mapper.WmSensitiveMapper;
import com.heima.wemedia.service.WmSensitiveService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@Slf4j
@Transactional
public class WmSensitiveServiceImpl extends ServiceImpl<WmSensitiveMapper, WmSensitive> implements WmSensitiveService{

    /**
     * 分页查询敏感词
     * @param dto
     * @return
     */
    @Override
    public ResponseResult list(SensitiveDto dto) {
        dto.checkParam();

        IPage page = new Page(dto.getPage(), dto.getSize());
        LambdaQueryWrapper<WmSensitive> lambdaQueryWrapper = new LambdaQueryWrapper<>();

        //关键字模糊查询
        if(StringUtils.isNotBlank(dto.getName())){
            lambdaQueryWrapper.like(WmSensitive::getSensitives, dto.getName());
        }

        lambdaQueryWrapper.orderByDesc(WmSensitive::getCreatedTime);

        page = page(page, lambdaQueryWrapper);

        ResponseResult res = new PageResponseResult(dto.getPage(), dto.getSize(), (int) page.getTotal());

        res.setData(page.getRecords());

        return res;
    }


    /**
     * 添加敏感词
     * @param adSensitive
     * @return
     */
    @Override
    public ResponseResult add(AdSensitive adSensitive) {
        if(StringUtils.isBlank(adSensitive.getSensitives())){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //查询敏感词是否存在
        WmSensitive sensitive = getOne(Wrappers.<WmSensitive>lambdaQuery().eq(WmSensitive::getSensitives, adSensitive.getSensitives()));
        if(sensitive != null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_EXIST, "敏感词已存在");
        }
        WmSensitive wmSensitive = new WmSensitive();
        BeanUtils.copyProperties(adSensitive, wmSensitive);
        wmSensitive.setCreatedTime(new Date());

        save(wmSensitive);

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Override
    public ResponseResult del(Integer id) {
        if(id == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        removeById(id);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Override
    public ResponseResult modify(AdSensitive adSensitive) {
        if(StringUtils.isBlank(adSensitive.getSensitives()) || adSensitive.getId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        WmSensitive sensitive = getOne(Wrappers.<WmSensitive>lambdaQuery().eq(WmSensitive::getId, adSensitive.getId()));
        if(sensitive == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST, "修改失败, 数据不存在");
        }
        if(!sensitive.getSensitives().equals(adSensitive.getSensitives())){
            WmSensitive repeat = getOne(Wrappers.<WmSensitive>lambdaQuery().eq(WmSensitive::getSensitives, adSensitive.getSensitives()));
            if(repeat != null){
                return ResponseResult.errorResult(AppHttpCodeEnum.DATA_EXIST, "修改失败, 敏感词已存在");
            }
        }
        WmSensitive wmSensitive = new WmSensitive();
        BeanUtils.copyProperties(adSensitive, wmSensitive);
        updateById(wmSensitive);

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
