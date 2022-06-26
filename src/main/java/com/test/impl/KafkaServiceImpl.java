package com.test.impl;

import java.util.HashMap;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.stereotype.Component;

import com.test.service.KafkaService;

/**
 * Description
 *
 * @author zengkai
 * Date: 2022/6/26 12:16
 */
@Component
public class KafkaServiceImpl implements KafkaService {

    @Override
    public void producerTest() {
        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(new HashMap<>());
        ProducerRecord<String, String> record =
                new ProducerRecord<>("topic", 0, "key", "ttltest",
                        new RecordHeaders().add(new RecordHeader("ttl", new byte[1])));
        kafkaProducer.send(record);
    }

}
