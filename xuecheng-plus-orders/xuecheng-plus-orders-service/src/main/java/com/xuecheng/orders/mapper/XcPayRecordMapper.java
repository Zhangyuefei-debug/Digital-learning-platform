package com.xuecheng.orders.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuecheng.orders.model.po.XcPayRecord;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author itcast
 */
public interface XcPayRecordMapper extends BaseMapper<XcPayRecord> {
    int updatePayRecordStatus(
            @Param("payNo") String payNo,
            @Param("outPayNo") String outPayNo,
            @Param("payChannel") String payChannel,
            @Param("paySuccessTime") LocalDateTime paySuccessTime
    );
}
