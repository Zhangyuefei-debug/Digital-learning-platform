package com.xuecheng.content.api;

import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.service.TeachplanService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Api(value = "课程计划编辑接口",tags = "课程计划编辑接口")
@RestController
public class TeachplanController {

    @Autowired
    TeachplanService teachplanService;

   @ApiOperation("查询课程计划树形结构")
   //查询课程计划  GET /teachplan/22/tree-nodes
   @GetMapping("/teachplan/{courseId}/tree-nodes")
    public List<TeachplanDto> getTreeNodes(@PathVariable Long courseId){
       List<TeachplanDto> teachplanTree = teachplanService.findTeachplanTree(courseId);

       return teachplanTree;
   }

    @ApiOperation("课程计划创建或修改")
    @PostMapping("/teachplan")
    public void saveTeachplan( @RequestBody SaveTeachplanDto teachplan){
        teachplanService.saveTeachplan(teachplan);
    }


    @ApiOperation("课程计划删除")
    @DeleteMapping("/teachplan/{id}")
    public void deleteTeachplan(@PathVariable Long id){
        teachplanService.deleteTeachplan(id);
    }

    /**
     * 课程计划移动
     * @param moveType
     * @param tepachplanId
     */
    @ApiOperation("课程计划移动")
    @PostMapping("/teachplan/{moveType}/{tepachplanId}")
    public void moveTeachplan(@PathVariable String moveType,@PathVariable Long tepachplanId) {
        teachplanService.moveTeachplan(moveType, tepachplanId);
    }


    @ApiOperation("课程计划绑定媒资")
    @PostMapping("/teachplan/association/media")
    public void associationMedia(@RequestBody @Validated BindTeachplanMediaDto dto) throws XueChengPlusException {
        teachplanService.association(dto);
    }

    @ApiOperation("课程计划删除绑定媒资")
    @DeleteMapping("/teachplan/association/media/{teachPlanId}/{mediaId}")
    public void deleteAssociationMedia(@PathVariable("teachPlanId") Integer teachplanId,@PathVariable String mediaId) throws XueChengPlusException {
        teachplanService.deleteAssociation(teachplanId,mediaId);
    }

}
