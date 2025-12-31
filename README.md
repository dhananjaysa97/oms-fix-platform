# OMS + FIX Platform

## Run OMS
```bash
./gradlew :oms-service:bootRun
```

## Run FIX gateway
```bash
./gradlew :fix-gateway:bootRun
```

## Send test FIX order
```bash
nc localhost 9876
11=cl1|55=AAPL|54=1|40=2|38=100|44=189.12
```

Events are published to Kafka topic `oms.events`.
