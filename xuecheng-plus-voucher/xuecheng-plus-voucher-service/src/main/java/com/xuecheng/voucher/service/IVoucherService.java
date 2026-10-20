package com.xuecheng.voucher.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuecheng.voucher.model.po.Voucher;

public interface IVoucherService extends IService<Voucher> {
    //添加优惠券
    void addSeckillVoucher(Voucher voucher);
}
