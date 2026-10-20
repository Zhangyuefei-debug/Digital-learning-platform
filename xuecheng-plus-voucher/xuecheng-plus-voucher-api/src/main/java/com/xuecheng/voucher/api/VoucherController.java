package com.xuecheng.voucher.api;

import com.xuecheng.voucher.model.dto.Result;
import com.xuecheng.voucher.model.po.Voucher;
import com.xuecheng.voucher.service.IVoucherService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api("发布优惠券接口")
public class VoucherController {

    @Autowired
    private IVoucherService voucherService;



    //新增优惠券
    @ApiOperation("新增优惠券")
    @PostMapping("seckill")
    public Result addSeckillVoucher(@RequestBody Voucher voucher) {
        voucherService.addSeckillVoucher(voucher);
        return Result.ok(voucher.getId());
    }
}
