package com.xuecheng.voucher.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuecheng.voucher.model.po.Voucher;
import org.apache.ibatis.annotations.Mapper;

import javax.annotation.ManagedBean;

@Mapper
public interface VoucherMapper extends BaseMapper<Voucher> {
}
