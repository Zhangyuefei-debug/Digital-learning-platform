package com.xuecheng.voucher.service.Impl;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuecheng.base.utils.RedisIdWorker;
import com.xuecheng.voucher.mapper.VoucherOrderMapper;
import com.xuecheng.voucher.model.dto.Result;
import com.xuecheng.voucher.model.po.SeckillVoucher;
import com.xuecheng.voucher.model.po.VoucherOrder;
import com.xuecheng.voucher.service.ISeckillVoucherService;
import com.xuecheng.voucher.service.IVoucherOrderService;
import com.xuecheng.voucher.service.IVoucherService;
import com.xuecheng.voucher.util.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;

import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;



@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Autowired
    RedisIdWorker redisIdWorker;
    @Autowired
    private IVoucherService voucherService;
    @Autowired
    private ISeckillVoucherService seckillVoucherService;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private VoucherOrderServiceImpl voucherOrderService;


    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }



    // 秒杀优惠券
//    @Override  //使用synchronized实现单机条件下的一人一单和使用类似于乐观锁的防止库存超卖
//    public Result seckillVoucher(Long voucherId) {
//        //根据优惠券id查询优惠券信息
//        Voucher voucher = voucherService.getById(voucherId);
//        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
//        if (voucher== null){
//            return Result.fail("优惠券不存在");
//        }
//        if (seckillVoucher.getStock() < 1){
//            return Result.fail("优惠券已售完");
//        }
//        if (voucher.getStatus() != 1){
//            return Result.fail("优惠券已下架或过期");
//        }
//        if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())){
//            return Result.fail("优惠券未开始");
//        }
//        if (seckillVoucher.getEndTime().isBefore(LocalDateTime.now())){
//            return Result.fail("优惠券已结束");
//        }
//        Long userId = UserHolder.getUser().getId();
//        synchronized(userId.toString().intern()) {  //intern() 的作用String.intern() 会将字符串放入 JVM 的字符串常量池中，保证相同字符串的引用地址相同，能让 synchronized 生效（否则 new 出来的对象锁不住同一个人）。
//            //获取代理对象
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return proxy.createVoucherOrder(voucherId);
//        }
//    }


//    @Override  //使用redisson 实现分布式锁 可以实现集群模式下的秒杀优惠券
//    public Result seckillVoucher(Long voucherId) {
//        //根据优惠券id查询优惠券信息
//        Voucher voucher = voucherService.getById(voucherId);
//        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
//        if (voucher== null){
//            return Result.fail("优惠券不存在");
//        }
//        if (seckillVoucher.getStock() < 1){
//            return Result.fail("优惠券已售完");
//        }
//        if (voucher.getStatus() != 1){
//            return Result.fail("优惠券已下架或过期");
//        }
//        if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())){
//            return Result.fail("优惠券未开始");
//        }
//        if (seckillVoucher.getEndTime().isBefore(LocalDateTime.now())){
//            return Result.fail("优惠券已结束");
//        }
//        Long userId = UserHolder.getUser().getId();
//        RLock lock = redissonClient.getLock(RedisConstants.LOCK_ORDER_KEY + userId);
//        boolean isLock = lock.tryLock();  //tryLock() 没有参数时，Redisson 会默认给锁一个 30 秒的初始过期时间（默认值）。在锁持有期间，看门狗会 自动续期，每隔 10 秒将锁续期一次，确保业务逻辑执行完锁不会过期。当业务执行完成，unlock() 会释放锁。
//                                          //非阻塞式：如果锁可用，立即获得锁并返回 true。如果锁不可用，立即返回 false（不等待）
//                                          //lock.lock() 阻塞式：如果锁已经被其他线程持有，它会一直阻塞等待，直到锁被释放。
//        if (!isLock) {
//            return Result.fail("请勿重复下单");
//        }
//        //我懂了 我之前项目的逻辑是使用redis缓存已发布的课程信息，为了防止缓存击穿使用了redisson中的lock.lock()方法，不能用tryLock方法吧，否则直接返回false用户不就傻眼了, 只能阻塞然后获取锁之后再从缓存中取数据。
//        // 而我先现在项目是使用分布式锁实现一人一单功能，使用用户id作为key，使用lock.tryLock方法，集群模式下获取锁失败的话证明该用户已经获取了该锁去下单了，直接返回false即可
//        try{
//            //获取代理对象
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return proxy.createVoucherOrder(voucherId);
//        }finally {
//            lock.unlock();
//        }
//    }




    @Override  //上述已经安全实现了一人一单和防库存超卖的问题，但是效率不高，我们可以将判断用户是否有购买资格的判断逻辑放到lua脚本中，这样效率更高，只需要返回给客户端一个结果成功或者没有购买资格或者不能重复购买，然后使用消息队列完成数据库扣减库存和订单创建
    public Result seckillVoucher(Long voucherId) {
        //获取用户
        Long userId = Long.valueOf(SecurityUtil.getUser().getId());
        long orderId = redisIdWorker.nextId("order"); //全局唯一订单 ID（时间戳 + 序列号的 ID 生成器）。它在真正写库之前就生成，这样 Lua 脚本可以把 orderId 带进消息队列，异步线程最终用同一个 ID 落库。
        // 1.执行lua脚本
        Long result = stringRedisTemplate.execute( //把“库存检查”“重复购买检查”“库存扣减”和“发送消息到 Stream”全放到 Lua 脚本里，减少网络往返和并发竞争
                SECKILL_SCRIPT, //lua脚本
                Collections.emptyList(), //KEY列表
                voucherId.toString(), //ARGV[1]
                userId.toString(), //ARGV[2]
                String.valueOf(orderId) //ARGV[3]
        );
        int r = result.intValue();
        // 2.判断结果是否为0
        if (r != 0) {
            // 2.1.不为0 ，代表没有购买资格
            return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
        }
        // 4.返回订单id
        return Result.ok(orderId);
    }

    //异步处理线程池
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    //// 线程池大小（建议=CPU核心数 * 2）
    //private static final int CONSUMER_COUNT = 4;
    //private static final ExecutorService SECKILL_ORDER_EXECUTOR =
    //        Executors.newFixedThreadPool(CONSUMER_COUNT);
    //“线程数 ≠ 消费者数，
    //但如果你每个线程绑定一个唯一 consumerName，
    //那么：线程数 ≈ 消费者数（成立）”

    //在类初始化之后执行，因为当这个类初始化好了之后，随时都是有可能要执行的
    @PostConstruct
    private void init() {
        // 1. 尝试创建消费者组
        try {                                                 //Stream的key类似于RabbitMQ的队列名   ”g1"消费者组名
            stringRedisTemplate.opsForStream().createGroup("stream.orders", "g1");
        } catch (Exception e) {
            // 如果已经存在就忽略
            log.info("消费者组已存在，无需创建");
        }
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());

//        // 启动多个消费者线程
//            for (int i = 0; i < CONSUMER_COUNT; i++) {
//                String consumerName = "c" + i;
//                SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler(consumerName));
//            }
    }

    private volatile boolean running = true;


    @PreDestroy
    public void destroy() throws Exception {
        // 通知线程退出并尝试优雅关闭线程池
        running = false;
        SECKILL_ORDER_EXECUTOR.shutdownNow();
        try {
            if (!SECKILL_ORDER_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("seckill-order-handler 未能在限定时间内关闭");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("VoucherOrderServiceImpl 已触发优雅停止");
    }


    // 用于线程池处理的任务
    // 当初始化完毕后，就会去从对列中去拿信息
    private class VoucherOrderHandler implements Runnable {
        String queueName = "stream.orders";  //要消费的Stream名称

        @Override
        public void run() {
            while(running && !Thread.currentThread().isInterrupted()) {
                try { //获取消息队列中的订单信息 XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS stream.orders >
                    // 1.获取消息队列中的订单信息
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"), // 组名"g1"，消费者名"c1"
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),  //每次最多取1条消息（避免一次处理太多） 没有消息时阻塞2秒，而不是立即返回
                            StreamOffset.create(queueName, ReadOffset.lastConsumed()) //从上次消费到的位置继续读（>符号）
                    );

                    // 2.判断消息是否获取成功
                    if(list==null||list.isEmpty()){
                        //2.1如果获取失败 则继续下一次获取
                        continue;
                    }
                    //3.解析消息中的订单信息
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> values = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(values, new VoucherOrder(), true); //消息是 {userId, voucherId, id} 这种 Map，fillBeanWithMap 把它填充成 VoucherOrder 对象
                    //4.如果成功获取可以下单
                    if(handleVoucherOrder(voucherOrder)){
                        //ack确认 告诉Redis这条消息已处理完，可以从Pending List移除了
                        stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", record.getId());
                    }else{
                        log.warn("订单{}未能获取用户锁，不ACK，立即从Pending List补偿", record.getId());
                        handlePendingList();
                    }
                } catch (Exception e) {
                    log.error("处理订单异常", e);
                    try {
                        handlePendingList();
                    } catch (Exception ex) {
                        log.error("处理Pending-list过程抛错", ex);
                    }
                    // 若 Redis 连接关闭或线程中断，短暂 sleep 并检查 running
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            log.info("秒杀订单消费线程结束");
        }

//    private class VoucherOrderHandler implements Runnable {
//        private final String consumerName;
//        private final String queueName = "stream.orders";
//
//        public VoucherOrderHandler(String consumerName) {
//            this.consumerName = consumerName;
//        }
//
//        @Override
//        public void run() {
//            while (running && !Thread.currentThread().isInterrupted()) {
//                try {
//                    // 1.读取消息，这里必须用当前线程自己的 consumerName
//                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
//                            Consumer.from("g1", consumerName),
//                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
//                            StreamOffset.create(queueName, ReadOffset.lastConsumed())
//                    );
//
//                    // 2.判断消息是否获取成功
//                    if (list == null || list.isEmpty()) {
//                        continue;
//                    }
//
//                    // 3.解析消息中的订单信息
//                    MapRecord<String, Object, Object> record = list.get(0);
//                    Map<Object, Object> values = record.getValue();
//                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(values, new VoucherOrder(), true);
//
//                    // 4.处理成功才ACK；锁获取失败则不确认，交给Pending List补偿
//                    if (handleVoucherOrder(voucherOrder)) {
//                        stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", record.getId());
//                    } else {
//                        log.warn("订单{}未能获取用户锁，不ACK，交给Pending List补偿", record.getId());
//                        handlePendingList();
//                    }
//                } catch (Exception e) {
//                    log.error("处理订单异常", e);
//                    try {
//                        handlePendingList();
//                    } catch (Exception ex) {
//                        log.error("处理Pending-list过程抛错", ex);
//                    }
//                    try {
//                        Thread.sleep(50);
//                    } catch (InterruptedException interruptedException) {
//                        Thread.currentThread().interrupt();
//                    }
//                }
//            }
//            log.info("秒杀订单消费线程 {} 结束", consumerName);
//        }

        private void handlePendingList() {
            while (running && !Thread.currentThread().isInterrupted()) {
                try { //获取Pending-list中的订单信息 XREADGROUP GROUP g1 c1 COUNT 1 STREAMS stream.orders 0 从 PEL 的头部开始读
                    // 1.获取消息队列中的订单信息
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            //Consumer.from("g1", consumerName),
                            StreamReadOptions.empty().count(1),
                            StreamOffset.create(queueName, ReadOffset.from("0")) //读取Pending List中的消息（已投递但未ACK的消息）
                    );

                    // 2.判断消息是否获取成功
                    if(list==null ||list.isEmpty()){
                        //2.1如果获取失败 说明Pending-list没有异常消息 结束循环
                        break;
                    }
                    //3.解析消息中的订单信息
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> values = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(values, new VoucherOrder(), true);
                    if (handleVoucherOrder(voucherOrder)) {
                        //5. ack确认
                        stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", record.getId());
                    } else {
                        log.warn("订单{}锁获取失败，保留在Pending List，稍后重试", record.getId());
                        Thread.sleep(100);
                    }
                } catch (Exception e) {
                    log.error("处理Pending-list异常");
                    // 在出现 Redis 连接问题或其他异常时，退出 Pending 处理，等待主循环重新尝试
                    break;
                }
            }
        }
    }

    //// 正常流程：
    //读取消息 → 处理 → ACK → 消息删除
    //
    //// 异常流程：
    //读取消息 → 处理 → 服务宕机 → 没发ACK
    //// 重启后，消息还在Pending List，可以重新处理

    private boolean handleVoucherOrder(VoucherOrder voucherOrder) {
        //1.获取用户
        Long userId = voucherOrder.getUserId();
        // 2.创建锁对象
        RLock redisLock = redissonClient.getLock("lock:order:" + userId);
        // 3.尝试获取锁
        boolean isLock = redisLock.tryLock();
        // 4.判断是否获得锁成功
        if (!isLock) {
            // 获取锁失败，直接返回失败或者重试
            log.error("不允许重复下单！");
            return false;
        }
        try {
            //注意：由于是spring的事务是放在threadLocal中，此时的是多线程，事务会失效
            //3. 获取代理对象
            voucherOrderService.createVoucherOrderLua(voucherOrder);  //AopContext.currentProxy() 只能在同线程的 Spring AOP 调用链中使用 多线程中调用事务方法 → 必须走 Spring 注入的代理对象
            return true;
            //"Spring事务是基于动态代理和ThreadLocal实现的。当通过代理对象调用方法时，代理对象会从ThreadLocal中获取当前线程的事务信息，开启事务，执行目标方法，然后提交或回滚事务。
            //但在这个秒杀场景中，订单处理是在独立的线程池线程中执行的。如果我直接使用this.createVoucherOrderLua()调用，就跳过了代理对象，相当于直接调用原始对象的普通方法，Spring事务拦截器根本不会执行，导致事务失效。
            //所以这里必须通过Spring注入的voucherOrderService代理对象来调用，让代理对象在新线程中重新开启事务，保证数据库操作的原子性。"
        } finally {
            // 释放锁
            redisLock.unlock();
        }
    }
    //第一，Lua脚本校验的是Redis中的状态，而最终数据是在MySQL中落地的。从消息进入Stream到异步线程处理完成之间存在时间窗口，如果同一个用户的多个请求在这个时间窗口内都通过了Lua校验，异步处理时就会并发操作数据库，导致重复下单。
    //
    //第二，虽然数据库层面有唯一索引可以兜底，但靠数据库抛异常回滚的成本太高。用分布式锁可以在业务层面就拦截掉重复请求，避免无效的数据库操作，提升系统效率。
    //
    //这个锁的粒度是用户级别的，既能保证同一个用户的请求串行处理，又不会影响不同用户的并发性能，是性能和一致性的一个平衡点。"



    @Transactional  //这个是因为使用Lua脚本时 订单id以及订单的必要信息会事先生成到队列中 直接传入订单信息然后保存即可。
    public Result createVoucherOrderLua(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        Long voucherId = voucherOrder.getVoucherId();
        Long orderId = voucherOrder.getId();

        // 一人一单
        LambdaQueryWrapper<VoucherOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VoucherOrder::getUserId, userId);
        queryWrapper.eq(VoucherOrder::getVoucherId, voucherId);
        VoucherOrder exist = this.getOne(queryWrapper);
        if (exist != null) {
            log.info("用户已购买，无需重复下单");
            return Result.fail("该用户已经购买过");
        }

        // 扣库存
        LambdaUpdateWrapper<SeckillVoucher> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SeckillVoucher::getVoucherId, voucherId);
        updateWrapper.gt(SeckillVoucher::getStock, 0);
        updateWrapper.setSql("stock = stock - 1");
        boolean success = seckillVoucherService.update(null, updateWrapper);
        if (!success) {
            log.info("库存不足");
            return Result.fail("库存不足");
        }
        //UPDATE seckill_voucher
        //SET stock = stock - 1
        //WHERE voucher_id = ?
        //  AND stock > 0;
        //扣减库存时，我通过 MyBatis-Plus 构造条件更新 SQL，要求优惠券 ID 匹配且库存大于 0，再执行 stock = stock - 1。
        // 库存判断和扣减在同一条 SQL 中完成，借助数据库的并发控制，避免多个并发请求在库存不足时仍然扣减成功。
        // 如果更新影响行数为 0，就说明没有符合条件的记录，当前请求无法扣减库存，从而防止数据库库存被扣成负数。

        // 保存订单
        this.save(voucherOrder);
        log.info("订单创建成功，订单ID：" + orderId);
        return Result.ok(orderId);
    }
}

//解决 RejectedExecutionException 和优雅停机
//你新增的逻辑非常到位：
//@PreDestroy 钩子 (destroy()): 完美利用 Spring 容器关闭时的回调。
//volatile boolean running: 作为一个线程间的信号量，用于安全地通知循环停止。
//SECKILL_ORDER_EXECUTOR.shutdownNow() 和 awaitTermination(): 这是一个标准的 优雅关闭线程池 的模式。它先尝试立即关闭，然后等待一个宽限期（5秒），确保应用在关闭时不会留下悬空的线程任务，这正是解决 RejectedExecutionException (线程池已关闭但又尝试提交任务) 的关键。
//while(running && !Thread.currentThread().isInterrupted()): 在 run() 方法中增加了 running 标志和中断检查，确保线程能够响应退出信号并退出无限循环。
//2. 增强了异常处理
//handlePendingList() 的调用: 你在主循环的 catch 块中调用了 handlePendingList() 并增加了新的 try-catch 块，这让异常处理的逻辑更健壮，并能处理 Pending-list 过程中可能出现的异常。
//Thread.sleep(50): 在主循环发生连接异常时，短暂休眠一下是合理的，可以避免在 Redis 连接彻底断开时 CPU 飙升。
//Pending-list 异常处理中的 break: 在 handlePendingList 中，一旦处理 Pending-list 出现异常（如连接问题），你使用 break 退出内层循环。这个处理是合理的，因为连接问题是全局的，应该让外层循环的 sleep 和重试机制来处理。
//总结
//你的修改是 非常专业和有效的。它解决了服务重启或关闭时可能导致的数据丢失、资源泄露和日志警告问题，特别是优雅地处理了后台消费者线程。



//悲观锁实现防止库存超卖
//@Transactional
//public Result seckillVoucherPessimistic(Long voucherId) {
//    // 1. 查询优惠券秒杀信息并加悲观锁
//    SeckillVoucher seckillVoucher = seckillVoucherService.getBaseMapper()
//            .selectOneForUpdate(voucherId); // 需要自己写 Mapper 方法
//
//    if (seckillVoucher.getStock() < 1) {
//        log.info("库存不足");
//        return Result.fail("库存不足");
//    }
//
//    // 2. 扣库存
//    seckillVoucher.setStock(seckillVoucher.getStock() - 1);
//    seckillVoucherService.updateById(seckillVoucher);
//
//    log.info("扣库存成功, voucherId={}", voucherId);
//    return Result.ok();
//}

//@Mapper
//public interface SeckillVoucherMapper extends BaseMapper<SeckillVoucher> {
//
//    @Select("SELECT * FROM seckill_voucher WHERE voucher_id = #{voucherId} FOR UPDATE")
//    SeckillVoucher selectOneForUpdate(@Param("voucherId") Long voucherId);
//
//}
//selectOneForUpdate 执行的是 SELECT ... FOR UPDATE，InnoDB 会对命中的那行记录加排他锁，其他事务的 SELECT FOR UPDATE、UPDATE、DELETE 都会阻塞在这里等待。
//这个锁的持有范围是整个事务，不是单条 SQL。所以 updateById 执行完、方法体走完，锁都还在。
//直到方法正常返回，@Transactional 提交事务（或者抛异常回滚），锁才释放。
//如果没有 @Transactional，MySQL 默认 autocommit，SELECT FOR UPDATE 语句一结束锁就释放，那就完全失去悲观锁的意义，两个事务可能同时读到库存 1，都更新成 0，造成超卖。