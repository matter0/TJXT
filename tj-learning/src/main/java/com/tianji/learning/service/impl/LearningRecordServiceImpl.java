package com.tianji.learning.service.impl;

import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.api.dto.leanring.LearningRecordDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.exceptions.DbException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.LearningRecordFormDto;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.SectionType;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.tianji.learning.service.ILearningRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 学习记录表 服务实现类
 * </p>
 *
 * @author 南哥
 * @since 2025-07-22
 */
@Service
@RequiredArgsConstructor
public class LearningRecordServiceImpl extends ServiceImpl<LearningRecordMapper, LearningRecord> implements ILearningRecordService {

    private final ILearningLessonService learningLessonService;
    private final CourseClient courseClient;

    @Override
    public LearningLessonDTO queryLearningRecordByCourse(Long courseId) {
        //获取用户id
        Long userId = UserContext.getUser();
        //根据用户id和courseid从learningLesson数据库表中找到对应的lessonid
        LearningLesson lesson = learningLessonService.queryByUserAndCourseId(userId, courseId);
        //然后根据lessonid和userid找到对应的record表，并从表中获取所需的返回信息
        Long lessonId = lesson.getId();
        List<LearningRecord> records = lambdaQuery()
                .eq(LearningRecord::getLessonId, lessonId)
                .list();
        //封装结果
      LearningLessonDTO dto=new LearningLessonDTO();
      dto.setId(lessonId);
      dto.setLatestSectionId(lesson.getLatestSectionId());
      dto.setRecords(BeanUtils.copyList(records, LearningRecordDTO.class));
      return dto;
    }

    @Override
    public void addLearningRecord(LearningRecordFormDto recordDto) {
       //获取登录用户
        Long userId = UserContext.getUser();
        //处理学习记录
        boolean finished=false;
        if (recordDto.getSectionType()== SectionType.VIDEO){
            //处理视频
            finished= handleVideoRecord(userId,recordDto);
        }else{
            //处理考试
            finished=handleExamRecord(userId,recordDto);
        }
       //处理课表信息
        handleLearningLessonChanges(recordDto,finished);

    }

    @Override
    public Integer countLearnedSections(Long userId, LocalDateTime begin, LocalDateTime end) {
        Integer count = lambdaQuery()
                .eq(LearningRecord::getUserId, userId)
                .eq(LearningRecord::getFinished, true)
                .gt(LearningRecord::getFinishTime, begin)
                .lt(LearningRecord::getFinishTime, end)
                .count();
        return count;
    }

    @Override
    public Integer countWeekLearnedSections(Long lessonId,Long userId, LocalDateTime begin,LocalDateTime end) {
        Integer count = lambdaQuery()
                .eq(LearningRecord::getUserId, userId)
                .eq(LearningRecord::getFinished, true)
                .eq(LearningRecord::getLessonId,lessonId)
                .gt(LearningRecord::getFinishTime, begin)
                .lt(LearningRecord::getFinishTime, end)
                .count();
        return count;
    }

    //处理视频
    private boolean handleVideoRecord(Long userId,LearningRecordFormDto recordDto){
        //查询记录是否存在，是否存在
        LearningRecord old = lambdaQuery()
                .eq(LearningRecord::getSectionId,recordDto.getSectionId())
                .eq(LearningRecord::getLessonId, recordDto.getLessonId())
                .one();
        boolean success=false;
        if (old==null){
            //不存在则新增学习记录
            LearningRecord newRecord=BeanUtils.copyBean(recordDto,LearningRecord.class);
            newRecord.setUserId(userId);
            newRecord.setUpdateTime(recordDto.getCommitTime());
            //判断学习进度是否超过一般，即是否学习完
          if (recordDto.getMoment()*2>recordDto.getDuration()){
              //学习完了
              newRecord.setFinished(true);
              newRecord.setFinishTime(recordDto.getCommitTime());
          }
          //写入数据库
            success=save(newRecord);
        }else {
            //已经存在，更新学习记录
            //查询记录是否被标记为已经学习完了
            if (old.getFinished()==true){
                //已经被标记过了
                old.setMoment(recordDto.getMoment());
                old.setUpdateTime(recordDto.getCommitTime());
            }else {
                //没有被标记过
                //判断新的进度是否超过了一半
                if (recordDto.getMoment()*2>recordDto.getDuration()){
                    //学习完了
                    old.setMoment(recordDto.getMoment());
                    old.setUpdateTime(recordDto.getCommitTime());
                    old.setFinished(true);
                    old.setFinishTime(recordDto.getCommitTime());
                }
            }
            success=updateById(old);
        }
        if (!success){
            throw new DbException("写入视频观看记录失败");
        }
        return  true;
    }
    //处理考试
    private boolean handleExamRecord(Long userId,LearningRecordFormDto recordDto){
        //查询记录是否存在，是否存在
        LearningRecord old = lambdaQuery()
                .eq(LearningRecord::getSectionId,recordDto.getSectionId())
                .eq(LearningRecord::getLessonId, recordDto.getLessonId())
                .one();
        boolean success=false;
        if (old==null){
            //不存在则新增学习记录
            LearningRecord newRecord=BeanUtils.copyBean(recordDto,LearningRecord.class);
            newRecord.setUserId(userId);
            newRecord.setFinished(true);
            newRecord.setFinishTime(recordDto.getCommitTime());
            //写入数据库
            success=save(newRecord);
        }else {
            //存在
            //判断状态是否是已经完成
            if (old.getFinished()){
                return true;
            }else {
                //否则更新记录中的状态
                old.setFinishTime(recordDto.getCommitTime());
                old.setFinished(true);
                success=save(old);
            }
        }
        if (!success){
            throw new DbException("写入考试记录失败");
        }
        return  true;
    }
    //修改课表信息
    private void handleLearningLessonChanges(LearningRecordFormDto recordDto,boolean finished){
        boolean success=false;
        //更新课表数据中的完成小节的数量
        LearningLesson lesson = learningLessonService.getById(recordDto.getLessonId());
        if (lesson==null){
            throw new BizIllegalException("课程不存在，无法更新数据");
        }
        if (finished){
            lesson.setLearnedSections(lesson.getLearnedSections()+1);
        }
        //查询该课程中的总的节数
        CourseFullInfoDTO courseInfo = courseClient.getCourseInfoById(lesson.getCourseId(), false, false);
        if (courseInfo==null){
            throw new BizIllegalException("课程不存在，无法更新数据");
        }
        Integer sectionNum = courseInfo.getSectionNum();
        //判读课程是否学完
        if ((lesson.getLearnedSections()+1)>=sectionNum){
            lesson.setStatus(LessonStatus.FINISHED);
        }
        //更新课表数据中最近学习的小节序号，以及最近学习的时间
        lesson.setLatestLearnTime(recordDto.getCommitTime());
        lesson.setLatestSectionId(recordDto.getSectionId());
        //写入数据库
       success=learningLessonService.updateById(lesson);
       if (!success){
           throw new BizIllegalException("更新课表信息失败");
       }
    }

}
