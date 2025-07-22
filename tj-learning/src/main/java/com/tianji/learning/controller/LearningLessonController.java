package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.service.ILearningLessonService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 学生课程表 前端控制器
 * </p>
 *
 * @author 南哥
 * @since 2025-07-21
 */
@Api(tags = "我的课表相关信息")
@RestController
@RequestMapping("/lessons")
@RequiredArgsConstructor
public class LearningLessonController {
    private final ILearningLessonService learningLessonService;


    @ApiOperation("查询我的课表，排序字段，latest_learn_time 学习时间排序，create_time 购买时间排序")
    @GetMapping("/page")
    public PageDTO<LearningLessonVO> queryMyLessons(PageQuery query){
        return learningLessonService.queryMyLessons(query);
    }

    @GetMapping("/now")
    @ApiOperation("查询正在学习的课程")
    public LearningLessonVO queryMyCurrentLesson(){
        return learningLessonService.queryMyCurrentLesson();
    }

    @ApiOperation("检验当前用户是否可以学习当前课程")
    @GetMapping("/lessons/{courseId}/valid")
    public Long isLessonValid(@PathVariable("courseId") Long courseId){
       return learningLessonService.isLessonValid(courseId);
    }
    @ApiOperation("根据课程的id简易查询课程的信息")
    @GetMapping("/ls/lessons/{courseId}")
    public LearningLessonVO queryLessonSimpInfoById(@PathVariable("courseId")Long courseId) {
        return learningLessonService.queryLessonSimpInfoById();
    }

    @ApiOperation("统计课程的学习人数")
    @GetMapping("/lessons/{courseId}/count")
    public Integer countLearningLessonByCourse(@PathVariable("courseId") Long courseId){
        return learningLessonService. countLearningLessonByCourse(courseId);
    }
}
