package com.xuecheng.content.api;


import com.alibaba.fastjson.JSON;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.service.CoursePublishService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/open")
@Api(value = "课程公开接口",tags = "课程公开接口")  //直接点击视频访问该接口
public class CourseOpenController {

    @Autowired
    private CourseBaseInfoService courseBaseInfoService;

    @Autowired
    private CoursePublishService coursePublishService;


    @GetMapping("/course/whole/{courseId}")
    public CoursePreviewDto getPreViewInfo(@PathVariable("courseId") Long courseId){
        //获取课程预览信息
        //return coursePublishService.getCoursePreviewInfo(courseId);
        //查询课程发布信息
        CoursePublish coursePublish =coursePublishService.getCoursePublishCache(courseId);
        if (coursePublish == null){
            return new CoursePreviewDto();
        }
        //查询课程基本信息
        CourseBaseInfoDto courseBaseInfo =new CourseBaseInfoDto();
        BeanUtils.copyProperties(coursePublish,courseBaseInfo);
        //查询课程计划
        List<TeachplanDto> teachplans = JSON.parseArray(coursePublish.getTeachplan(), TeachplanDto.class);
        CoursePreviewDto coursePreviewInfo = new CoursePreviewDto();
        coursePreviewInfo.setCourseBase(courseBaseInfo);
        coursePreviewInfo.setTeachplans(teachplans);
        return coursePreviewInfo;

    }
}
