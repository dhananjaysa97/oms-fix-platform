package com.example.fix;

import com.example.oms.common.Headers;
import com.example.oms.proto.*;
import org.apache.kafka.clients.producer.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.UUID;

@Component
public class FixTcpServer {

  private final KafkaProducer<String, byte[]> producer;
  private final String topic;

  public FixTcpServer(@Value("${oms.kafka.bootstrapServers}") String bs,
                      @Value("${oms.kafka.commandsTopic}") String topic){
    this.topic = topic;
    Properties p = new Properties();
    p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bs);
    p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
    p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");
    this.producer = new KafkaProducer<>(p);
  }

  @PostConstruct
  public void start() throws Exception {
    ServerSocket ss = new ServerSocket(9876);
    Thread t = new Thread(() -> {
      while (true){
        try (Socket s = ss.accept()){
          BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
          String line;
          while ((line = br.readLine()) != null){
            Order o = parse(line);
            ProducerRecord<String, byte[]> r = new ProducerRecord<>(topic, o.getOrderId(), o.toByteArray());
            r.headers().add(Headers.CONTENT_TYPE, "application/x-protobuf".getBytes(StandardCharsets.UTF_8));
            r.headers().add(Headers.MESSAGE_TYPE, "oms.Order".getBytes(StandardCharsets.UTF_8));
            r.headers().add(Headers.SCHEMA_ID, "oms:v1".getBytes(StandardCharsets.UTF_8));
            producer.send(r);
          }
        } catch (Exception ignore){}
      }
    }, "fix-tcp");
    t.setDaemon(true);
    t.start();
  }

  private static Order parse(String line){
    // dev-friendly: key=value|key=value
    String oid = UUID.randomUUID().toString();
    String sym = "AAPL"; long qty=100; Side side=Side.BUY; OrdType ot=OrdType.MARKET; double px=0;
    for (String p : line.split("\\|")){
      String[] kv = p.split("=");
      if (kv.length!=2) continue;
      switch (kv[0]){
        case "11" -> oid = kv[1];
        case "55" -> sym = kv[1];
        case "38" -> qty = Long.parseLong(kv[1]);
        case "54" -> side = "1".equals(kv[1])?Side.BUY:Side.SELL;
        case "40" -> ot = "2".equals(kv[1])?OrdType.LIMIT:OrdType.MARKET;
        case "44" -> px = Double.parseDouble(kv[1]);
      }
    }
    return Order.newBuilder()
        .setOrderId(oid)
        .setSymbol(sym)
        .setQty(qty)
        .setSide(side)
        .setOrdType(ot)
        .setLimitPrice(px)
        .setTsEpochMs(System.currentTimeMillis())
        .build();
  }
}
