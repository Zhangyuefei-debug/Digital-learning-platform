package com.xuecheng.media.service.jobhandler;

import com.xuecheng.base.utils.Mp4VideoUtil;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileProcessService;
import com.xuecheng.media.service.MediaFileService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;


@Slf4j
@Component
public class VideoTask {

    @Autowired
    MediaFileService mediaFileService;
    @Autowired
    MediaFileProcessService mediaFileProcessService;


    @Value("${videoprocess.ffmpegpath}")
    String ffmpegpath;

    @XxlJob("videoJobHandler")
    public void videoJobHandler() throws Exception {

        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        //分片（Sharding）：是为了负载均衡。让 3 台机器平分 100 万个任务，每台只查 33 万条，避免所有机器都查全表导致数据库崩溃。它解决了“查”的范围问题。
        List<MediaProcess> mediaProcessList = null;
        int size = 0;
        try {
            //取出cpu核心数作为一次处理数据的条数
            int processors = Runtime.getRuntime().availableProcessors();
            //一次处理视频数量不要超过cpu核心数
            mediaProcessList = mediaFileProcessService.getMediaProcessList(shardIndex, shardTotal, processors); //转码是典型的 CPU 密集型任务，真正干活的是 CPU，不是线程。取超过核心数的任务也没用，
                                                                                                                //反而会让线程频繁切换、互相抢 CPU，单个视频转得更慢，还可能把 JVM 和同机其他服务拖垮。
            size = mediaProcessList.size();
            log.debug("取出待处理视频任务{}条", size);
            if (size < 0) {
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        //启动size个线程的线程池
        ExecutorService threadPool = Executors.newFixedThreadPool(size);
        //计数器
        CountDownLatch countDownLatch = new CountDownLatch(size);
        //将处理任务加入线程池
        mediaProcessList.forEach(mediaProcess -> {
            threadPool.execute(() -> {
                try {
                    //任务id
                    Long taskId = mediaProcess.getId();
                    //抢占任务
                    //抢占（Locking）：是为了数据一致性。防止在“查出来”和“开始做”的时间窗口内，任务状态发生变化导致重复执行。它解决了“做”的权限问题。
                    boolean b = mediaFileProcessService.startTask(taskId);
                    if (!b) {
                        return;
                    }
                    log.debug("开始执行任务:{}", mediaProcess);
                    //下边是处理逻辑
                    //桶
                    String bucket = mediaProcess.getBucket();
                    //存储路径
                    String filePath = mediaProcess.getFilePath();
                    //原始视频的md5值
                    String fileId = mediaProcess.getFileId();
                    //原始文件名称
                    String filename = mediaProcess.getFilename();
                    //将要处理的文件下载到服务器上
                    File originalFile = mediaFileService.downloadFileFromMinIO(mediaProcess.getBucket(), mediaProcess.getFilePath());
                    if (originalFile == null) {
                        log.debug("下载待处理文件失败,originalFile:{}", mediaProcess.getBucket().concat(mediaProcess.getFilePath()));
                        mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", fileId, null, "下载待处理文件失败");
                        return;
                    }
                    //处理结束的视频文件
                    File mp4File = null;
                    try {
                        mp4File = File.createTempFile("mp4", ".mp4");
                    } catch (IOException e) {
                        log.error("创建mp4临时文件失败");
                        mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", fileId, null, "创建mp4临时文件失败");
                        return;
                    }
                    //视频处理结果
                    String result = "";
                    try {
                        //开始处理视频
                        Mp4VideoUtil videoUtil = new Mp4VideoUtil(ffmpegpath, originalFile.getAbsolutePath(), mp4File.getName(), mp4File.getAbsolutePath());
                        //开始视频转换，成功将返回success
                        result = videoUtil.generateMp4();
                    } catch (Exception e) {
                        e.printStackTrace();
                        log.error("处理视频文件:{},出错:{}", mediaProcess.getFilePath(), e.getMessage());
                    }
                    if (!result.equals("success")) {
                        //记录错误信息
                        log.error("处理视频失败,视频地址:{},错误信息:{}", bucket + filePath, result);
                        mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", fileId, null, result);
                        return;
                    }

                    //将mp4上传至minio
                    //mp4在minio的存储路径
                    String objectName = getFilePath(fileId, ".mp4");
                    //访问url
                    String url = "/" + bucket + "/" + objectName;
                    try {
                        mediaFileService.addMediaFilesToMinIO(mp4File.getAbsolutePath(), "video/mp4", bucket, objectName);
                        //将url存储至数据，并更新状态为成功，并将待处理视频记录删除存入历史
                        mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "2", fileId, url, null);
                    } catch (Exception e) {
                        log.error("上传视频失败或入库失败,视频地址:{},错误信息:{}", bucket + objectName, e.getMessage());
                        //最终还是失败了
                        mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", fileId, null, "处理后视频上传或入库失败");
                    }
                }finally {
                    countDownLatch.countDown();
                }
            });
        });
        //等待,给一个充裕的超时时间,防止无限等待，到达超时时间还没有处理完成则结束任务
        countDownLatch.await(30, TimeUnit.MINUTES);
    }

    private String getFilePath(String fileMd5,String fileExt){
        return   fileMd5.substring(0,1) + "/" + fileMd5.substring(1,2) + "/" + fileMd5 + "/" +fileMd5 +fileExt;
    }

}

//✅ 1. Java 里常用的线程池类型
//
//Java 的 Executors 工具类提供了几个工厂方法：
//
//线程池类型	特点	潜在问题
//Executors.newFixedThreadPool(n)	固定 n 个线程	容易任务积压 → 内部是 无界队列，高并发可能 OOM
//Executors.newCachedThreadPool()	线程数可动态增长	高并发时线程无限制增长 → CPU 被大量线程耗尽
//Executors.newSingleThreadExecutor()	单线程串行执行	简单，但不能并行
//Executors.newScheduledThreadPool(n)	支持定时任务	适合周期性任务
//
//核心问题：这些工厂方法封装了线程池，但 不允许你控制核心参数（比如最大线程数、队列类型、拒绝策略）。
//高并发场景容易导致：
//
//内存泄露
//
//OOM
//
//CPU 线程过多
//
//✅ 2. 官方推荐方式（Java 并发最佳实践）
//
//官方推荐：
//
//不要直接用 Executors 的工厂方法，而是使用 ThreadPoolExecutor 自己创建线程池，显式设置核心参数。
//
//ExecutorService threadPool = new ThreadPoolExecutor(
//        corePoolSize,       // 核心线程数
//        maximumPoolSize,    // 最大线程数
//        keepAliveTime,      // 空闲线程存活时间
//        TimeUnit.SECONDS,   // 时间单位
//        new LinkedBlockingQueue<>(queueCapacity), // 阻塞队列
//        new ThreadFactory() { ... },              // 线程工厂
//        new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
//);
//
//
//好处：
//
//可控线程数量 → 避免 CPU 被大量线程耗尽
//
//可控队列容量 → 避免任务无限积压
//
//可控拒绝策略 → 高并发时不会直接 OOM
//
//可控线程存活时间 → 避免线程空闲浪费资源
//
//✅ 3. 为什么说“不推荐固定大小线程池或缓存线程池”？
//
//FixedThreadPool：核心线程数固定，队列是无界队列，任务积压可能 OOM
//
//CachedThreadPool：线程数无限制，高并发直接创建大量线程，CPU 100% + 内存压力大
//
//所以在生产项目里 最好自己通过 ThreadPoolExecutor 配置线程池参数，尤其是视频转码这种 CPU 密集型任务。
//
//📌 总结一句话
//
//不要用 Executors.newFixedThreadPool() 或 newCachedThreadPool()，生产项目最好自己通过 ThreadPoolExecutor 配置核心线程数、最大线程数、队列长度和拒绝策略。