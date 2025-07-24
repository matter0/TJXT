package com.tianji.learning.mq;

import com.tianji.api.dto.trade.OrderBasicDTO;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.CollUtils;
import com.tianji.learning.service.ILearningLessonService;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LessonChangeListener {

    private final ILearningLessonService learningLessonService;
    /**
     *   监听订单支付和课程报名的信息
     * @param
     * @return
    */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "learing.lesson.pay.queue",durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.ORDER_EXCHANGE,type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.ORDER_PAY_KEY
    ))
    public void listenLessonPay(OrderBasicDTO order){
        //健壮性处理
        if (order==null||order.getUserId()==null|| CollUtils.isEmpty(order.getCourseIds())){
            //数据有误，无需处理
            log.error("接收掉的mq消息有误，订单数据为空");
            return;
        }
        //添加课程
        log.debug("监听到用户{}的订单{}，需要加入课程{}到课表中",order.getUserId(),order.getOrderId(),order.getCourseIds());
        learningLessonService.addUserLessons(order.getUserId(),order.getCourseIds());
    }

    /**
     *   监听退款之后取消用户课程
     * @param
     * @return
    */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "learning.lesson.cancel.queue",durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.ORDER_EXCHANGE,type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.ORDER_REFUND_KEY
    ))
    public void listenCancelLesson(OrderBasicDTO order){
        //健壮性处理
        if (order==null||order.getUserId()==null|| CollUtils.isEmpty(order.getCourseIds())){
            //数据有误，无需处理
            log.error("接收掉的mq消息有误，订单数据为空");
            return;
        }
        //删除课程
        log.debug("监听到用户{}退款，删除课程{}",order.getUserId(),order.getCourseIds());
        learningLessonService.deleteLessons(order.getUserId(),order.getCourseIds());
    }
}
