package com.tianji.learning.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;

/**
 * <p>
 * 互动提问的问题表 服务类
 * </p>
 *
 * @author 南哥
 * @since 2025-07-25
 */
public interface IInteractionQuestionService extends IService<InteractionQuestion> {



    void saveQuestion(QuestionFormDTO formDTO);

    void updateQuestion(Long questionId, QuestionFormDTO formDTO);

    PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery pageQuery);

    QuestionVO queryQuestionById(Long questionId);

    void deleteQuestionById(Long id);

    PageDTO<QuestionAdminVO> queryQuestionPageAdmin(QuestionAdminPageQuery query);

}
