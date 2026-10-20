package com.xuecheng.content.model.dto;

import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class CoursePreviewDto {  //在使用freemarker渲染生成视图时需要数据模型，此数据模型包括了基本信息、营销信息、课程计划、师资等信息。


    //课程基本信息,课程营销信息
    CourseBaseInfoDto courseBase;


    //课程计划信息
    List<TeachplanDto> teachplans;



}
