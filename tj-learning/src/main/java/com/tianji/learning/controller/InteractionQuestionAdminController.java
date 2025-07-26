package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.service.IInteractionQuestionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * <p>
 * 互动提问的问题表 前端控制器
 * </p>
 *
 * @author 南哥
 * @since 2025-07-25
 */
@RestController
@RequestMapping("/admin/questions")
@RequiredArgsConstructor
@Api(tags = "互动问答管理端相关接口")
public class InteractionQuestionAdminController {
    private final IInteractionQuestionService iInteractionQuestionService;


    @ApiOperation("管理端分页查询互动问题")
    @GetMapping("/page")
    public PageDTO<QuestionAdminVO> queryQuestionPageAdmin(QuestionAdminPageQuery query){
        return iInteractionQuestionService.queryQuestionPageAdmin(query);
    }

}
