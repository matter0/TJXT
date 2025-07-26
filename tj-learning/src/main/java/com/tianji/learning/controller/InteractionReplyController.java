package com.tianji.learning.controller;


import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.service.IInteractionReplyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 互动问题的回答或评论 前端控制器
 * </p>
 *
 * @author 南哥
 * @since 2025-07-25
 */
@RestController
@RequestMapping("/replys")
@Api(value = "评论相关接口")
@RequiredArgsConstructor
public class InteractionReplyController {
    private final IInteractionReplyService iInteractionReplyService;


    @PostMapping("/replies")
    @ApiOperation("新增评论")
    public void addReplay(@RequestBody ReplyDTO replyDTO){
        iInteractionReplyService.addReplay(replyDTO);
    }

}
