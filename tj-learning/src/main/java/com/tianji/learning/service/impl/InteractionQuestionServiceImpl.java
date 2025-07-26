package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.cache.CategoryCache;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CategoryClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.client.search.SearchClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.mapper.InteractionReplyMapper;
import com.tianji.learning.service.IInteractionQuestionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 互动提问的问题表 服务实现类
 * </p>
 *
 * @author 南哥
 * @since 2025-07-25
 */
@Service
@RequiredArgsConstructor
public class InteractionQuestionServiceImpl extends ServiceImpl<InteractionQuestionMapper, InteractionQuestion> implements IInteractionQuestionService {
    private final InteractionReplyMapper replyMapper;
    private final UserClient userClient;
    private final CourseClient courseClient;
    private final CatalogueClient catalogueClient;
    private final CategoryCache categoryCache;
    private final SearchClient searchClient;



    @Override
    public void saveQuestion(QuestionFormDTO formDTO) {
        //获取登录用户
        Long userId = UserContext.getUser();
        //数据转换
        InteractionQuestion interactionQuestion = BeanUtils.copyBean(formDTO, InteractionQuestion.class);
        //补充数据
        interactionQuestion.setUserId(userId);
        //保存问题
        save(interactionQuestion);
    }

    @Override
    public void updateQuestion(Long questionId, QuestionFormDTO formDTO) {
        //获取原始数据
        InteractionQuestion question = lambdaQuery()
                .eq(InteractionQuestion::getId, questionId)
                .one();
        question.setTitle(formDTO.getTitle());
        question.setDescription(formDTO.getDescription());
        question.setAnonymity(formDTO.getAnonymity());
        updateById(question);
    }

    @Override
    public PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery query) {
        //参数校验
        Long courseId = query.getCourseId();
        Long sectionId = query.getSectionId();
        if (courseId==null&&sectionId==null){
            throw new BadRequestException("课程id和小小节id不能都为空");
        }
        //分页查询
        //查询数据库表中所有符合要求的数据questions集合，不被隐藏的,自己的还是全部的
        Page<InteractionQuestion> page = lambdaQuery()
                .select(InteractionQuestion.class, info -> !info.getProperty().equals("description"))  //过滤descirption这行数据，不显示
                .eq(courseId != null, InteractionQuestion::getCourseId, courseId)
                .eq(sectionId != null, InteractionQuestion::getSectionId, sectionId)
                .eq(InteractionQuestion::getHidden, false)
                .eq(query.getOnlyMine(), InteractionQuestion::getUserId, UserContext.getUser())
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<InteractionQuestion> questions = page.getRecords();
        if (CollUtils.isEmpty(questions)){
            return PageDTO.empty(page);
        }
        //定义存储所用用户id的集合userIds和所有回答id的集合answerIds
        Set<Long> userIds=new HashSet<>();
        Set<Long> answerIds=new HashSet<>();
        //遍历questions给suerIds和answerIds赋值，赋值userIds时用户必须是不匿名的
        for (InteractionQuestion question: questions) {
            if (!question.getAnonymity()){
                userIds.add(question.getUserId());
            }
            answerIds.add(question.getLatestAnswerId());
        }


        //定义一个map集合，key为answerid，value为replays，
        Map<Long, InteractionReply> replayMap=new HashMap<>(answerIds.size());
        //根据answerIds查询所有数据并赋值为replays集合，并且将查出来的回答者的id添加奥userIds集合中去，给map集合赋值
        answerIds.remove(null);
        if (CollUtils.isNotEmpty(answerIds)){
            List<InteractionReply> replays = replyMapper.selectBatchIds(answerIds);
            for (InteractionReply replay:replays) {
                replayMap.put(replay.getId(),replay);
                if (!replay.getAnonymity()){//匿名用户不做查询
                    userIds.add(replay.getUserId());
                }
            }
        }
        //遍历users集合，并以userId为key，user为value赋值给map集合
        Map<Long ,UserDTO> userMap=new HashMap<>(userIds.size());
        //使用远程调用接口根据userIds查询所有的users集合
        userIds.remove(null);
        if (CollUtils.isNotEmpty(userIds)){
            List<UserDTO> userDTOS = userClient.queryUserByIds(userIds);
            for (UserDTO userdto: userDTOS) {
                userMap.put(userdto.getId(),userdto);
            }
        }
        //定义QuestionVo集合
        List<QuestionVO> list=new ArrayList<>();
        //遍历questions,给questionVo集合赋值
        for (InteractionQuestion question:questions) {
            QuestionVO vo=new QuestionVO();
            //赋值问题id，问题标题，问题回答数量，问题提问时间,是否匿名,提问者id
            vo.setId(question.getId());
            vo.setTitle(question.getTitle());
            vo.setAnswerTimes(question.getAnswerTimes());
            vo.setCreateTime(question.getCreateTime());
            vo.setAnonymity(question.getAnonymity());
            //根据提问者id查询userMap赋值提问者昵称，提问者头像
            if (!question.getAnonymity()){
                if (userMap.get(vo.getUserId())!=null){
                    vo.setUserId(question.getUserId());
                    vo.setUserName(userMap.get(vo.getUserId()).getName());
                    vo.setUserIcon(userMap.get(vo.getUserId()).getIcon());
                }
            }
            //根据answerid查询replayMap，赋值回答内容，查询回答者id并查询usermap辅助回答者用户昵称
            if (replayMap.get(question.getLatestAnswerId())!=null){
                vo.setLatestReplyContent(replayMap.get(question.getLatestAnswerId()).getContent());
                if (!replayMap.get(question.getLatestAnswerId()).getAnonymity()){
                    vo.setLatestReplyUser(userMap.get(replayMap.get(question.getLatestAnswerId()).getUserId()).getName());
                }
            }
            list.add(vo);
        }
        return PageDTO.of(page,list);
    }

    @Override
    public QuestionVO queryQuestionById(Long questionId) {
        //根据id查询数据
        InteractionQuestion question = getById(questionId);
        //数据校验
        if (question==null){
            //没有数据或者数据被隐藏了
            return null;
        }
        //查询提问者信息
        UserDTO user=null;
        if (!question.getAnonymity()){
            user=userClient.queryUserById(question.getUserId());
        }
        //封装信息
        QuestionVO vo = BeanUtils.copyBean(question, QuestionVO.class);
        if (user!=null){
            vo.setUserName(user.getName());
            vo.setUserIcon(user.getIcon());
        }
        return vo;
    }

    @Override
    @Transactional
    public void deleteQuestionById(Long id) {
        //根据id查询数据
        InteractionQuestion question = getById(id);
        if (question==null){
            throw new BadRequestException("数据不存在");
        }
        if (!question.getUserId().equals(UserContext.getUser())){
          throw new BadRequestException("不是当前用户提出的问题");
        }
        deleteQuestionById(id);
        replyMapper.deleteByQuestionId(id);
    }

    @Override
    public PageDTO<QuestionAdminVO> queryQuestionPageAdmin(QuestionAdminPageQuery query) {
        //处理课程名称，得到课程id
        List<Long> courseIds=null;
        if (StringUtils.isNotBlank(query.getCourseName())){
            courseIds=searchClient.queryCoursesIdByName(query.getCourseName());
            if (CollUtils.isEmpty(courseIds)){
                return PageDTO.empty(0L,0L);
            }
        }
        //分页查询得到基础数据
        Integer status=query.getStatus();
        LocalDateTime begin = query.getBeginTime();
        LocalDateTime end = query.getEndTime();
        Page<InteractionQuestion> page = lambdaQuery()
                .in(courseIds != null, InteractionQuestion::getCourseId, courseIds)
                .eq(status != null, InteractionQuestion::getStatus, status)
                .gt(begin != null, InteractionQuestion::getCreateTime, begin)
                .lt(end != null, InteractionQuestion::getCreateTime, end)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<InteractionQuestion> records = page.getRecords();
        if (CollUtils.isEmpty(records)){
            return PageDTO.empty(page);
        }
        //准备Vo需要的数据：用户数据，课程数据，章节数据
        Set<Long> userIds=new HashSet<>();
        Set<Long> cIds=new HashSet<>();
        Set<Long> cataIds=new HashSet<>();
        //获取各种数据的id集合
        for (InteractionQuestion question:records) {
            userIds.add(question.getUserId());
            cIds.add(question.getCourseId());
            cataIds.add(question.getSectionId());
            cataIds.add(question.getChapterId());
        }
        //根据id查询用户
        List<UserDTO> userDTOS = userClient.queryUserByIds(userIds);
        //将数据存入userMap集合中
        Map<Long,UserDTO> userMap=new HashMap<>(userDTOS.size());
        if (CollUtils.isNotEmpty(userDTOS)){
            for (UserDTO user: userDTOS) {
                userMap.put(user.getId(),user);
            }
        }
        // 3.3.根据id查询课程
        List<CourseSimpleInfoDTO> cInfos = courseClient.getSimpleInfoList(cIds);
        Map<Long, CourseSimpleInfoDTO> cInfoMap = new HashMap<>(cInfos.size());
        if (CollUtils.isNotEmpty(cInfos)) {
            cInfoMap = cInfos.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));
        }

        // 3.4.根据id查询章节
        List<CataSimpleInfoDTO> catas = catalogueClient.batchQueryCatalogue(cataIds);
        Map<Long, String> cataMap = new HashMap<>(catas.size());
        if (CollUtils.isNotEmpty(catas)) {
            cataMap = catas.stream()
                    .collect(Collectors.toMap(CataSimpleInfoDTO::getId, CataSimpleInfoDTO::getName));
        }
        // 4.封装VO
        List<QuestionAdminVO> voList = new ArrayList<>(records.size());
        for (InteractionQuestion q : records) {
            // 4.1.将PO转VO，属性拷贝
            QuestionAdminVO vo = BeanUtils.copyBean(q, QuestionAdminVO.class);
            voList.add(vo);
            // 4.2.用户信息
            UserDTO user = userMap.get(q.getUserId());
            if (user != null) {
                vo.setUserName(user.getName());
            }
            // 4.3.课程信息以及分类信息
            CourseSimpleInfoDTO cInfo = cInfoMap.get(q.getCourseId());
            if (cInfo != null) {
                vo.setCourseName(cInfo.getName());
                vo.setCategoryName(categoryCache.getCategoryNames(cInfo.getCategoryIds()));
            }
            // 4.4.章节信息
            vo.setChapterName(cataMap.getOrDefault(q.getChapterId(), ""));
            vo.setSectionName(cataMap.getOrDefault(q.getSectionId(), ""));
        }
        return PageDTO.of(page, voList);
    }
}
