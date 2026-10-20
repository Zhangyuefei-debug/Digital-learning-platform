package com.xuecheng.learning.service;

import com.xuecheng.base.model.PageResult;
import com.xuecheng.learning.model.dto.MyCourseTableParams;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcCourseTables;

public interface MyCourseTablesService {

    /**
     *添加选课
     * @param userId
     * @param courseId
     * @return
     */
    public XcChooseCourseDto addChooseCourse(String userId, Long courseId);


    /**
     * 查询学习资格
     */
    public XcCourseTablesDto getLearningStatus(String userId, Long courseId);

    //我的课程表
    PageResult<XcCourseTables> mycoursetable(MyCourseTableParams params);

    boolean saveChooseCourseStauts(String choosecourseId);
}
