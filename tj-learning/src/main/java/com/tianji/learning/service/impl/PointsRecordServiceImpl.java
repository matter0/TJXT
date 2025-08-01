package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constans.RedisConstans;
import com.tianji.learning.domain.po.PointsRecord;
import com.tianji.learning.domain.vo.PointsStatisticsVO;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.mapper.PointsRecordMapper;
import com.tianji.learning.service.IPointsRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 学习积分记录，每个月底清零 服务实现类
 * </p>
 *
 * @author 南哥
 * @since 2025-07-28
 */
@Service
@RequiredArgsConstructor
public class PointsRecordServiceImpl extends ServiceImpl<PointsRecordMapper, PointsRecord> implements IPointsRecordService {
    private final StringRedisTemplate redisTemplate;
    @Override
    public void addPointsRecord(Long userId, int points, PointsRecordType type) {
        LocalDateTime now=LocalDateTime.now();
        int maxPoints = type.getMaxPoints();
        //判断当前方式有没有积分上限
        int realPoints=points;
        if (maxPoints>0){
            //如果有，则需要判断是否超过上限
            LocalDateTime begin = DateUtils.getDayStartTime(now);
            LocalDateTime end = DateUtils.getDayEndTime(now);
            //查询今日所得积分
            int currentPoints = queryUserPointsByTypeAndDate(userId, type, begin, end);
            //判断是否超过上限
            if (currentPoints>=maxPoints){
                //如果超过，则直接结束
                return;
            }
            //没超过，这保存积分记录
            if (currentPoints+points>maxPoints){
                realPoints=maxPoints-currentPoints;
            }
        }
        //如果没有积分上限，则直接保存积分记录
        PointsRecord p=new PointsRecord();
        p.setPoints(realPoints);
        p.setUserId(userId);
        p.setType(type);
        save(p);
        //更新总积分到redis
        String key= RedisConstans.POINTS_BOARD_KEY_PREFIX+now.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        redisTemplate.opsForZSet().incrementScore(key,userId.toString(),realPoints);

    }

    @Override
    public List<PointsStatisticsVO> queryMyPointsToday() {
        Long userId = UserContext.getUser();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime begin = DateUtils.getDayStartTime(now);
        LocalDateTime end = DateUtils.getDayEndTime(now);
        QueryWrapper<PointsRecord> wrapper=new QueryWrapper<>();
        wrapper.lambda()
                .eq(PointsRecord::getUserId,userId)
                .between(PointsRecord::getCreateTime,begin,end);
        List<PointsRecord> list= getBaseMapper().queryUserPOintsByDates(wrapper);
        if (CollUtils.isEmpty(list)){
            return CollUtils.emptyList();
        }
        List<PointsStatisticsVO> vos=new ArrayList<>(list.size());
        for (PointsRecord p:list) {
            PointsStatisticsVO vo=new PointsStatisticsVO();
            vo.setType(p.getType().getDesc());
            vo.setMaxPoints(p.getType().getMaxPoints());
            vo.setPoints(p.getPoints());
            vos.add(vo);
        }
        return vos;
    }

    private int queryUserPointsByTypeAndDate(Long userId,PointsRecordType type,LocalDateTime begin,LocalDateTime end){
        // 1.查询条件
        QueryWrapper<PointsRecord> wrapper = new QueryWrapper<>();
        wrapper.lambda()
                .eq(PointsRecord::getUserId, userId)
                .eq(type != null, PointsRecord::getType, type)
                .between(begin != null && end != null, PointsRecord::getCreateTime, begin, end);
        // 2.调用mapper，查询结果
        Integer points = getBaseMapper().queryUserPointsByTypeAndDate(wrapper);
        // 3.判断并返回
        return points == null ? 0 : points;


    }
}
