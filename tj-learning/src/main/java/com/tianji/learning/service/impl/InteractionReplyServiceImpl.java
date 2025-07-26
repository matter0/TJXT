package com.tianji.learning.service.impl;

import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.enums.QuestionStatus;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.mapper.InteractionReplyMapper;
import com.tianji.learning.service.IInteractionReplyService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * <p>
 * 互动问题的回答或评论 服务实现类
 * </p>
 *
 * @author 南哥
 * @since 2025-07-25
 */
@Service
@RequiredArgsConstructor
public class InteractionReplyServiceImpl extends ServiceImpl<InteractionReplyMapper, InteractionReply> implements IInteractionReplyService {
    private final InteractionQuestionMapper questionMapper;




    @Override
    public void addReplay(ReplyDTO replyDTO) {
        Long userId = UserContext.getUser();
        InteractionReply reply = BeanUtils.copyBean(replyDTO, InteractionReply.class);
        reply.setUserId(userId);
        reply.setCreateTime(LocalDateTime.now());
        save(reply);
        //判断是不是回答
        if (replyDTO.getAnswerId()==null){
            //是回答
            InteractionQuestion question = questionMapper.selectById(replyDTO.getQuestionId());
            InteractionReply one = lambdaQuery()
                    .eq(InteractionReply::getQuestionId, replyDTO.getQuestionId())
                    .eq(InteractionReply::getUserId, userId)
                    .orderByDesc(InteractionReply::getCreateTime)
                    .one();
            //更新最新一次回答的id
            question.setLatestAnswerId(one.getId());
            //判断是否是学习，更新查看状态
            if (replyDTO.getIsStudent()){
                question.setStatus(QuestionStatus.CHECKED);
            }
            question.setAnswerTimes(question.getAnswerTimes()+1);
            questionMapper.updateById(question);
        }
    }





}
