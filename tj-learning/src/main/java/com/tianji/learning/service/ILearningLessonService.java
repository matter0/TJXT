package com.tianji.learning.service;

import com.tianji.api.dto.trade.OrderBasicDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.po.LearningLesson;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.vo.LearningLessonVO;

import java.util.List;

/**
 * <p>
 * 学生课程表 服务类
 * </p>
 *
 * @author 南哥
 * @since 2025-07-21
 */
public interface ILearningLessonService extends IService<LearningLesson> {


    void addUserLessons(Long userId, List<Long> courseIds);

    PageDTO<LearningLessonVO> queryMyLessons(PageQuery query);

    LearningLessonVO queryMyCurrentLesson();

    void deleteLessons(Long userId, List<Long> courseIds);

    Long isLessonValid(Long courseId);

    LearningLessonVO queryLessonSimpInfoById();

    Integer countLearningLessonByCourse(Long courseId);
}


