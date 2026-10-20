package com.xuecheng.voucher.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuecheng.voucher.model.dto.Result;
import com.xuecheng.voucher.model.po.SeckillVoucher;
import com.xuecheng.voucher.model.po.VoucherOrder;

public interface IVoucherOrderService extends IService<VoucherOrder> {


    //抢购优惠券
    Result seckillVoucher(Long voucherId);
}
