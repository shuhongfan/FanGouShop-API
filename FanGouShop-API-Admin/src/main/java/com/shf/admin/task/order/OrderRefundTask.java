package com.shf.admin.task.order;

import com.shf.common.utils.DateUtil;
import com.shf.service.service.OrderTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 账单退款操作
 */
@Component
@Configuration //读取配置
@EnableScheduling // 2.开启定时任务
public class OrderRefundTask {
    //日志
    private static final Logger logger = LoggerFactory.getLogger(OrderRefundTask.class);

    @Autowired
    private OrderTaskService orderTaskService;

    @Scheduled(fixedDelay = 1000 * 60L) //1分钟同步一次数据
    public void init(){
        logger.info("---OrderRefundTask task------produce Data with fixed rate task: Execution Time - {}", DateUtil.nowDateTime());
        try {
            orderTaskService.refundApply();

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("OrderRefundTask.task" + " | msg : " + e.getMessage());
        }

    }
}
