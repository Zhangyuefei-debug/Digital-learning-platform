package com.xuecheng.content.api;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.service.CoursePublishService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

@RestController
@Api(value = "课程发布接口",tags = "课程发布接口")
public class CoursePublishController {

    @Autowired
    private CoursePublishService coursePublishService;

    @GetMapping("/coursepreview/{courseId}")
    @ApiOperation("课程预览接口")
    public ModelAndView preview(@PathVariable("courseId") Long courseId){

        CoursePreviewDto coursePreviewInfo = coursePublishService.getCoursePreviewInfo(courseId);

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("model",coursePreviewInfo);
        modelAndView.setViewName("course_template");
        return modelAndView;
    }

    @ApiOperation("课程提交审核接口")
    @ResponseBody   //@ResponseBody 就是告诉 Spring 不要渲染页面，把返回的数据直接给客户端。
    @PostMapping("/courseaudit/commit/{courseId}")
    public void commitAudit(@PathVariable("courseId") Long courseId){
        Long companyId = 1232141425L;
        coursePublishService.commitAudit(companyId,courseId);
    }


    @ApiOperation("课程发布")
    @ResponseBody  //@ResponseBody 就是告诉 Spring 不要渲染页面，把返回的数据直接给客户端。
    @PostMapping ("/coursepublish/{courseId}")
    public void coursepublish(@PathVariable("courseId") Long courseId){
        Long companyId = 1232141425L;
        coursePublishService.publish(companyId,courseId);
    }


    @ApiOperation("查询课程发布信息")
    @ResponseBody
    @GetMapping("/r/coursepublish/{courseId}")
    public CoursePublish getCoursepublish(@PathVariable("courseId") Long courseId) {
        CoursePublish coursePublish = coursePublishService.getCoursePublish(courseId);
        return coursePublish;
    }

    @ApiOperation("获取课程发布信息")
    @ResponseBody
    @GetMapping("/course/whole/{courseId}")  //点击学习按钮访问该接口
    public CoursePreviewDto getCoursePublish(@PathVariable("courseId") Long courseId){
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
