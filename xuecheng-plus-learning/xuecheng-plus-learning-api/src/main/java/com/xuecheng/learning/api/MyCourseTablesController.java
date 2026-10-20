package com.xuecheng.learning.api;

import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.learning.model.dto.MyCourseTableParams;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcCourseTables;
import com.xuecheng.learning.service.MyCourseTablesService;
import com.xuecheng.learning.util.SecurityUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@Api(value = "我的课程表接口", tags = "我的课程表接口")
@Slf4j
@RestController
public class MyCourseTablesController {

    @Autowired
    MyCourseTablesService myCourseTablesService;


    @ApiOperation("添加选课")
    @PostMapping("/choosecourse/{courseId}")
    public XcChooseCourseDto addChooseCourse(@PathVariable("courseId") Long courseId) {
        //获取当前登录用户
        SecurityUtil.XcUser user = SecurityUtil.getUser();
        if (user == null) {
            XueChengPlusException.cast("请登录");
        }
        //用户id
        String userId = user.getId();
        //选课
        return myCourseTablesService.addChooseCourse(userId, courseId);
    }

    @ApiOperation("查询学习资格")
    @PostMapping("/choosecourse/learnstatus/{courseId}")
    public XcCourseTablesDto getLearnstatus(@PathVariable("courseId") Long courseId) {
        //获取当前登录用户
        SecurityUtil.XcUser user = SecurityUtil.getUser();
        if (user == null) {
            XueChengPlusException.cast("请登录");
        }
        String userId = user.getId();
        return myCourseTablesService.getLearningStatus(userId, courseId);

    }

    @ApiOperation("我的课程表")
    @GetMapping("/mycoursetable")
    public PageResult<XcCourseTables> mycoursetable(MyCourseTableParams params) {
        //获取登录用户
        SecurityUtil.XcUser user = SecurityUtil.getUser();
        if(user==null){
            XueChengPlusException.cast("请登录");
        }
        params.setUserId(user.getId());
        return myCourseTablesService.mycoursetable(params);
    }


//    CREATE TABLE learning_progress (
//            id BIGINT PRIMARY KEY AUTO_INCREMENT,
//            user_id BIGINT NOT NULL,
//            course_id BIGINT NOT NULL,
//            video_id BIGINT NOT NULL,
//            progress INT DEFAULT 0,        -- 当前观看到多少秒
//                    duration INT DEFAULT 0,        -- 视频总时长（秒）
//                    status TINYINT DEFAULT 0,      -- 0=学习中 1=已完成
//                    update_time DATETIME,
//            UNIQUE KEY uk_user_video (user_id, video_id)
//);

//        @Autowired
//        private LearningProgressService progressService;
//
//        @PostMapping("/report")
          //@ApiOperation("视频进度上报")
//        public Result report(@RequestBody ProgressDTO dto) {
//            progressService.reportProgress(dto);
//            return Result.ok();
//        }


//    @Service
//    public class LearningProgressService {
//
//        @Autowired
//        private StringRedisTemplate redisTemplate;
//
//        @Autowired
//        private RabbitTemplate rabbitTemplate;
//
//        public void reportProgress(ProgressDTO dto) {
//
//            String key = "learn:progress:" + dto.getUserId() + ":" + dto.getVideoId();
//
//            // 1️⃣ 获取旧值
//            String oldStr = redisTemplate.opsForValue().get(key);
//            int oldProgress = oldStr == null ? 0 : Integer.parseInt(oldStr);
//
//            // 2️⃣ 计算最大进度（核心🔥）
//            int newProgress = Math.max(oldProgress, dto.getProgress());
//
//            // 3️⃣ 写回Redis
//            redisTemplate.opsForValue().set(key, String.valueOf(newProgress));
//
//            // 4️⃣ 发送MQ（异步落库）
//            ProgressMessage msg = new ProgressMessage(
//                    dto.getUserId(),
//                    dto.getVideoId(),
//                    newProgress
//            );
//
//            rabbitTemplate.convertAndSend("progress.exchange", "progress.key", msg);
//        }


//    @RabbitListener(queues = "progress.queue")
//    @Component
//    public class ProgressConsumer {
//
//        @Autowired
//        private LearningProgressMapper mapper;
//
//        @Transactional
//        public void handle(ProgressMessage msg) {
//
//            LearningProgress record = mapper.select(
//                    msg.getUserId(),
//                    msg.getVideoId()
//            );
//
//            if (record == null) {
//                mapper.insert(msg);
//            } else {
//                mapper.updateMaxProgress(
//                        msg.getUserId(),
//                        msg.getVideoId(),
//                        msg.getProgress()
//                );
//            }
//        }
//    }


//    UPDATE learning_progress
//    SET progress = CASE
//    WHEN progress < #{progress} THEN #{progress}
//    ELSE progress
//    END,
//    update_time = NOW()
//    WHERE user_id = #{userId}
//    AND video_id = #{videoId};


    //--------------------------------------------------------------------------------------------------------------------
//    @ApiOperation("获取学习进度")
//    @GetMapping("/progress")
//    public RestResponse<Integer> getProgress(
//            @RequestParam Long userId,
//            @RequestParam Long videoId) {
//
//        Integer progress = progressService.getProgress(userId, videoId);
//        return RestResponse.success(progress);
//    }


//    @Service
//    public class LearningProgressService {
//
//        @Autowired
//        private StringRedisTemplate redisTemplate;
//
//        @Autowired
//        private LearningProgressMapper mapper;
//
//        public Integer getProgress(Long userId, Long videoId) {
//
//            String key = "learn:progress:" + userId + ":" + videoId;
//
//            // 1️⃣ 查Redis
//            String progressStr = redisTemplate.opsForValue().get(key);
//
//            if (progressStr != null) {
//                return Integer.parseInt(progressStr);
//            }
//
//            // 2️⃣ Redis没有 → 查数据库
//            LearningProgress record = mapper.select(userId, videoId);
//
//            if (record != null) {
//
//                int progress = record.getProgress();
//
//                // 3️⃣ 回写Redis（缓存预热）
//                redisTemplate.opsForValue().set(
//                        key,
//                        String.valueOf(progress),
//                        1, TimeUnit.HOURS
//                );
//
//                return progress;
//            }
//
//            // 4️⃣ 没有记录 → 从0开始
//            return 0;
//        }
//    }


//    @Select("SELECT * FROM learning_progress WHERE user_id = #{userId} AND video_id = #{videoId}")
//    LearningProgress select(@Param("userId") Long userId,
//                            @Param("videoId") Long videoId);
}
