package com.tianji.learning.service.impl;

import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BooleanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constans.RedisConstans;
import com.tianji.learning.domain.po.SignRecord;
import com.tianji.learning.domain.vo.SignRecordVO;
import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.mapper.SignRecordMapper;
import com.tianji.learning.mq.message.SignInMessage;
import com.tianji.learning.service.ISignRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * <p>
 * 签到记录表 服务实现类
 * </p>
 *
 * @author 南哥
 * @since 2025-07-28
 */
@Service
@RequiredArgsConstructor
public class SignRecordServiceImpl extends ServiceImpl<SignRecordMapper, SignRecord> implements ISignRecordService {
    private final StringRedisTemplate redisTemplate;
    private final RabbitMqHelper mqHelper;


    @Override
    public SignResultVO addSignRecords() {
        //获取登录用户
        Long userId = UserContext.getUser();
        //获取签到日期
        LocalDate now=LocalDate.now();
        //拼接key
        String key= RedisConstans.SIGN_RECORD_KEY_PREFIX+userId+":"+now.format(DateUtils.SIGN_DATE_SUFFIX_FORMATTER);
        //计算offset偏移量
        int offset=now.getDayOfMonth()-1;
        //保存签到信息到redis
        Boolean exist = redisTemplate.opsForValue().setBit(key, offset, true);
        if (BooleanUtils.isTrue(exist)){
            throw new BizIllegalException("不允许重复签到");
        }
        //计算连续签到的天数
        int signDays=countSignDays(key,now.getDayOfMonth());
        //计算签到得分
        int rewardPoints=0;
        switch (signDays){
            case 7:
                rewardPoints=10;
                break;
            case 14:
                rewardPoints=20;
                break;
            case 28:
                rewardPoints=40;
                break;
        }
        // 保存积分明细
        mqHelper.send(
                MqConstants.Exchange.LEARNING_EXCHANGE,
                MqConstants.Key.SIGN_IN,
                SignInMessage.of(userId,rewardPoints+1)//签到积分是奖励分加基本分
        );

        //封装vo返回
        SignResultVO vo=new SignResultVO();
        vo.setSignDays(signDays);
        vo.setRewardPoints(rewardPoints);
        return vo;
    }
    public int countSignDays(String key,int len){
        //获取从本月第一天开始到今天为止的所有签到记录
        List<Long> result = redisTemplate.opsForValue()
                .bitField(key, BitFieldSubCommands.create().get(
                        BitFieldSubCommands.BitFieldType.unsigned(len)).valueAt(0));
        if (CollUtils.isEmpty(result)){
            return 0;
        }
        int num = result.get(0).intValue();
        // 2.定义一个计数器
        int count = 0;
        // 3.循环，与1做与运算，得到最后一个bit，判断是否为0，为0则终止，为1则继续
        while ((num & 1) == 1) {
            // 4.计数器+1
            count++;
            // 5.把数字右移一位，最后一位被舍弃，倒数第二位成了最后一位
            num >>>= 1;
        }
        return count;
    }
}
