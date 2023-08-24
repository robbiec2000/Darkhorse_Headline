package com.heima.behavior.controller.v1;

import com.heima.behavior.service.ApBehaviorService;
import com.heima.model.behavior.dtos.CollectionBehaviorDto;
import com.heima.model.behavior.dtos.LikesBehaviorDto;
import com.heima.model.behavior.dtos.ReadBehaviorDto;
import com.heima.model.behavior.dtos.UnLikesBehaviorDto;
import com.heima.model.common.dtos.ResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ApBehaviorController {

    @Autowired
    private ApBehaviorService apBehaviorService;


    @PostMapping("/likes_behavior")
    public ResponseResult like(@RequestBody LikesBehaviorDto dto){return apBehaviorService.like(dto);}

    @PostMapping("/un_likes_behavior")
    public ResponseResult dislike(@RequestBody UnLikesBehaviorDto dto){return apBehaviorService.dislike(dto);}

    @PostMapping("/read_behavior")
    public ResponseResult read(@RequestBody ReadBehaviorDto dto){return apBehaviorService.read(dto);}

}
