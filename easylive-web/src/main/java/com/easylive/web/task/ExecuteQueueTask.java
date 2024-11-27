package com.easylive.web.task;

import com.easylive.component.RabbitMQComponent;

import com.easylive.entity.constants.Constants;
import com.easylive.entity.po.VideoInfoFilePost;
import com.easylive.service.VideoInfoPostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Component
@Slf4j
public class ExecuteQueueTask {

    private ExecutorService executorService = Executors.newFixedThreadPool(Constants.LENGTH_2);

    @Resource
    private RabbitMQComponent rabbitMQComponent;

    @Resource
    private VideoInfoPostService videoInfoPostService;

    @PostConstruct
    public void conSumTransferFileQueue() {
        executorService.execute(() -> {
            while (true) {
                try {
                    // 从 RabbitMQ 获取转码文件信息
                    List<VideoInfoFilePost> videoInfoFilePosts = rabbitMQComponent.getFileFromTransferQueue();
                    if (videoInfoFilePosts == null || videoInfoFilePosts.isEmpty()) {
                        Thread.sleep(1500);  // 如果队列为空，休眠 1.5 秒
                        continue;
                    }
                    // 处理获取到的文件
                    for (VideoInfoFilePost videoInfoFilePost : videoInfoFilePosts) {
                        videoInfoPostService.transferVideoFile(videoInfoFilePost);
                    }
                } catch (Exception e) {
                    log.error("获取转码文件队列信息失败", e);
                }
            }
        });
    }
}

