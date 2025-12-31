package com.example.oms.kafka;

import com.example.oms.common.Headers;
import com.example.oms.engine.OmsEngine;
import com.example.oms.proto.*;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.*;
import org.springframework.kafka.core.*;
import jakarta.annotation.PostConstruct;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Configuration
@EnableKafka
public class OmsKafka {
  private final KafkaTemplate<String, byte[]> kt;
  private final OmsEngine engine = new OmsEngine();

  public OmsKafka(KafkaTemplate<String, byte[]> kt,
                  @Value("${oms.kafka.eventsTopic}") String eventsTopic){
    this.kt = kt;
    engine.setPublisher(evt -> {
      ProducerRecord<String, byte[]> r = new ProducerRecord<>(eventsTopic, evt.getOrder().getOrderId(), evt.toByteArray());
      r.headers().add(Headers.CONTENT_TYPE, "application/x-protobuf".getBytes(StandardCharsets.UTF_8));
      r.headers().add(Headers.MESSAGE_TYPE, "oms.OrderEvent".getBytes(StandardCharsets.UTF_8));
      r.headers().add(Headers.SCHEMA_ID, "oms:v1".getBytes(StandardCharsets.UTF_8));
      kt.send(r);
    });
  }

  @KafkaListener(topics = "${oms.kafka.commandsTopic}", groupId = "oms-service")
  public void onCommand(ConsumerRecord<String, byte[]> rec){
    try{
      Order o = Order.parseFrom(rec.value());
      for (OrderEvent e : engine.onNew(o)){
        ProducerRecord<String, byte[]> r = new ProducerRecord<>(
            kt.getDefaultTopic(), o.getOrderId(), e.toByteArray());
        kt.send(r);
      }
    } catch (Exception ignore){}
  }
}
