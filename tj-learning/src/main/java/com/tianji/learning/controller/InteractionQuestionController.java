package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.enums.QuestionStatus;
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
@RequestMapping("/questions")
@RequiredArgsConstructor
@Api(tags = "互动问答相关接口")
public class InteractionQuestionController {
    private final IInteractionQuestionService iInteractionQuestionService;


    @ApiOperation("新增问题")
    @PostMapping
    public void saveQuestion(@Valid @RequestBody QuestionFormDTO formDTO){
        iInteractionQuestionService.saveQuestion(formDTO);
    }

    @ApiOperation("修改问题")
    @PutMapping("/{id}")
    public void updateQuestion(@PathVariable("id")Long questionId, @RequestBody QuestionFormDTO formDTO){
        iInteractionQuestionService.updateQuestion(questionId,formDTO);
    }


    @ApiOperation("分页查询互动问题")
    @GetMapping("page")
    public PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery pageQuery){
        return iInteractionQuestionService.queryQuestionPage(pageQuery);
    }


    @ApiOperation("根据id查询问题详情")
    @GetMapping("/{id}")
    public  QuestionVO queryQuestionById(
            @ApiParam(value = "问题id",example = "1")
            @PathVariable("id")
            Long id
    ){
       return iInteractionQuestionService.queryQuestionById(id);
    }



    @ApiOperation("根据id删除问题")
    @DeleteMapping("/{id}")
    public void deleteQuestionById(@PathVariable("id")Long id){
        iInteractionQuestionService.deleteQuestionById(id);
    }
}
