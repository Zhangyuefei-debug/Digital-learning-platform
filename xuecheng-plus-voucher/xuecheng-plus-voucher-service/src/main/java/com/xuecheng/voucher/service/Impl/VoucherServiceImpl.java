package com.xuecheng.voucher.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuecheng.voucher.mapper.VoucherMapper;
import com.xuecheng.voucher.model.po.SeckillVoucher;
import com.xuecheng.voucher.model.po.Voucher;
import com.xuecheng.voucher.service.ISeckillVoucherService;
import com.xuecheng.voucher.service.IVoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper,Voucher> implements IVoucherService {


    @Autowired
    private ISeckillVoucherService seckillVoucherService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // 添加秒杀券
    @Override
    @Transactional
    public void addSeckillVoucher(Voucher voucher) {
        // 保存优惠券
        save(voucher);
        // 保存秒杀信息
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucherService.save(seckillVoucher);
        //保存秒杀券信息到redis中
        stringRedisTemplate.opsForValue().set("seckill:stock:" + voucher.getId(), voucher.getStock().toString());
    }
}
