package com.xuecheng.content.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.xuecheng.content.model.dto.TeacherSaveOrUpdateDto;
import com.xuecheng.content.model.po.CourseTeacher;

import javax.validation.constraints.NotNull;
import java.util.List;

public interface CourseTeacherService extends IService<CourseTeacher> {

    // 根据课程id查询课程教师
    List<CourseTeacher> listTeacherByCourseId(@NotNull(message = "课程id不能为空") Long courseId);

    // 新增或修改课程教师
    void saveOrUpdateTeacher(TeacherSaveOrUpdateDto dto);

    // 删除课程教师
    void deleteTeacher(Long courseId, Long id);
}
