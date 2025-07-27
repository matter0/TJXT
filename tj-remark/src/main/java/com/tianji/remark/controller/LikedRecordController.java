package com.tianji.remark.controller;


import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.service.ILikedRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 点赞记录表 前端控制器
 * </p>
 *
 * @author 南哥
 * @since 2025-07-27
 */
@RestController
@RequestMapping("/likes")
@RequiredArgsConstructor
@Api(tags = "点赞业务相关接口")
public class LikedRecordController {
    private final ILikedRecordService iLikedRecordService;

    @ApiOperation("点赞或者取消点赞")
    @PostMapping
    public void addLilkeRecord(@Valid@RequestBody LikeRecordFormDTO recordFormDTO){
        iLikedRecordService.addLikeRecord(recordFormDTO);
    }

    @GetMapping("/list")
    @ApiOperation("查询指定业务id的点赞状态")
    public Set<Long> isBizLiked(@RequestParam("bizIds") List<Long> bizIds){
        return iLikedRecordService.isBizLiked(bizIds);
    }
}
