package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.service.CourseCategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@Api(value = "课程分类相关接口",tags = "课程分类相关接口")
public class CourseCategoryController {

    @Autowired
    CourseCategoryService courseCategoryService;


    @GetMapping("/course-category/tree-nodes")
    @ApiOperation("查询课程分类树形结构")
    public List<CourseCategoryTreeDto> queryTreeNodes() {
        return courseCategoryService.queryTreeNodes("1");
    }

}
