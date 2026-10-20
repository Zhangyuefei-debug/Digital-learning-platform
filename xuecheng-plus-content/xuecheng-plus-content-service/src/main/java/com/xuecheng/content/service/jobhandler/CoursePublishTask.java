package com.xuecheng.content.service.jobhandler;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.feignclient.SearchServiceClient;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.model.dto.CourseIndex;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MessageProcessAbstract;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.TimeUnit;


@Component
@Slf4j
public class CoursePublishTask extends MessageProcessAbstract {  //(实现层): 定义了 “做什么” 的具体业务逻辑。它实现了抽象类中的关键方法，将课程发布分解为多个具体步骤（静态化、建索引等）。

    @Autowired
    private CoursePublishService coursePublishService;

    @Autowired
    private CoursePublishMapper coursePublishMapper;

    @Autowired
    private SearchServiceClient searchServiceClient;

    @Autowired
    private RedisTemplate redisTemplate;


    //任务调度入口
    @XxlJob("CoursePublishJobHandler")
    public void coursePublishJobHandler() throws Exception {
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        log.debug("shardIndex="+shardIndex+",shardTotal="+shardTotal);
        //参数:分片序号、分片总数、消息类型、一次最多取到的任务数量、一次任务调度执行的超时时间
        process(shardIndex,shardTotal,"course_publish",30,60);
        //这些大多是 I/O 和远程调用，线程大部分时间在等待网络返回，CPU 用不满，所以可以开 30 个线程并发处理，不会像视频转码那样互相抢 CPU。
    }


    //执行课程发布任务的逻辑
    @Override
    public boolean execute(MqMessage mqMessage) {
        //从mqMessage中获取课程id
        Long courseId = Long.parseLong(mqMessage.getBusinessKey1());


        //向minio中写静态页面
        generateCourseHtml(mqMessage, courseId);


        //向Elasticsearch 中写数据
        saveCourseIndex(mqMessage, courseId);

        //向redis中写缓存
        saveCourseCache(mqMessage, courseId);


        //返回：true成功，false失败
        return true;
    }

    //生成课程静态化页面并上传至文件系统
    private void generateCourseHtml(MqMessage mqMessage, Long courseId){
        Long taskId = mqMessage.getId();
        MqMessageService mqMessageService = this.getMqMessageService();

        //做任务幂等性处理
        //取出该阶段的执行状态
        int stageOne = mqMessageService.getStageOne(taskId);
        if(stageOne == 1){
            log.debug("课程静态化已处理直接返回，课程id:{}",courseId);
            return ;
        }
        //生成静态化页面
        File file = coursePublishService.generateCourseHtml(courseId);
        //上传静态化页面
        if(file!=null){
            coursePublishService.uploadCourseHtml(courseId,file);
        }
        // 更新任务处理状态为1，表示处理完成
        mqMessageService.completedStageOne(taskId);
    }


    //保存课程索引信息
    private void saveCourseIndex(MqMessage mqMessage, Long courseId){
        Long taskId = mqMessage.getId();
        MqMessageService mqMessageService = this.getMqMessageService();

        //做任务幂等性处理
        //取出该阶段的执行状态
        int stageTwo = mqMessageService.getStageTwo(taskId);
        if(stageTwo>0){
            log.debug("课程索引任务完成，无需处理");
            return;
        }
        Boolean result = saveCourseIndex(courseId);
        if(result){
            //保存第二阶段状态
            mqMessageService.completedStageTwo(taskId);
        }
    }

    private Boolean saveCourseIndex(Long courseId) {

        //取出课程发布信息
        CoursePublish coursePublish = coursePublishMapper.selectById(courseId);
        //拷贝至课程索引对象
        CourseIndex courseIndex = new CourseIndex();
        BeanUtils.copyProperties(coursePublish,courseIndex);
        //远程调用搜索服务api添加课程信息到索引
        Boolean add = searchServiceClient.add(courseIndex);
        if(!add){
            XueChengPlusException.cast("添加索引失败");
        }
        return add;
    }

    //将课程信息缓存至redis
    public void saveCourseCache(MqMessage mqMessage,long courseId){
        Long taskId = mqMessage.getId();
        MqMessageService mqMessageService = this.getMqMessageService();
        //做任务幂等性处理
        //取出该阶段的执行状态
        int stageThree = mqMessageService.getStageThree(taskId);
        if(stageThree>0){
            log.debug("课程缓存任务完成，无需处理");
            return;
        }
        //缓存课程信息
        CoursePublish coursePublish = coursePublishMapper.selectById(courseId);
        redisTemplate.opsForValue().set("course:"+courseId, JSON.toJSONString(coursePublish), 2L, TimeUnit.HOURS);
        mqMessageService.completedStageThree(taskId);
    }
}
