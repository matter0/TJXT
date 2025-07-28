package com.tianji.learning.controller;


import com.tianji.learning.domain.vo.SignRecordVO;
import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.service.ISignRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 签到记录表 前端控制器
 * </p>
 *
 * @author 南哥
 * @since 2025-07-28
 */
@RestController
@Api(tags = "签到相关接口")
@RequestMapping("/signRecord")
@RequiredArgsConstructor
public class SignRecordController {
    private final ISignRecordService iSignRecordService;


    @PostMapping
    @ApiOperation("签到功能相关接口")
    public SignResultVO addSignRecords(){
        return iSignRecordService.addSignRecords();
    }
}
