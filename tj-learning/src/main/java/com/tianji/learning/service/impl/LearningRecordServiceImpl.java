package com.tianji.learning.service.impl;

import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.api.dto.leanring.LearningRecordDTO;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.LearningRecordFormDto;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.tianji.learning.service.ILearningRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 学习记录表 服务实现类
 * </p>
 *
 * @author 南哥
 * @since 2025-07-22
 */
@Service
@RequiredArgsConstructor
public class LearningRecordServiceImpl extends ServiceImpl<LearningRecordMapper, LearningRecord> implements ILearningRecordService {

    private final ILearningLessonService learningLessonService;
    private final ILearningRecordService learningRecordService;

    @Override
    public LearningLessonDTO queryLearningRecordByCourse(Long courseId) {
        //获取用户id
        Long userId = UserContext.getUser();
        //根据用户id和courseid从learningLesson数据库表中找到对应的lessonid
        LearningLesson lesson = learningLessonService.queryByUserAndCourseId(userId, courseId);
        //然后根据lessonid和userid找到对应的record表，并从表中获取所需的返回信息
        Long lessonId = lesson.getId();
        List<LearningRecord> records = lambdaQuery()
                .eq(LearningRecord::getLessonId, lessonId)
                .list();
        //封装结果
      LearningLessonDTO dto=new LearningLessonDTO();
      dto.setId(lessonId);
      dto.setLatestSectionId(lesson.getLatestSectionId());
      dto.setRecords(BeanUtils.copyList(records, LearningRecordDTO.class));
      return dto;
    }
}
