package com.xuecheng.voucher.api;

import com.xuecheng.voucher.model.dto.Result;
import com.xuecheng.voucher.service.IVoucherOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api("优惠券抢购接口")
public class VoucherOrderController {


    @Autowired
    private IVoucherOrderService voucherOrderService;



    @PostMapping("seckill/{id}")
    @ApiOperation("抢购优惠券")
    public Result seckillVoucher(@PathVariable("id") Long voucherId){
        return voucherOrderService.seckillVoucher(voucherId);
    }
}
