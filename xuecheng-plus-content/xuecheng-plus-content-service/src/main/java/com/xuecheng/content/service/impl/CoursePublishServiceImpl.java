package com.xuecheng.content.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.utils.StringUtils;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignclient.MediaServiceClient;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.mapper.CoursePublishPreMapper;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.model.po.CoursePublishPre;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.content.service.TeachplanService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;

import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class CoursePublishServiceImpl implements CoursePublishService {

    @Autowired
    private CourseBaseInfoService courseBaseInfoService;
    @Autowired
    private TeachplanService teachplanService;
    @Autowired
    private CoursePublishPreMapper coursePublishPreMapper;
    @Autowired
    private CoursePublishMapper coursePublishMapper;
    @Autowired
    private CourseBaseMapper courseBaseMapper;
    @Autowired
    private CourseMarketMapper courseMarketMapper;
    @Autowired
    private MqMessageService mqMessageService;
    @Autowired
    private MediaServiceClient mediaServiceClient;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;



    //获取课程预览信息
    @Override
    public CoursePreviewDto getCoursePreviewInfo(Long courseId) {
        CoursePreviewDto coursePreviewInfo = new CoursePreviewDto();

        //获取课程的基本信息
        coursePreviewInfo.setCourseBase(courseBaseInfoService.getCourseBaseInfo(courseId));

        //获取课程计划
        coursePreviewInfo.setTeachplans(teachplanService.findTeachplanTree(courseId));

        return coursePreviewInfo;
    }

    //提交审核 向课程预发布表写数据
    @Transactional
    @Override
    public void commitAudit(Long companyId, Long courseId) {

        //约束校验
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        //课程审核状态
        String auditStatus = courseBase.getAuditStatus();
        //当前审核状态为已提交不允许再次提交
        if("202003".equals(auditStatus)){
            XueChengPlusException.cast("当前为等待审核状态，审核完成可以再次提交。");
        }
        //本机构只允许提交本机构的课程
        if(!courseBase.getCompanyId().equals(companyId)){
            XueChengPlusException.cast("不允许提交其它机构的课程。");
        }

        //课程图片是否填写
        if(StringUtils.isEmpty(courseBase.getPic())){
            XueChengPlusException.cast("提交失败，请上传课程图片");
        }

        //添加课程预发布记录
        CoursePublishPre coursePublishPre = new CoursePublishPre();
        //课程基本信息加部分营销信息
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        BeanUtils.copyProperties(courseBaseInfo,coursePublishPre);
        //课程营销信息
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        //转为json
        String courseMarketJson = JSON.toJSONString(courseMarket);
        //将课程营销信息json数据放入课程预发布表
        coursePublishPre.setMarket(courseMarketJson);

        //查询课程计划信息
        List<TeachplanDto> teachplanTree = teachplanService.findTeachplanTree(courseId);
        if(teachplanTree.size()<=0){
            XueChengPlusException.cast("提交失败，还没有添加课程计划");
        }
        //转json
        String teachplanTreeString = JSON.toJSONString(teachplanTree);
        coursePublishPre.setTeachplan(teachplanTreeString);

        //设置预发布记录状态,已提交
        coursePublishPre.setStatus("202003");
        //教学机构id
        coursePublishPre.setCompanyId(companyId);
        //提交时间
        coursePublishPre.setCreateDate(LocalDateTime.now());
        CoursePublishPre coursePublishPreUpdate = coursePublishPreMapper.selectById(courseId);
        if(coursePublishPreUpdate == null){
            //添加课程预发布记录
            coursePublishPreMapper.insert(coursePublishPre);
        }else{
            coursePublishPreMapper.updateById(coursePublishPre);
        }

        //更新课程基本表的审核状态
        courseBase.setAuditStatus("202003");
        courseBaseMapper.updateById(courseBase);
    }


    //发布课程
    @Transactional
    @Override
    public void publish(Long companyId, Long courseId) {
        //查预发布表
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);
        if(coursePublishPre == null){
            XueChengPlusException.cast("数据不存在");
        }
        //查询状态
        String status = coursePublishPre.getStatus();
        //课程没有通过审核则不允许发布
        if(!"202004".equals(status)){
            XueChengPlusException.cast("课程没有审核通过不允许发布");
        }
        //向课程发布表写入数据
        CoursePublish coursePublish = new CoursePublish();
        BeanUtils.copyProperties(coursePublishPre,coursePublish);
        //将课程发布表中的课程发布状态设置为已发布
        coursePublish.setStatus("203002");
        //课程发布 先查询课程发布表，如果有则更新否则插入
        CoursePublish coursePublishObj = coursePublishMapper.selectById(courseId);
        if(coursePublishObj == null){
            coursePublishMapper.insert(coursePublish);
        }else {
            coursePublishMapper.updateById(coursePublish);
//            // === 新增：删除缓存，防止脏读 ===
//            // 第一次删除缓存
//            String cacheKey = "course:" + courseId;
//            redisTemplate.delete(cacheKey);
//            log.info("已删除缓存（第一次）：{}", cacheKey);
//
//            // 异步延时双删，避免缓存不一致
//            CompletableFuture.runAsync(() -> {
//                try {
//                    Thread.sleep(500); // 延迟0.5秒，可根据业务压力调整
//                    redisTemplate.delete(cacheKey);
//                    log.info("已延时删除缓存（第二次）：{}", cacheKey);
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                }
//            });
        }
        //向消息表写入数据
        saveCoursePublicMessage(courseId);
        //将预发布表数据删除
        coursePublishPreMapper.deleteById(courseId);
        //更新课程基本表中发布状态
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        courseBase.setStatus("203002");
        courseBaseMapper.updateById(courseBase);

    }


    //向消息表写入数据
    private void saveCoursePublicMessage(Long courseId){
        MqMessage mqMessage = mqMessageService.addMessage("course_publish", String.valueOf(courseId), null, null);
        if(mqMessage==null){
            XueChengPlusException.cast("添加消息失败");
        }
    }



// 生成静态化文件
    @Override
    public File generateCourseHtml(Long courseId) {

        //静态化文件
        File htmlFile  = null;

        try {
            //配置freemarker
            Configuration configuration = new Configuration(Configuration.getVersion());

            //加载模板
            //选指定模板路径,classpath下templates下
            //得到classpath路径
            //String classpath = this.getClass().getResource("/").getPath();
            //configuration.setDirectoryForTemplateLoading(new File(classpath + "/templates/"));
            configuration.setClassLoaderForTemplateLoading(this.getClass().getClassLoader(), "/templates");
            //设置字符编码
            configuration.setDefaultEncoding("utf-8");

            //指定模板文件名称
            Template template = configuration.getTemplate("course_template.ftl");

            //准备数据
            CoursePreviewDto coursePreviewInfo = this.getCoursePreviewInfo(courseId);

            Map<String, Object> map = new HashMap<>();
            map.put("model", coursePreviewInfo);

            //静态化
            //参数1：模板，参数2：数据模型
            String content = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);
//            System.out.println(content);
            //将静态化内容输出到文件中
            InputStream inputStream = IOUtils.toInputStream(content);
            //创建静态化文件
            htmlFile = File.createTempFile("course",".html");
            log.debug("课程静态化，生成静态文件:{}",htmlFile.getAbsolutePath());
            //输出流
            FileOutputStream outputStream = new FileOutputStream(htmlFile);
            IOUtils.copy(inputStream, outputStream);
        } catch (Exception e) {
            log.error("课程静态化异常:{}",e.toString());
            XueChengPlusException.cast("课程静态化异常");
        }

        return htmlFile;
    }

    //上传课程静态化页面
    @Override
    public void uploadCourseHtml(Long courseId, File file) {
        MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);
        String course = mediaServiceClient.uploadFile(multipartFile, "course/"+courseId+".html");
        if(course==null){
            XueChengPlusException.cast("上传静态文件异常");
        }
    }

    //查询课程发布信息
    @Override
    public CoursePublish getCoursePublish(Long courseId) {
        return coursePublishMapper.selectById(courseId);
    }

    //查询课程发布信息缓存
//    @Override
//    public CoursePublish getCoursePublishCache(Long courseId) {
//        // 第一重检查：先查缓存，防止大多数请求进入同步块
//        Object jsonObj = redisTemplate.opsForValue().get("course:" + courseId);
//        if(jsonObj != null){
//            String jsonString=jsonObj.toString();
//            log.info("从缓存中取数据，key：course" + courseId);
//            // 处理缓存穿透：如果缓存的是“null”，直接返回
//            if(jsonString.equals("null")) return null;
//            //反序列化，将 JSON 字符串转换回 Java 对象
//            return JSON.parseObject(jsonString, CoursePublish.class);
//        }else {
//            // 未命中，进入同步块，防止缓存击穿
//            synchronized (this) {  //作用是保证在同一时刻，只有一个线程可以执行特定的代码块
//                // 第二重检查：在拿到锁后再次检查缓存
//                Object innerJsonObj = redisTemplate.opsForValue().get("course:" + courseId);
//                if (innerJsonObj != null) {
//                    String innerJsonString  = innerJsonObj.toString();
//                    if (innerJsonString.equals("null")) return null;
//                    return JSON.parseObject(innerJsonString , CoursePublish.class);
//                }
//                // 缓存确实失效了，从数据库查询
//                log.info("从数据库中取数据");
//                CoursePublish coursePublish = getCoursePublish(courseId);
//            //如果数据不为空 将数据存入缓存中 设置300秒的过期时间
//            if (coursePublish != null) {                                  //将对象序列化为 JSON 字符串
//                redisTemplate.opsForValue().set("course" + courseId, JSON.toJSONString(coursePublish), 30, TimeUnit.SECONDS);
//            }else{
//                redisTemplate.opsForValue().set("course" + courseId, "null", 30, TimeUnit.SECONDS);
//            }
//                return coursePublish;
//            }
//        }
//    }


    //上述使用synchronized获取的锁只在当前jvm中有效 当有多个实例并发访问时锁失效 使用redisson实现分布式锁
    @Override
    public CoursePublish getCoursePublishCache(Long courseId) {
        // 第一重检查：先查缓存，防止大多数请求进入同步块
        Object jsonObj = redisTemplate.opsForValue().get("course:" + courseId);
        if(jsonObj != null){
            String jsonString=jsonObj.toString();
            log.info("从缓存中取数据，key：course" + courseId);
            // 处理缓存穿透：如果缓存的是“null”，直接返回
            if(jsonString.equals("null")) return null;
            //反序列化，将 JSON 字符串转换回 Java 对象
            return JSON.parseObject(jsonString, CoursePublish.class);
        }else {
                // 未命中，进入同步块，防止缓存击穿
                RLock lock = redissonClient.getLock("coursequerylock" + courseId);
                //我懂了 我现在的项目的逻辑是使用redis缓存已发布的课程信息，为了防止缓存击穿使用了redisson中的lock.lock()方法，这里不能用tryLock方法吧，否则直接返回false用户不就傻眼了只能阻塞然后获取锁之后再从缓存中取数据。
                // 而我之前的项目是使用分布式锁实现一人一单功能，使用用户id作为key，使用lock.tryLock方法，集群模式下获取锁失败的话证明该用户已经获取了该锁去下单了，直接返回false即可
                lock.lock();  //获取锁 阻塞式：如果锁已经被其他线程持有，它会一直阻塞等待，直到锁被释放。
                try {                                                                                                   //为什么要再检查一次？
                                                                                                                        //防止多个线程同时缓存未命中：
                                                                                                                        //线程 A 和线程 B 同时缓存未命中
                                                                                                                        // A 获取锁后查询数据库并写入缓存
                                                                                                                        //线程 B 等锁，锁释放后直接查询缓存即可，无需重复访问数据库
                                                                                                                        //这是经典的缓存双重检查机制。
                                                                                                                        // 第二重检查：在拿到锁后再次检查缓存
                    Object innerJsonObj = redisTemplate.opsForValue().get("course:" + courseId);
                    if (innerJsonObj != null) {
                        String innerJsonString  = innerJsonObj.toString();
                        if (innerJsonString.equals("null")) return null;
                        return JSON.parseObject(innerJsonString , CoursePublish.class);
                    }
                    // 缓存确实失效了，从数据库查询
                    log.info("从数据库中取数据");
                    CoursePublish coursePublish = getCoursePublish(courseId);
                    //如果数据不为空 将数据存入缓存中 设置300秒的过期时间
                    if (coursePublish != null) {                                  //将对象序列化为 JSON 字符串
                        redisTemplate.opsForValue().set("course:" + courseId, JSON.toJSONString(coursePublish), 300, TimeUnit.SECONDS);
                    }else{
                        redisTemplate.opsForValue().set("course:" + courseId, "null", 30, TimeUnit.SECONDS);
                    }
                    return coursePublish;
                }finally {
                    lock.unlock();  //实现了Lua脚本 释放锁
                }
        }
    }
}
