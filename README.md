# Mini Online Shop (microservices)

A small online shop split into independent Spring Boot services. The point of the project is resilience:
**you can stop any single service and the others keep working and report sensible errors.**

Project plan: Mon 09 Nov 2026 to Sun 06 Dec 2026 (28 days, 14 milestones, 52 hours).

## Architecture

```
Client (Postman) -> API Gateway -> catalog-service  (MongoDB)
                               -> stock-service     (MariaDB)
                               -> order-service     (MariaDB) --REST--> stock-service
                                                              --Kafka-> notification-service
Eureka (discovery) and Zipkin (tracing) support every service.
```

| Service | Responsibility | Database |
|---|---|---|
| `discovery-server` | Eureka service registry (dashboard on port 8761) | none |
| `catalog-service` | Product CRUD | MongoDB `product` |
| `stock-service` | Stock per sku: set, check, reduce | MariaDB `stock` |
| `order-service` | Place, list and read orders; checks and reduces stock in stock-service | MariaDB `ordertb` |
| `notification-service` | Logs "Order X confirmed" from a Kafka event | none |
| `api-gateway` | Single entry point, routes by service name | none |

Each service owns its own database. Services never read each other's tables; they link only by `sku`.
Diagrams (architecture, ER, high level, solution architecture) are in [`docs/`](docs/).

## Tech stack

Java 21, Maven (multi-module), Spring Boot 3.5.15, Spring Cloud 2025.0.3 (Eureka, Gateway),
Spring Data MongoDB, Spring Data JPA + MariaDB, Apache Kafka, Resilience4j, Zipkin, Docker + Jib.

Service-to-service calls use a load-balanced `RestTemplate` and the Eureka service name
(for example `http://stock-service/...`), so no port is ever hard-coded.

## Status

- [x] Parent project and architecture design (milestone 1)
- [x] catalog-service (milestone 2)
- [x] stock-service (milestone 3)
- [x] Eureka discovery server (milestone 4)
- [x] order-service (milestone 5)
- [x] Service-to-service call (milestone 6): availability check, reject with 409, reduce after save, no half-finished orders
- [ ] API gateway and first full run (milestone 7)
- [ ] Kafka and notification-service (milestone 8)
- [ ] Resilience (milestone 9)
- [ ] Zipkin tracing (milestone 10)
- [ ] Failure tests (milestone 11)
- [ ] Docker images with Jib (milestone 12)
- [ ] Documentation and v1.0 (milestone 13)

Tick each box as the milestone's "done when" is met.

## Prerequisites

- JDK 21 (check with `java -version`; `JAVA_HOME` must point to it)
- Docker Desktop (for MongoDB, MariaDB, and later Kafka and Zipkin)
- Maven is not needed; use the wrapper `mvnw.cmd` (Windows) or `./mvnw` inside a module folder

## Running locally

Start things in this order.

1. **Databases** (Docker):
   ```
   docker run -d --name mongodb -p 27017:27017 -v catalog_mongo_data:/data/db mongo:7
   cd stock-service
   docker compose up -d        # MariaDB; needs DB_PASSWORD in a .env file (copy .env.example)
   ```
   Create the `ordertb` database for order-service as well.
2. **discovery-server**, then check the dashboard at <http://localhost:8761>.
3. **catalog-service**, **stock-service** and **order-service**. All three should appear on the Eureka dashboard.
   Wait about 30 seconds after starting so every service sees the others in its registry copy.

### Passwords

Passwords are never committed. `stock-service` and `order-service` read `DB_PASSWORD` from an
environment variable (set it in the IDE run configuration of each service). `.env` is git-ignored.
Properties files must contain `${DB_PASSWORD}`, never the real value.

### Ports

Services use dynamic ports (`server.port=0`) and register by name in Eureka. The real port is in each
service's log (`Tomcat started on port ...`). To test a service directly, give it a fixed port with the
program argument `--server.port=8081`. Through the gateway the port does not matter.

## API

### catalog-service

| Call | Result |
|---|---|
| `POST /api/products` | 201 created; 400 invalid; 409 duplicate sku |
| `GET /api/products`, `GET /api/products/{id}` | 200; 404 if not found |
| `PUT /api/products/{id}`, `DELETE /api/products/{id}` | 200 / 204; 404; 409 |

### stock-service

| Call | Result |
|---|---|
| `POST /stock` | sets the quantity of a sku (201) |
| `GET /stock/{sku}` | sku and quantity; 404 if the sku is unknown |
| `GET /stock/{sku}/available?qty=5` | `true` or `false` (unknown sku is `false`) |
| `PUT /stock/{sku}/reduce?qty=3` | new stock (200); 409 if there is not enough; 404 if the sku is unknown; 400 if qty is not positive |

The reduce is one atomic SQL update (`... where quantity >= :qty`), so two orders can never take the same
last items.

### order-service

| Call | Result |
|---|---|
| `POST /orders` | 201 placed; 409 not enough stock; 400 invalid request; 503 stock-service unavailable |
| `GET /orders`, `GET /orders/{id}` | 200; 404 if not found |

Example request:

```json
{ "lines": [ { "sku": "PHN-001", "quantity": 3 } ] }
```

How an order is placed:

1. order-service asks stock-service `GET /stock/{sku}/available?qty=` for every line. Nothing is saved yet.
2. If any line is not available, the order is rejected with **409** and the database is untouched.
3. Otherwise the order is saved, then stock-service `PUT /stock/{sku}/reduce?qty=` is called for every line.
4. The whole method is `@Transactional`: if the reduce fails, the saved order is rolled back, so there are
   no half-finished orders.

All errors are JSON with `timestamp`, `status`, `error` and `message`.

## Failure behaviour (what the project is about)

| Stopped | Everything else | Expected response |
|---|---|---|
| catalog-service or its MongoDB | stock, orders keep working | Gateway/service 503 with a JSON message |
| stock-service | catalog, order reads keep working | Placing an order returns a clean 503 message |
| order-service | catalog, stock keep working | Gateway 503 with a JSON message |
| notification-service | orders still succeed | Confirmations catch up when it restarts |
| Eureka | running services keep working from their cache | No new registrations |

## Known limitations

- An order with several lines is not fully atomic across services. If line 1 is reduced and line 2 fails,
  the order is rolled back in the order database, but line 1's stock stays reduced because stock-service has
  no "give back" call. The fix would be a compensating call or a saga; it is outside the 14 milestones.
- Unit prices of order lines are `0` until order-service reads real prices from catalog-service.

## Tests

```
cd <module>
.\mvnw.cmd test
```

## Repository layout

```
.
├── pom.xml                 # parent: Spring Boot + Spring Cloud versions only
├── discovery-server/
├── catalog-service/
├── stock-service/
├── order-service/
└── docs/                   # diagrams and solution architecture (PDF)
```

## License

For learning purposes.