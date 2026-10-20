package com.xuecheng.content.api;

import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.model.dto.TeacherSaveOrUpdateDto;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@Api(value = "师资管理编辑接口",tags = "师资管理编辑接口")
public class CourseTeacherController {

    @Autowired
    private CourseTeacherService courseTeacherService;

    @ApiOperation("根据课程id查询课程教师名称")
    @GetMapping("/courseTeacher/list/{courseId}")
    public List<CourseTeacher> list(@PathVariable @NotNull(message = "课程id不能为空") Long courseId) throws XueChengPlusException {
        return courseTeacherService.listTeacherByCourseId(courseId);
    }

    @ApiOperation("新增或修改课程教师")
    @PostMapping("/courseTeacher")
    public void saveOrUpdate(@RequestBody @Validated TeacherSaveOrUpdateDto dto) throws XueChengPlusException {
        courseTeacherService.saveOrUpdateTeacher(dto);
    }


    @ApiOperation("删除课程教师信息")
    @DeleteMapping("/courseTeacher/course/{courseId}/{id}")
    public void deleteById(@PathVariable Long courseId, @PathVariable Long id) throws XueChengPlusException {
        courseTeacherService.deleteTeacher(courseId,id);
    }


}
