package com.test.service.interfaces;

import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * Description
 *
 * @author zengkai
 * Date: 2022/6/26 12:15
 */
public interface KafkaConsumerService {

    void processMessage(ConsumerRecord<String, String> records);
}
