package com.xuecheng.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;

import java.util.List;

/**
 * @author Mr.M
 * @version 1.0
 * @description 课程计划管理相关接口
 * @date 2023/2/14 12:10
 */
public interface TeachplanService extends IService<Teachplan> {
 /**
  * 根据课程id查询课程计划
  * @param courseId 课程计划
  * @return
  */
  public List<TeachplanDto> findTeachplanTree(Long courseId);

 /**
  * 新增/修改/保存课程计划
  * @param saveTeachplanDto
  */
 public void saveTeachplan(SaveTeachplanDto saveTeachplanDto);


    //删除课程计划
    void deleteTeachplan(Long id);


    /**
     * 课程计划移动
     * @param moveType
     * @param tepachplanId
     */
    void moveTeachplan(String moveType, Long tepachplanId);


    //课程计划关联媒资
    void association(BindTeachplanMediaDto dto);


    //课程计划删除关联媒资
    void deleteAssociation(Integer teachplanId, String mediaId);
}
