# MandiFlow

A farm-to-home produce marketplace where every order travels through Kafka.
Five Spring Boot services, no direct calls between them: the order service drops an event into Kafka and every other service reacts on its own.

**Stack:** Java 21 · Spring Boot 3.5 · Spring for Apache Kafka · Kafka Streams · Hibernate/JPA · Liquibase · MySQL 8.4 · Redis 7.4 · Docker Compose · Vite (plain HTML/JS)

```
                 Web (Vite :5173)
                      │ REST + SSE
        ┌─────────────┴─────────────┐
   order-service :8081        inventory-service :8082
        │                           │
        └──────────► Apache Kafka ◄─┴──── payment-service :8083
                     (6 topics + 6 DLTs, key = orderId)
                        │        │
     notification-service :8084  analytics-service :8085 (Kafka Streams)

   MySQL: one database per service      Redis: cache · rate limit · dedupe · relay lock
```

## The saga

| Topic | Published by | Read by | Means |
|---|---|---|---|
| `orders.created` | order | inventory, analytics, notification (logger) | A customer placed an order |
| `inventory.reserved` | inventory | payment | Stock is held |
| `inventory.rejected` | inventory | order | Out of stock, unknown product or price changed |
| `payments.completed` | payment | order | Money collected |
| `payments.failed` | payment | order, inventory | Declined; inventory releases the stock |
| `orders.status-changed` | order | notification, order (SSE) | Order moved to CONFIRMED / REJECTED / CANCELLED |

Every topic has 3 partitions and a `.DLT` twin. Every message is keyed by order ID, so all events for one order stay in order on one partition.

## Run it

**You need:** JDK 21, Maven 3.9+, Docker Desktop, Node 20+.

```bash
# 1. Infrastructure: MySQL, Kafka (KRaft), Kafka UI, Redis, Mailpit
docker compose up -d

# 2. Build everything (add -DskipTests to skip the Testcontainers tests)
mvn verify

# 3. Start the services: either run each *Application class in IntelliJ
#    (start order-service first, it creates the topics), or:
scripts/run-services.sh          # logs go to ./logs, stop with scripts/stop-services.sh

# 4. The web app
cd web && npm install && npm run dev     # http://localhost:5173
```

| What | Where |
|---|---|
| Shop | http://localhost:5173 |
| Swagger (orders / products / payments) | :8081, :8082, :8083 `/swagger-ui.html` |
| Kafka UI (topics, lag, DLTs) | http://localhost:8090 |
| Mailpit inbox | http://localhost:8025 |
| Redis | `docker compose exec redis redis-cli MONITOR` |

Seeded produce: 1 Tomatoes ₹40/kg · 2 Kinnow ₹60/dozen · 3 Sarson saag ₹30/bunch (35 in stock) · 4 Basmati rice ₹120/kg · 5 Onions ₹35/kg · 6 Fresh coriander ₹10/bunch.

## Try these

| Scenario | Do this | You should see |
|---|---|---|
| Happy path | 2 kg tomatoes + 1 bunch saag (₹110) | Tracking page turns CONFIRMED by itself; email in Mailpit |
| Out of stock | 50 bunches of saag | REJECTED: "short by 15 bunch"; payment never hears about it |
| Payment declined | 25 kg basmati (₹3,000) | CANCELLED; the 25 kg goes back to stock (check Admin) |
| Price changed | Add tomatoes to cart, change the price on Admin, then place the order | REJECTED: PRICE_CHANGED |
| Resume from offset | Stop notification-service, place 3 orders, start it | It logs all three with partition and offset |
| Rebalancing | Run a second inventory-service with `--server.port=8092` | Partitions split 2/1 in the logs; stop one, the other takes all 3 |
| Retries → DLT | `docker compose stop mailpit`, place an order | 3 retries (1 s, 2 s, 4 s), then a record in `orders.status-changed.DLT` |
| Replay a DLT | `docker compose start mailpit`, re-send the DLT message to the main topic in Kafka UI | Exactly one email (Redis `SET NX` dedupe) |
| Outbox | `docker compose stop kafka`, place an order, `docker compose start kafka` | Order still completes; its outbox row waited in MySQL |
| Rate limit | Place 6 orders within a minute | 6th returns HTTP 429 |
| Cache | `GET :8082/api/products/1` twice | Second call skips SQL; key `products::1` in Redis |
| Consumer lag | Add `Thread.sleep(5000)` in `StatusEmailListener`, place orders | Lag grows in Kafka UI |

## Where each phase of the guide lives

| Phase | Code |
|---|---|
| 0 Infrastructure | `docker-compose.yml`, `docker/mysql-init.sql`, parent `pom.xml` |
| 1 JPA + MySQL | `order-service/.../domain/Order.java`, `api/OrderController.java`, `db/changelog` |
| 2 First event | `common-events/.../Topics.java`, `OrderCreatedEvent.java`, `config/KafkaTopicConfig.java`, `notification-service/.../OrderCreatedLogger.java` |
| 3 Inventory + partitions | `inventory-service/.../ReservationService.java`, `Product.java` (`@Version`) |
| 4 Saga | `payment-service/.../PaymentService.java`, `order-service/.../OrderSagaListener.java`, `inventory-service/.../PaymentFailedListener.java` |
| 5 Retries, DLT, duplicates | `config/KafkaErrorConfig.java` (every service), `notification-service/.../IdempotencyGuard.java`, UNIQUE `payments.order_id` |
| 6 Transactional outbox | `domain/OutboxEvent.java`, `messaging/OutboxRelay.java` (order, inventory, payment), `OutboxRelayLock.java` |
| 7 Redis cache + rate limit | `inventory-service/.../CatalogService.java`, `CacheConfig.java`, `order-service/.../OrderRateLimiter.java` |
| 8 Kafka Streams | `analytics-service/.../TopProductsTopology.java`, `api/TopProductsController.java` (interactive query) |
| 9 Web + live tracking | `web/`, `order-service/.../SseHub.java`, `config/WebConfig.java` |
| 10 Tests | `OrderSagaIT`, `ReservationIT`, `PaymentIT` (Testcontainers + Awaitility), plus unit tests and an in-memory Kafka Streams test |

The code is the finished state of every phase: no service calls `kafkaTemplate.send` inside a business transaction, it writes an outbox row instead.

## Choices that go a little beyond the guide

- **Price check without a direct call.** The browser sends the price it showed; inventory compares it with today's price and rejects with `PRICE_CHANGED` if the admin changed it. Order-service never calls inventory-service.
- **Rejected reservations are stored too** (status `REJECTED`), so a redelivered `orders.created` is recognised and never reserved late.
- **Reasons travel with the saga.** `InventoryRejectedEvent.detail` becomes `orders.status_reason`, then the email text and the tracking page message.
- **Undeserializable records still reach the DLT.** The dead-letter recoverer has a raw-bytes template next to the JSON one.
- **Hibernate mapping safety for `ddl-auto: validate`:** UUIDs map to `CHAR(36)` (`preferred_uuid_jdbc_type`), enums to `VARCHAR` (not MySQL `ENUM`), `Instant` to `TIMESTAMP`.
- **SSE listener uses `auto.offset.reset=latest`**, so a fresh order-service copy does not replay old status changes to browsers.

## Interview questions this project answers

Why key by order ID? What if a consumer crashes before committing? How does the outbox fix dual writes? Choreography or orchestration? How do you undo a step across services? Same group vs different groups? How many partitions? Which errors go straight to the DLT? Does Kafka exactly-once cover MySQL? Why not cache stock? What is consumer lag? See the Build Guide's interview section; every answer points at code above.

## Resume line

Built MandiFlow, an event-driven produce marketplace with 5 Spring Boot microservices communicating over Apache Kafka (choreographed saga, transactional outbox, retries with dead-letter topics, idempotent consumers), per-service MySQL schemas with JPA and Liquibase, Redis caching and rate limiting, and live order tracking with Server-Sent Events.

## Troubleshooting

- **Liquibase/Hibernate "Schema-validation" error:** a table drifted from its changeset. With a fresh volume: `docker compose down -v && docker compose up -d`.
- **Kafka UI shows no cluster:** wait ~20 s after `docker compose up`; the broker needs a moment in KRaft mode.
- **Tests fail to start:** Testcontainers needs Docker running.
