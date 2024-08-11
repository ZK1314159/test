package com.test.service.impl;

import com.test.service.interfaces.KafkaProducerService;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;

/**
 * Description
 *
 * @author zengkai
 * Date: 2022/6/26 12:16
 */
@Component
public class KafkaServiceImpl implements KafkaProducerService {

//    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void producerTest() {
        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(new HashMap<>());
        ProducerRecord<String, String> record =
                new ProducerRecord<>("topic", 0, "key", "ttltest",
                        new RecordHeaders().add(new RecordHeader("ttl", new byte[1])));
        kafkaProducer.send(record);
    }

    @Override
    public void send() {
        kafkaTemplate.send("test2", "first message");
    }

}
