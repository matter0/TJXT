package com.tianji.learning.service;

import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.learning.domain.dto.LearningRecordFormDto;
import com.tianji.learning.domain.po.LearningRecord;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDateTime;

/**
 * <p>
 * 学习记录表 服务类
 * </p>
 *
 * @author 南哥
 * @since 2025-07-22
 */
public interface ILearningRecordService extends IService<LearningRecord> {

    LearningLessonDTO queryLearningRecordByCourse(Long courseId);

    void addLearningRecord(LearningRecordFormDto formDto);

    Integer countLearnedSections(Long userId, LocalDateTime begin,LocalDateTime end);

    Integer countWeekLearnedSections(Long lessonId,Long userId, LocalDateTime begin,LocalDateTime end);
}

