package com.tianji.remark.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.remark.constans.RedisConstants;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.dto.LikedTimesDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.tianji.remark.mapper.LikedRecordMapper;
import com.tianji.remark.service.ILikedRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.tianji.common.constants.MqConstants.Exchange.LIKE_RECORD_EXCHANGE;
import static com.tianji.common.constants.MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE;

/**
 * <p>
 * 点赞记录表 服务实现类
 * </p>
 *
 * @author 南哥
 * @since 2025-07-27
 */
@Service
@RequiredArgsConstructor
public class LikedRecordServiceRedisImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {
    private final RabbitMqHelper mqHelper;

    private final StringRedisTemplate redisTemplate;

    @Override
    public void addLikeRecord(LikeRecordFormDTO recordFormDTO) {
        //基于参数判断是点赞还是取消点赞
        boolean success=recordFormDTO.getLiked()?like(recordFormDTO):unlike(recordFormDTO);
        //判断是否成功
        if (!success){
            return;
        }
        //如果点赞执行成功，统计该业务的点赞总数
        Long likedTimes = redisTemplate.opsForSet()
                .size(RedisConstants.LIKE_BIZ_KEY_PREFIX + recordFormDTO.getBizId());
        if (likedTimes==null){
            return;
        }
        //缓存点赞总数到redis
        redisTemplate.opsForZSet().add(
                RedisConstants.LIKES_TIMES_KEY_PREFIX+recordFormDTO.getBizType(),
                recordFormDTO.getBizId().toString(),
                likedTimes
        );
    }

    @Override
    public Set<Long> isBizLiked(List<Long> bizIds) {
        // 1.获取登录用户id
        Long userId = UserContext.getUser();
        // 2.查询点赞状态
        List<Object> objects = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            StringRedisConnection src = (StringRedisConnection) connection;
            for (Long bizId : bizIds) {
                String key = RedisConstants.LIKE_BIZ_KEY_PREFIX + bizId;
                src.sIsMember(key, userId.toString());
            }
            return null;
        });
        // 3.返回结果
        return IntStream.range(0, objects.size()) // 创建从0到集合size的流
                .filter(i -> (boolean) objects.get(i)) // 遍历每个元素，保留结果为true的角标i
                .mapToObj(bizIds::get)// 用角标i取bizIds中的对应数据，就是点赞过的id
                .collect(Collectors.toSet());// 收集
    }

    @Override
    public void readLikedTimesAndSendMessage(String bizType, int maxBizSize) {
        //读取并移除reids中缓存的点赞总数
        String key=RedisConstants.LIKES_TIMES_KEY_PREFIX+bizType;
        Set<ZSetOperations.TypedTuple<String>> typedTuples = redisTemplate.opsForZSet().popMin(key, maxBizSize);
        if (CollUtils.isEmpty(typedTuples)){
            return;
        }
        //数据转换
        List<LikedTimesDTO> list=new ArrayList<>(typedTuples.size());
        for (ZSetOperations.TypedTuple<String> tuple : typedTuples) {
            String bizId=tuple.getValue();
            Double likedTimes = tuple.getScore();
            if (bizId==null||likedTimes==null){
                continue;
            }
            list.add(LikedTimesDTO.of(Long.valueOf(bizId),likedTimes.intValue()));
        }
        //发送mq消息
        mqHelper.send(
                LIKE_RECORD_EXCHANGE,
                StringUtils.format(LIKED_TIMES_KEY_TEMPLATE,bizType),
                list
        );


    }

    private boolean like(LikeRecordFormDTO recordFormDTO){
        //获取用户id
        Long userId = UserContext.getUser();
        //获取key
        String key= RedisConstants.LIKE_BIZ_KEY_PREFIX+recordFormDTO.getBizId();
        //执行删除操作
        Long result=redisTemplate.opsForSet().add(key,userId.toString());
        return result!=null&&result>0;
    }
    private boolean unlike(LikeRecordFormDTO recordFormDTO) {
        //获取用户id
        Long userId = UserContext.getUser();
        //获取key
        String key = RedisConstants.LIKE_BIZ_KEY_PREFIX + recordFormDTO.getBizId();
        //执行删除操作
        Long result = redisTemplate.opsForSet().remove(key, userId.toString());
        return result != null && result > 0;
    }

}
