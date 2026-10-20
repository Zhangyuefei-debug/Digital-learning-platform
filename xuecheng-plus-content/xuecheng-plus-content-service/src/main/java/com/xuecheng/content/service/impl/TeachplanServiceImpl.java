package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuecheng.base.exception.CommonError;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.dto.TeachplanGrade;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import com.xuecheng.content.service.TeachplanMediaService;
import com.xuecheng.content.service.TeachplanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2023/2/14 12:11
 */
@Service
@Slf4j
public class TeachplanServiceImpl extends ServiceImpl<TeachplanMapper, Teachplan> implements TeachplanService {

    @Autowired
    TeachplanMapper teachplanMapper;
    @Autowired
    TeachplanMediaMapper teachplanMediaMapper;
    @Autowired
    TeachplanMediaService teachplanMediaService;

    //查询课程计划树形结构
    @Override
    public List<TeachplanDto> findTeachplanTree(Long courseId) {
        List<TeachplanDto> teachplanDtos = teachplanMapper.selectTreeNodes(courseId);
        return teachplanDtos;  //✅Teachplan 用 MyBatis 自动映射树结构✅CourseCategory 用 SQL 查出扁平结构后，由 Java 手动组装树结构 Teachplan：最多两级，所以用 Join + <collection> 直接让 MyBatis 自动拼好树CourseCategory：层级可能是 1-10 层，所以只能先查扁平 → 再 Java 拼树
    }

    private int getTeachplanCount(Long courseId,Long parentId){
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper = queryWrapper.eq(Teachplan::getCourseId, courseId).eq(Teachplan::getParentid, parentId);
        Integer count = teachplanMapper.selectCount(queryWrapper);
        return  count+1;
    }

    //新增或修改课程计划
    @Override
    public void saveTeachplan(SaveTeachplanDto saveTeachplanDto) {
        //通过课程计划id判断是新增和修改
        Long teachplanId = saveTeachplanDto.getId();
        if(teachplanId ==null){
            //新增
            Teachplan teachplan = new Teachplan();
            BeanUtils.copyProperties(saveTeachplanDto,teachplan);
            //确定排序字段，找到它的同级节点个数，排序字段就是个数加1  select count(1) from teachplan where course_id=117 and parentid=268
            Long parentid = saveTeachplanDto.getParentid();
            Long courseId = saveTeachplanDto.getCourseId();
            int teachplanCount = getTeachplanCount(courseId, parentid);
            teachplan.setOrderby(teachplanCount);
            //若添加的为一大节需要设置setIsPreview字段，因为前端并未传该字段
            teachplan.setIsPreview("0");
            teachplanMapper.insert(teachplan);

        }else{
            //修改
            Teachplan teachplan = teachplanMapper.selectById(teachplanId);
            //将参数复制到teachplan
            BeanUtils.copyProperties(saveTeachplanDto,teachplan);
            teachplanMapper.updateById(teachplan);
        }

    }


    //删除课程计划
    @Override
    public void deleteTeachplan(Long id) {
        //首先判断删除的是大章节还是小章节
        Teachplan teachplan = teachplanMapper.selectById(id);
        if(teachplan.getGrade()==1){
            //此时是大章节 想要删除还要先判断是否有小章节
            if(teachplanMapper.selectCount(new LambdaQueryWrapper<Teachplan>().eq(Teachplan::getParentid, id))>0){
                XueChengPlusException.cast("该大章节有小章节，无法删除");
            }else{
                teachplanMapper.deleteById(id);
            }
        }else {
            //此时是小章节可以直接删除同时删除对应的媒资信息
            teachplanMediaMapper.delete(new LambdaQueryWrapper<TeachplanMedia>().eq(TeachplanMedia::getTeachplanId, id));
            teachplanMapper.deleteById(id);
        }
    }



    /**
     * 课程计划移动
     * @param moveType
     * @param tepachplanId
     */
    @Override
    @Transactional
    public void moveTeachplan(String moveType, Long tepachplanId) {
        // 查询当前课程计划是否存在
        // 当前的课程计划
        Teachplan currentTeachplan = teachplanMapper.selectById(tepachplanId);
        if (currentTeachplan == null) {
            XueChengPlusException.cast(CommonError.UNKOWN_ERROR);
        }

        // 根据课程id，和节点id查询当前课程下的大章节或者小章节
        List<Teachplan> teachplanList = teachplanMapper.selectList(
                new LambdaQueryWrapper<Teachplan>()
                        .eq(Teachplan::getCourseId, currentTeachplan.getCourseId())
                        .eq(Teachplan::getParentid, currentTeachplan.getParentid())
                        .orderByAsc(Teachplan::getOrderby)
        );

        // 判断当前课程计划是否存在
        // 当前课程计划的下标
        int currentIndex = teachplanList.indexOf(currentTeachplan);
        if (currentIndex == -1) {
            XueChengPlusException.cast(CommonError.UNKOWN_ERROR);
        }
        // 创建要交换的课程计划
        Teachplan targetTeachplan;
        // 向下移动
        if ("movedown".equals(moveType)) {
            if (currentIndex == teachplanList.size() - 1) {
                return; // 已经是最后一个，不能向下移动了
            }
            targetTeachplan = teachplanList.get(currentIndex + 1);
        }else { // "moveup" 向上移动
            if (currentIndex == 0) {
                return; // 已经是第一个了，不能向上移动了
            }
            targetTeachplan = teachplanList.get(currentIndex - 1);
        }

        // 当前课程计划排序数据
        Integer tempOrder = currentTeachplan.getOrderby();
        currentTeachplan.setOrderby(targetTeachplan.getOrderby());
        targetTeachplan.setOrderby(tempOrder);

        // 更新数据库
        teachplanMapper.updateById(currentTeachplan);
        teachplanMapper.updateById(targetTeachplan);

    }

    //绑定媒资
    @Transactional
    @Override
    public void association(BindTeachplanMediaDto dto) {
        /*
         * 1.查询课程计划
         * 2.如果不是2级，报错
         * 3.查询
         * */
        Teachplan teachplan = teachplanMapper.selectById(dto.getTeachplanId());
        if(teachplan==null) XueChengPlusException.cast("未查询到相应的课程计划");
        if(!Objects.equals(teachplan.getGrade(), TeachplanGrade.GRADE2.getGrade())) XueChengPlusException.cast("非小节目录不允许绑定媒资");
        /*查询数据库 看该课程媒资信息是否存在*/
        LambdaQueryWrapper<TeachplanMedia> q1 = new LambdaQueryWrapper<>();
        q1.eq(TeachplanMedia::getTeachplanId,dto.getTeachplanId());
        TeachplanMedia one = teachplanMediaService.getOne(q1);
        if(one== null) { //空,新插入数据
            TeachplanMedia teachplanMedia = new TeachplanMedia();
            teachplanMedia.setTeachplanId(dto.getTeachplanId());
            teachplanMedia.setMediaId(dto.getMediaId());
            teachplanMedia.setMediaFilename(dto.getFileName());
            teachplanMedia.setCourseId(teachplan.getCourseId());
            teachplanMedia.setCreateDate(LocalDateTime.now());
            teachplanMediaService.save(teachplanMedia);
        }
        else { //非空更新数据
            one.setMediaId(dto.getMediaId());
            one.setMediaFilename(dto.getFileName());
            teachplanMediaService.updateById(one);
        }
    }

    //解绑媒资
    @Override
    public void deleteAssociation(Integer teachplanId, String mediaId) {
        teachplanMediaMapper.delete(new LambdaQueryWrapper<TeachplanMedia>()
                .eq(TeachplanMedia::getTeachplanId, teachplanId)
                .eq(TeachplanMedia::getMediaId, mediaId));
    }
}
