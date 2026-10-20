package com.xuecheng.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.search.po.CourseIndex;
import com.xuecheng.search.service.IndexService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;


@Slf4j
@Service
public class IndexServiceImpl implements IndexService {


    @Autowired
    RestHighLevelClient client; //是 ES 官方提供的 高级 REST 客户端     用来通过 HTTP 调用 ES

    @Override
    public Boolean addCourseIndex(String indexName, String id, Object object) { //indexName 要存入的 Elasticsearch 索引名，相当于数据库的“表名”。 id 该课程在索引中的唯一标识（文档 ID），通常是课程的主键，如 "courseId"。 object 要存储的课程对象（通常是 CourseIndex 或 CoursePubIndex 类的实例），包含课程标题、讲师、分类、价格等信息。
        String jsonString = JSON.toJSONString(object); //把 course101 对象转成 JSON 字符串，因为 ES 存储的是 JSON 数据。
        IndexRequest indexRequest = new IndexRequest(indexName).id(id);  //表示一条索引操作请求。
        //指定索引文档内容
        indexRequest.source(jsonString, XContentType.JSON); //指定要写入的 JSON 数据。
        //索引响应对象
        IndexResponse indexResponse = null;
        try {
            indexResponse = client.index(indexRequest, RequestOptions.DEFAULT); //通过 RestHighLevelClient 发 HTTP 请求到 ES，把数据存进去。
        } catch (IOException e) {
            log.error("添加索引出错:{}", e.getMessage());
            e.printStackTrace();
            XueChengPlusException.cast("添加索引出错");
        }
        String name = indexResponse.getResult().name();  //检查 ES 返回的结果："created" → 新文档成功创建  "updated" → 文档已存在，被覆盖/更新 返回 true 或 false 给 Controller。
        System.out.println(name);
        return name.equalsIgnoreCase("created") || name.equalsIgnoreCase("updated");

    }

    @Override
    public Boolean updateCourseIndex(String indexName, String id, Object object) {

        String jsonString = JSON.toJSONString(object);
        UpdateRequest updateRequest = new UpdateRequest(indexName, id); //告诉 ES，要更新 indexName 索引中 ID 为 id 的文档。
        updateRequest.doc(jsonString, XContentType.JSON); //新的 JSON 数据，会部分更新原来的文档。
        UpdateResponse updateResponse = null;
        try {
            updateResponse = client.update(updateRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("更新索引出错:{}", e.getMessage());
            e.printStackTrace();
            XueChengPlusException.cast("更新索引出错");
        }
        DocWriteResponse.Result result = updateResponse.getResult(); //返回 true 表示更新成功。
        return result.name().equalsIgnoreCase("updated");

    }

    @Override
    public Boolean deleteCourseIndex(String indexName, String id) {

        //删除索引请求对象
        DeleteRequest deleteRequest = new DeleteRequest(indexName, id); //ES 删除请求，指定索引名和文档 ID。
        //响应对象
        DeleteResponse deleteResponse = null;
        try {
            deleteResponse = client.delete(deleteRequest, RequestOptions.DEFAULT); //发请求到 ES。
        } catch (IOException e) {
            log.error("删除索引出错:{}", e.getMessage());
            e.printStackTrace();
            XueChengPlusException.cast("删除索引出错");
        }
        //获取响应结果
        DocWriteResponse.Result result = deleteResponse.getResult();
        return result.name().equalsIgnoreCase("deleted");  //返回结果 → "deleted" 表示删除成功。
    }
}
