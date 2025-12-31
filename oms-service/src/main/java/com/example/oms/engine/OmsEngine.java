package com.example.oms.engine;

import com.example.oms.proto.*;
import java.util.*;
import java.util.concurrent.*;

public class OmsEngine {
  private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
  private final Map<String, Long> remaining = new ConcurrentHashMap<>();
  private volatile java.util.function.Consumer<OrderEvent> publisher = e -> {};

  public void setPublisher(java.util.function.Consumer<OrderEvent> p){ this.publisher = p; }

  public List<OrderEvent> onNew(Order o){
    List<OrderEvent> out = new ArrayList<>();
    if (o.getQty() <= 0){
      out.add(reject(o, "Invalid qty"));
      return out;
    }
    remaining.put(o.getOrderId(), o.getQty());
    out.add(event(EventType.ACK, o, OrderStatus.ACKED, 0, 0, "ACK"));

    long half = o.getQty()/2;
    long rest = o.getQty()-half;

    exec.schedule(() -> {
      if (!remaining.containsKey(o.getOrderId())) return;
      remaining.put(o.getOrderId(), rest);
      publisher.accept(event(EventType.TRADE, o, OrderStatus.PARTIALLY_FILLED, half, px(o), "PARTIAL"));
    }, 200, TimeUnit.MILLISECONDS);

    exec.schedule(() -> {
      if (!remaining.containsKey(o.getOrderId())) return;
      remaining.remove(o.getOrderId());
      publisher.accept(event(EventType.TRADE, o, OrderStatus.FILLED, rest, px(o), "FILLED"));
    }, 600, TimeUnit.MILLISECONDS);

    return out;
  }

  public OrderEvent onCancel(Order o){
    if (remaining.remove(o.getOrderId()) != null){
      return event(EventType.CANCEL, o, OrderStatus.CANCELED, 0, 0, "CANCELED");
    }
    return reject(o, "Too late to cancel");
  }

  private double px(Order o){
    return o.getOrdType()==OrdType.MARKET ? 100.0 : o.getLimitPrice();
  }

  private OrderEvent reject(Order o, String msg){
    return event(EventType.REJECT, o, OrderStatus.REJECTED, 0, 0, msg);
  }

  private OrderEvent event(EventType t, Order o, OrderStatus s, long fq, double px, String txt){
    return OrderEvent.newBuilder()
        .setType(t)
        .setOrder(o)
        .setStatus(s)
        .setFilledQty(fq)
        .setLastPx(px)
        .setText(txt)
        .setTsEpochMs(System.currentTimeMillis())
        .build();
  }
}
