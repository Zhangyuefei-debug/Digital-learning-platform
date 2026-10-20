package com.xuecheng.learning.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.mapper.XcChooseCourseMapper;
import com.xuecheng.learning.mapper.XcCourseTablesMapper;
import com.xuecheng.learning.model.dto.MyCourseTableParams;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcChooseCourse;
import com.xuecheng.learning.model.po.XcCourseTables;
import com.xuecheng.learning.service.MyCourseTablesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class MyCourseTablesServiceImpl implements MyCourseTablesService {

    @Autowired
    ContentServiceClient contentServiceClient;
    @Autowired
    XcChooseCourseMapper xcChooseCourseMapper;
    @Autowired
    XcCourseTablesMapper xcCourseTablesMapper;


    @Transactional
    @Override
    public XcChooseCourseDto addChooseCourse(String userId, Long courseId) {
        //远程调用查询已发布课程
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        if(coursepublish == null){
            XueChengPlusException.cast("课程不存在");
        }
        //获取课程的收费规则
        String charge = coursepublish.getCharge();
        //选课记录
        XcChooseCourse chooseCourse =null;
        //判断
        if(charge.equals("201000")){
            //向选课记录表中添加免费课程数据
            chooseCourse=addFreeCourse(userId,coursepublish);
            //添加到我的课程表
            XcCourseTables xcCourseTables = addCourseTables(chooseCourse);

        }else{
            //仅向选课记录表中添加收费课程数据
            chooseCourse = addChargeCourse(userId, coursepublish);
        }
        XcChooseCourseDto xcChooseCourseDto = new XcChooseCourseDto();
        BeanUtils.copyProperties(chooseCourse,xcChooseCourseDto);
        //判断学生的学习资格
        XcCourseTablesDto xcCourseTablesDto = getLearningStatus(userId, courseId);
        xcChooseCourseDto.setLearnStatus(xcCourseTablesDto.getLearnStatus());
        return xcChooseCourseDto;
    }

    //学习资格，[{"code":"702001","desc":"正常学习"},{"code":"702002","desc":"没有选课或选课后没有支付"},{"code":"702003","desc":"已过期需要申请续期或重新支付"}]
    //判断学习资格
    @Override
    public XcCourseTablesDto getLearningStatus(String userId, Long courseId) {
        XcCourseTablesDto xcCourseTablesDto = new XcCourseTablesDto();
        //查询我的课程表
        XcCourseTables xcCourseTables = getXcCourseTables(userId, courseId);
        //若没有数据 证明没有学习资格
        if(xcCourseTables == null){
            xcCourseTablesDto.setLearnStatus("702002");
            return xcCourseTablesDto;
        }
        BeanUtils.copyProperties(xcCourseTables,xcCourseTablesDto);
        //如果查到了 判断是否过期
        if(xcCourseTables.getValidtimeEnd().isBefore(LocalDateTime.now())){
            xcCourseTablesDto.setLearnStatus("702003");
            return xcCourseTablesDto;
        }else{
            xcCourseTablesDto.setLearnStatus("702001");
            return xcCourseTablesDto;
        }
    }


    //向选课记录表中添加数据
    private XcChooseCourse addFreeCourse(String userId, CoursePublish coursepublish) {
        //如果选课表中有该课程记录且状态为选课成功则直接返回
        LambdaQueryWrapper<XcChooseCourse> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(XcChooseCourse::getUserId,userId);
        queryWrapper.eq(XcChooseCourse::getCourseId,coursepublish.getId());
        queryWrapper.eq(XcChooseCourse::getOrderType,"700001");
        queryWrapper.eq(XcChooseCourse::getStatus,"701001");
        List<XcChooseCourse> xcChooseCourses = xcChooseCourseMapper.selectList(queryWrapper);
        if (!xcChooseCourses.isEmpty()) {
            return xcChooseCourses.get(0);
        }
        //添加选课记录信息
        XcChooseCourse xcChooseCourse = new XcChooseCourse();
        xcChooseCourse.setCourseId(coursepublish.getId());
        xcChooseCourse.setCourseName(coursepublish.getName());
        xcChooseCourse.setCoursePrice(0f);//免费课程价格为0
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCompanyId(coursepublish.getCompanyId());
        xcChooseCourse.setOrderType("700001");//免费课程
        xcChooseCourse.setCreateDate(LocalDateTime.now());
        xcChooseCourse.setStatus("701001");//选课成功

        xcChooseCourse.setValidDays(365);//免费课程默认365
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(365));
        int insert = xcChooseCourseMapper.insert(xcChooseCourse);
        if(insert<=0){
            XueChengPlusException.cast("添加选课记录失败");
        }

        return xcChooseCourse;

    }

    //向我的课程表添加数据
    private XcCourseTables addCourseTables(XcChooseCourse xcChooseCourse) {
        //选课记录完成且未过期可以添加课程到课程表
        String status = xcChooseCourse.getStatus();
        if (!"701001".equals(status)){
            XueChengPlusException.cast("选课未成功，无法添加到课程表");
        }
        //查询我的课程表
        XcCourseTables xcCourseTables = getXcCourseTables(xcChooseCourse.getUserId(), xcChooseCourse.getCourseId());
        if(xcCourseTables!=null){
            return xcCourseTables;
        }
        xcCourseTables = new XcCourseTables();
        BeanUtils.copyProperties(xcChooseCourse,xcCourseTables);
        xcCourseTables.setChooseCourseId(xcChooseCourse.getId());//选课记录表的主键
        xcCourseTables.setCourseType(xcChooseCourse.getOrderType());//选课类型
        xcCourseTables.setUpdateDate(LocalDateTime.now());
        int insert = xcCourseTablesMapper.insert(xcCourseTables);
        if(insert<=0){
            XueChengPlusException.cast("添加课程失败");
        }
        return xcCourseTables;

    }

    //查询我的课程表
    private XcCourseTables getXcCourseTables(String userId, Long courseId) {
        LambdaQueryWrapper<XcCourseTables> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(XcCourseTables::getUserId,userId);
        queryWrapper.eq(XcCourseTables::getCourseId,courseId);
        return xcCourseTablesMapper.selectOne(queryWrapper);
    }


    //添加收费课程
    private XcChooseCourse addChargeCourse(String userId, CoursePublish coursepublish) {

        //如果选课表中有该课程记录且状态为待支付则直接返回
        LambdaQueryWrapper<XcChooseCourse> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(XcChooseCourse::getUserId,userId);
        queryWrapper.eq(XcChooseCourse::getCourseId,coursepublish.getId());
        queryWrapper.eq(XcChooseCourse::getOrderType,"700002"); //收费课程
        queryWrapper.eq(XcChooseCourse::getStatus,"701002"); //待支付
        List<XcChooseCourse> xcChooseCourses = xcChooseCourseMapper.selectList(queryWrapper);
        if (!xcChooseCourses.isEmpty()) {
            return xcChooseCourses.get(0);
        }
        //添加选课记录信息
        XcChooseCourse xcChooseCourse = new XcChooseCourse();
        xcChooseCourse.setCourseId(coursepublish.getId());
        xcChooseCourse.setCourseName(coursepublish.getName());
        xcChooseCourse.setCoursePrice(coursepublish.getPrice());
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCompanyId(coursepublish.getCompanyId());
        xcChooseCourse.setOrderType("700002");
        xcChooseCourse.setCreateDate(LocalDateTime.now());
        xcChooseCourse.setStatus("701002");//选课成功

        xcChooseCourse.setValidDays(coursepublish.getValidDays());
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(coursepublish.getValidDays()));
        int insert = xcChooseCourseMapper.insert(xcChooseCourse);
        if(insert<=0){
            XueChengPlusException.cast("添加选课记录失败");
        }

        return xcChooseCourse;
    }


    //我的课程表
    @Override
    public PageResult<XcCourseTables> mycoursetable(MyCourseTableParams params) {
        //页码
        long pageNo=params.getPage();
        //每页记录数固定为4
        long pageSize=4;
        //分页条件
        Page<XcCourseTables> page = new Page<>(pageNo, pageSize);
        //根据用户id进行查询
        String userId = params.getUserId();
        LambdaQueryWrapper<XcCourseTables> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(XcCourseTables::getUserId,userId);
        Page<XcCourseTables> pageResult = xcCourseTablesMapper.selectPage(page, queryWrapper);
        List<XcCourseTables> records = pageResult.getRecords();
        //总记录数
        long total = pageResult.getTotal();
        return new PageResult<>(records,total,pageNo, pageSize);
    }

    //
    @Override
    public boolean saveChooseCourseStauts(String choosecourseId) {
        //根据选课id查询选课表
        XcChooseCourse chooseCourse = xcChooseCourseMapper.selectById(choosecourseId);
        if(chooseCourse == null){
            log.debug("接收购买课程的消息,根据选课id从数据库找不到选课记录,选课id:{}", choosecourseId);
            return false;
        }

        //选课状态
        String status = chooseCourse.getStatus();
        //只有当未支付时才更新为已支付
        if("701002".equals(status)){
            //更新选课记录的状态为支付成功.
            chooseCourse.setStatus("701001");
            int i = xcChooseCourseMapper.updateById(chooseCourse);
            if(i <=0){
                log.debug("添加选课记录失败:{}", chooseCourse);
                XueChengPlusException.cast("添加选课记录失败");
            }
            //向我的课程表插入记录
            XcCourseTables xcCourseTables = addCourseTables(chooseCourse);

            return true;
        }


        return false;
    }


    /**
     * 更新选课状态并向我的课程表添加记录。
     * 幂等：消息重复投递或已经处理成功时，直接按成功返回。
     */
//    @Transactional
//    @Override
//    public boolean saveChooseCourseStauts(String choosecourseId) {
//        // 根据选课id查询选课表
//        XcChooseCourse chooseCourse = xcChooseCourseMapper.selectById(choosecourseId);
//        if (chooseCourse == null) {
//            log.warn("接收购买课程的消息,根据选课id从数据库找不到选课记录,选课id:{}", choosecourseId);
//            // 记录不存在属于永久性异常，交给重试/死信机制处理，不要静默成功
//            return false;
//        }
//
//        String status = chooseCourse.getStatus();
//        // 701002：收费课待支付，支付结果通知到达后更新为已支付
//        if ("701002".equals(status)) {
//            chooseCourse.setStatus("701001");
//            int i = xcChooseCourseMapper.updateById(chooseCourse);
//            if (i <= 0) {
//                log.error("更新选课记录状态失败,选课id:{}", choosecourseId);
//                XueChengPlusException.cast("更新选课记录状态失败");
//            }
//            // 向我的课程表插入记录，addCourseTables 内部会做幂等判断
//            addCourseTables(chooseCourse);
//            return true;
//        }
//
//        // 已经是已支付/选课成功：说明之前处理过，补齐课程表后按成功返回
//        if ("701001".equals(status)) {
//            addCourseTables(chooseCourse);
//            return true;
//        }
//
//        // 其他终态（如已过期、已取消等）无需再处理，按成功确认，避免无限重试
//        log.info("选课记录状态{}无需处理,选课id:{}", status, choosecourseId);
//        return true;
//    }
}
