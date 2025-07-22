package com.tianji.learning.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.mapper.LearningLessonMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 学生课程表 服务实现类
 * </p>
 *
 * @author 南哥
 * @since 2025-07-21
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LearningLessonServiceImpl extends ServiceImpl<LearningLessonMapper, LearningLesson> implements ILearningLessonService {
    private final CourseClient courseClient;
    private final CatalogueClient catalogueClient;



    @Override
    public void addUserLessons(Long userId, List<Long> courseIds) {
        List<CourseSimpleInfoDTO> cInfoList = courseClient.getSimpleInfoList(courseIds);
        if (CollUtils.isEmpty(cInfoList)){
            //课程不存在，无法添加
            log.error("课程信息不存在，无法添加");
            return;
        }
        //循环便利，处理learningLeson数据
        List<LearningLesson> list=new ArrayList<>(cInfoList.size());
        for (CourseSimpleInfoDTO cInfo:cInfoList) {
            LearningLesson lesson=new LearningLesson();
            //获取过期时间
            Integer validDuration = cInfo.getValidDuration();
            if (validDuration!=null&&validDuration>0){
                LocalDateTime now=LocalDateTime.now();
                lesson.setCreateTime(now);
                lesson.setExpireTime(now.plusMonths(validDuration));
            }
            //填充userId和courseid
            lesson.setUserId(userId);
            lesson.setCourseId(cInfo.getId());
            list.add(lesson);
        }
        //批量新增
        saveBatch(list);
    }

    @Override
    public PageDTO<LearningLessonVO> queryMyLessons(PageQuery query) {
        //获取userid
        Long userId = UserContext.getUser();
        //分页查询获取learninglessons集合records
        Page<LearningLesson> page = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .page(query.toMpPage("latest_learn_time", false));
        List<LearningLesson> records = page.getRecords();
        if (CollUtils.isEmpty(records)){
            return PageDTO.empty(page);
        }
        //调用额外方法来获取课程的详细信息，满足前端查询的信息
        Map<Long, CourseSimpleInfoDTO> cMap = queryCourseSimpleInfoList(records);
        //定义一vo集合，便利records，records中每条lesson的基础信息赋值给vo
        List<LearningLessonVO> list=new ArrayList<>(records.size());
        for (LearningLesson r:records) {
            LearningLessonVO vo= BeanUtils.copyBean(r,LearningLessonVO.class);
            //获取课程信息，补充到vo中
            CourseSimpleInfoDTO courseSimpleInfoDTO = cMap.get(r.getCourseId());
            vo.setCourseName(courseSimpleInfoDTO.getName());
            vo.setCourseCoverUrl(courseSimpleInfoDTO.getCoverUrl());
            vo.setSections(courseSimpleInfoDTO.getSectionNum());
            list.add(vo);
        }
        //返回分页结果
      return PageDTO.of(page,list);
    }

    @Override
    public LearningLessonVO queryMyCurrentLesson() {
        //获取用户id
        Long userId = UserContext.getUser();
        //查询学习状态为学习中的课程,并返回最新时间的一条数据
        LearningLesson lesson=lambdaQuery()
                .eq(LearningLesson::getUserId,userId)
                .eq(LearningLesson::getStatus, LessonStatus.LEARNING.getValue())
                .orderByDesc(LearningLesson::getLatestLearnTime)
                .last("limit 1")
                .one();
        if (lesson==null){
            return null;
        }
        //将查询到的课程的基本信息赋值为vo
        LearningLessonVO vo=BeanUtils.copyBean(lesson,LearningLessonVO.class);
        //查询课程的详细信息，赋值给vo
        CourseFullInfoDTO cInfo = courseClient.getCourseInfoById(vo.getCourseId(), false, false);
        if (cInfo==null){
            throw new BadRequestException("课程信息不存在");
        }
        vo.setCourseName(cInfo.getName());
        vo.setCourseCoverUrl(cInfo.getCoverUrl());
        vo.setSections(cInfo.getSectionNum());
        //查询总课程数量，并赋值给vo
        Integer courseAmount=lambdaQuery()
                .eq(LearningLesson::getUserId,userId)
                .count();
        vo.setCourseAmount(courseAmount);
        //查询小节的相关信息，并进行赋值vo
        List<CataSimpleInfoDTO> cataInfos = catalogueClient.batchQueryCatalogue(CollUtils.singletonList(lesson.getLatestSectionId()));
        if (!CollUtils.isEmpty(cataInfos)){
            CataSimpleInfoDTO cataInfo = cataInfos.get(0);
            vo.setLatestSectionName(cataInfo.getName());
            vo.setLatestSectionIndex(cataInfo.getCIndex());
        }
        return vo;
        //返回vo
    }

    @Override
    public void deleteLessons(Long userId, List<Long> courseIds) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("无效的用户ID");
        }
        if (courseIds == null || courseIds.isEmpty()) {
            throw new IllegalArgumentException("课程ID列表不能为空");
        }
        List<CourseSimpleInfoDTO> cInfoList = courseClient.getSimpleInfoList(courseIds);
        if (cInfoList.size() != courseIds.size()) {
            throw new BadRequestException("部分课程不存在");
        }
        try {
            boolean deleteRow=lambdaUpdate()  // 继承 ServiceImpl 后才能调用
                    .eq(LearningLesson::getUserId, userId)
                    .in(LearningLesson::getCourseId, courseIds)
                    .remove();  // 执行删除
            if (!deleteRow){
                log.warn("未删除任何数据，userId={},courseIds={}",userId,courseIds);
            }
        } catch (Exception e) {
            log.warn("课程删除失败，userId={},courseIds={}",userId,courseIds,e.getMessage());
            throw new BadRequestException("课程删除失败，请稍后再试");
        }
    }

    @Override
    public Long isLessonValid(Long courseId) {
        //获取用户id
        Long userId = UserContext.getUser();
        //获取用户的课表，查看是否含有该课程,并且查看课程是否过期
        List<LearningLesson> list = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .gt(LearningLesson::getExpireTime, LocalDateTime.now())
                .list();
        if (CollUtils.isEmpty(list)){
            return null;
        }
        return list.get(0).getId();
    }

    @Override
    public LearningLessonVO queryLessonSimpInfoById() {
        //获取用户id
        Long userId = UserContext.getUser();
        //获取用户的课表，查看是否含有该课程,并且查看课程是否过期
        List<LearningLesson> list = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .list();
        if (CollUtils.isEmpty(list)){
            return null;
        }
        LearningLesson lesson=list.get(0);
        LearningLessonVO vo=new LearningLessonVO();
        vo.setId(lesson.getId());
        vo.setCourseId(lesson.getCourseId());
        vo.setStatus(lesson.getStatus());
        vo.setLearnedSections(lesson.getLearnedSections());
        vo.setCreateTime(lesson.getCreateTime());
        vo.setExpireTime(lesson.getExpireTime());
        vo.setPlanStatus(lesson.getPlanStatus());
        return vo;
    }

    @Override
    public Integer countLearningLessonByCourse(Long courseId) {
      //查询拥有课程的人数
        Integer count = lambdaQuery()
                .eq(LearningLesson::getCourseId, courseId)
                .count();
       return count;
    }

    //定义额外的方法来通过远程调用查询课程的详细信息
    private Map<Long,CourseSimpleInfoDTO> queryCourseSimpleInfoList(List<LearningLesson> records){
        //获取课程id集合
        Set<Long> cIds = records.stream().map(LearningLesson::getCourseId).collect(Collectors.toSet());
        //查询课程信息
        List<CourseSimpleInfoDTO> cInfoList = courseClient.getSimpleInfoList(cIds);
        if (CollUtils.isEmpty(cInfoList)){
            //课程信息不存在，无法添加
            throw new BadRequestException("课程信息不存在");
        }
        //把课程信息处理为一个map集合，key是courseId，值是课程的信息
        Map<Long, CourseSimpleInfoDTO> cMap = cInfoList.stream()
                .collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));
        return cMap;
    }
}
