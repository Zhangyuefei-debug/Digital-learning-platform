package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseTeacherMapper;
import com.xuecheng.content.model.dto.TeacherSaveOrUpdateDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class CourseTeacherServiceImpl extends ServiceImpl<CourseTeacherMapper, CourseTeacher> implements CourseTeacherService {

    @Autowired
    private CourseTeacherMapper courseTeacherMapper;
    @Autowired
    private CourseBaseMapper courseBaseMapper;
    @Autowired
    private CourseTeacherService courseTeacherService;


    // 根据课程id查询课程教师名称
    @Override
    public List<CourseTeacher> listTeacherByCourseId(Long courseId) {

        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper = queryWrapper.eq(CourseTeacher::getCourseId, courseId);
        List<CourseTeacher> courseTeachers = courseTeacherMapper.selectList(queryWrapper);
        if (courseTeachers== null){
            return Collections.emptyList();
        }
        return courseTeachers;
    }

    // 新增或修改课程教师
    @Override
    public void saveOrUpdateTeacher(TeacherSaveOrUpdateDto dto) {
        Long id = dto.getId();
        Long courseId = dto.getCourseId();
        CourseTeacher courseTeacher = new CourseTeacher();
        BeanUtils.copyProperties(dto,courseTeacher);
        if(id == null){
            //新增
            courseTeacher.setCreateDate(LocalDateTime.now());
            int insert = courseTeacherMapper.insert(courseTeacher);
            if (insert <= 0)
                XueChengPlusException.cast("新增课程教师失败");
        }else {
            //修改
            LambdaQueryWrapper<CourseTeacher> q1 = new LambdaQueryWrapper<>();
            q1.eq(CourseTeacher::getId, id).eq(CourseTeacher::getCourseId, courseId);
            int i = courseTeacherMapper.updateById(courseTeacher);
            if (i <= 0)
                XueChengPlusException.cast("修改课程教师失败");
        }
    }

    // 删除课程教师信息
    @Override
    public void deleteTeacher(Long courseId, Long id) {
        //1. 根据id与courseId删除数据
        if (courseId == null || id == null)
            XueChengPlusException.cast("id或者课程id为空");

        /*查询课程id是否存在于数据库*/
        if (courseBaseMapper.selectById(courseId) == null)
            XueChengPlusException.cast("课程id不合法");


        LambdaQueryWrapper<CourseTeacher> q = new LambdaQueryWrapper<>();
        q.eq(CourseTeacher::getId, id).eq(CourseTeacher::getCourseId, courseId);
        int delete = courseTeacherMapper.delete(q);
        if (delete <= 0)
            XueChengPlusException.cast("删除课程教师失败");
    }
}
