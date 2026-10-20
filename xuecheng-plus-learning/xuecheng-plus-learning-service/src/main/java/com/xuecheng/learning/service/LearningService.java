package com.xuecheng.learning.service;

import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.learning.model.dto.MyCourseTableParams;
import com.xuecheng.learning.model.po.XcCourseTables;

public interface LearningService {

    // 获取视频
    public RestResponse<String> getVideo(String userId, Long courseId, Long teachplanId, String mediaId);



}
