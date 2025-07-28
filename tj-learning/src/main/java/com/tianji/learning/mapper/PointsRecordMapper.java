package com.tianji.learning.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tianji.learning.domain.po.PointsRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 学习积分记录，每个月底清零 Mapper 接口
 * </p>
 *
 * @author 南哥
 * @since 2025-07-28
 */
public interface PointsRecordMapper extends BaseMapper<PointsRecord> {

    @Select("SELECT SUM(points) FROM points_record ${ew.customSqlSegment}")
    Integer queryUserPointsByTypeAndDate(QueryWrapper<PointsRecord> wrapper);


    @Select("select type,sum(points) as points from points_record ${ew.customSqlSegment} group by type")
    List<PointsRecord> queryUserPOintsByDates(QueryWrapper<PointsRecord> wrapper);
}
