package com.test.impl;

import com.test.service.KafkaConsumerService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;

/**
 * Description
 *
 * @author zengkai
 * Date: 2022/7/23 10:38
 */
@Slf4j
@Component
public class KafkaConsumerServiceImpl implements KafkaConsumerService {

//    @KafkaListener(topics = "test2")
    @Override
    public void processMessage(ConsumerRecord<String, String> records) {
        log.info("consumer-topic:" + records.topic());
        log.info("consumer-partition:" + records.partition());
        log.info("consumer-offset:" + records.offset());
        log.info("consumer-value:" + records.value());
    }

}
