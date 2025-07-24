package com.tianji.learning.mapper;

import com.tianji.learning.domain.po.LearningRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import feign.Param;

import java.time.LocalDateTime;

/**
 * <p>
 * 学习记录表 Mapper 接口
 * </p>
 *
 * @author 南哥
 * @since 2025-07-22
 */
public interface LearningRecordMapper extends BaseMapper<LearningRecord> {
    /**
     * 统计用户指定时间段内完成的小节数
     * @param userId 用户ID
     * @param begin 开始时间
     * @param end 结束时间
     */
    Integer countLearnedSections(
            @Param("userId") Long userId,
            @Param("begin") LocalDateTime begin,
            @Param("end") LocalDateTime end
    );

    /**
     * 统计用户指定课程在时间段内完成的小节数
     * @param lessonId 课表ID
     * @param userId 用户ID
     * @param begin 开始时间
     * @param end 结束时间
     */
    Integer countWeekLearnedSections(
            @Param("lessonId") Long lessonId,
            @Param("userId") Long userId,
            @Param("begin") LocalDateTime begin,
            @Param("end") LocalDateTime end
    );
}
