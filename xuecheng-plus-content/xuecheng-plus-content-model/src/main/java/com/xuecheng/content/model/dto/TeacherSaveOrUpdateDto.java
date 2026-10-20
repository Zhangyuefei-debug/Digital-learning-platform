package com.xuecheng.content.model.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class TeacherSaveOrUpdateDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 课程标识
     */
    @NotNull(message = "新增或者修改时课程id不能为空")
    private Long courseId;

    /**
     * 教师标识
     */
    @NotNull(message = "新增或者修改时课程教师名不能为空")
    private String teacherName;

    /**
     * 教师职位
     */
    private String position;

    /**
     * 教师简介
     */
    private String introduction;

    /**
     * 照片
     */
    private String photograph;






}



