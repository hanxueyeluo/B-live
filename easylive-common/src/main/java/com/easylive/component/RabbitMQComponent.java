package com.easylive.component;


import com.easylive.entity.constants.Constants;
import com.easylive.entity.po.VideoInfoFilePost;
import com.easylive.rabbitmq.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;


import javax.annotation.Resource;
import java.util.List;

@Component
public class RabbitMQComponent {

    @Resource
    private RabbitTemplate rabbitTemplate;

    // 发送消息到 RabbitMQ 队列
    public void addFile2TransferQueue(List<VideoInfoFilePost> addFileList) {
        // 使用 RabbitTemplate 将消息发送到队列
        rabbitTemplate.convertAndSend(Constants.EXCHANGE_NAME, Constants.REDIS_KEY_QUEUE_TRANSFER , addFileList);
    }

    // 从 RabbitMQ 队列获取消息（消费者端会自动处理）
    public List<VideoInfoFilePost> getFileFromTransferQueue() {
        // 这里返回 null 或空表示队列没有消息
        return (List<VideoInfoFilePost>) rabbitTemplate.receiveAndConvert(Constants.REDIS_KEY_QUEUE_TRANSFER, Constants.REDIS_KEY_EXPIRES_ONE_DAY);
    }

    // 发送删除文件路径消息到 RabbitMQ 队列
    public void addFile2DelQueue(String videoId, List<String> delFilePathList) {
        // 发送消息到 RabbitMQ 队列
        rabbitTemplate.convertAndSend(Constants.EXCHANGE_NAME, Constants.REDIS_KEY_FILE_DEL+videoId , delFilePathList);
    }

}

