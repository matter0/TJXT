//package com.tianji.remark.service.impl;
//
//import com.baomidou.mybatisplus.core.conditions.Wrapper;
//import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
//import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
//import com.tianji.common.utils.BeanUtils;
//import com.tianji.common.utils.StringUtils;
//import com.tianji.common.utils.UserContext;
//import com.tianji.remark.domain.dto.LikeRecordFormDTO;
//import com.tianji.remark.domain.dto.LikedTimesDTO;
//import com.tianji.remark.domain.po.LikedRecord;
//import com.tianji.remark.mapper.LikedRecordMapper;
//import com.tianji.remark.service.ILikedRecordService;
//import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.annotation.Bean;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//import static com.tianji.common.constants.MqConstants.Exchange.LIKE_RECORD_EXCHANGE;
//import static com.tianji.common.constants.MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE;
//
///**
// * <p>
// * 点赞记录表 服务实现类
// * </p>
// *
// * @author 南哥
// * @since 2025-07-27
// */
//
//@RequiredArgsConstructor
//public class LikedRecordServiceImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {
//    private final RabbitMqHelper mqHelper;
//
//
//
//    @Override
//    public void addLikeRecord(LikeRecordFormDTO recordFormDTO) {
//        //基于参数判断是点赞还是取消点赞
//        boolean success=recordFormDTO.getLiked()?like(recordFormDTO):unlike(recordFormDTO);
//        //判断是否成功
//        if (!success){
//            return;
//        }
//        //如果点赞执行成功，统计该业务的点赞总数
//        Integer likedTimes = lambdaQuery()
//                .eq(LikedRecord::getBizId, recordFormDTO.getBizId())
//                .count();
//        //发送mq通知
//        mqHelper.send(
//                LIKE_RECORD_EXCHANGE,
//                StringUtils.format(LIKED_TIMES_KEY_TEMPLATE,recordFormDTO.getBizType()),
//                LikedTimesDTO.of(recordFormDTO.getBizId(),likedTimes)
//        );
//
//
//
//
//    }
//
//    @Override
//    public Set<Long> isBizLiked(List<Long> bizIds) {
//        Long userId = UserContext.getUser();
//        List<LikedRecord> list = lambdaQuery()
//                .in(LikedRecord::getBizId, bizIds)
//                .eq(LikedRecord::getUserId, userId)
//                .list();
//        return list.stream()
//                .map(LikedRecord::getBizId)
//                .collect(Collectors.toSet());
//    }
//
//    private boolean like(LikeRecordFormDTO recordFormDTO){
//        Long userId = UserContext.getUser();
//        //查询点赞记录
//        Integer count = lambdaQuery()
//                .eq(LikedRecord::getBizId, recordFormDTO.getBizId())
//                .eq(LikedRecord::getUserId, userId)
//                .count();
//        //判断是否存在，如果存在就直接退出
//        if (count>0){
//            return false;
//        }
//        //如果不存在则新增
//        LikedRecord record = BeanUtils.copyBean(recordFormDTO, LikedRecord.class);
//        record.setUserId(userId);
//        save(record);
//        return true;
//    }
//    private boolean unlike(LikeRecordFormDTO recordFormDTO){
//        return remove(new QueryWrapper<LikedRecord>().lambda()
//                .eq(LikedRecord::getBizId,recordFormDTO.getBizId())
//                .eq(LikedRecord::getUserId,UserContext.getUser())
//        );
//    }
//}
