package com.heima.wemedia.controller.v1;

import com.heima.model.admin.pojos.AdChannel;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.ChannelDto;
import com.heima.wemedia.service.WmChannelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/channel")
public class WmChannelController {

    @Autowired
    WmChannelService wmChannelService;

    @GetMapping("/channels")
    public ResponseResult findAll(){
        return wmChannelService.findAll();
    }

    @PostMapping("/list")
    public ResponseResult list(@RequestBody ChannelDto dto){
        return wmChannelService.list(dto);
    }

    @PostMapping("/save")
    public ResponseResult add(@RequestBody AdChannel adChannel){
        return wmChannelService.add(adChannel);
    }

    @GetMapping("/del/{id}")
    public ResponseResult del(@PathVariable Integer id){return  wmChannelService.del(id);}

    @PostMapping("/update")
    public ResponseResult modify(@RequestBody AdChannel adChannel){
        return wmChannelService.modify(adChannel);
    }


}
