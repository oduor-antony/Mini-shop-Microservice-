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
| `order-service` | Place, list and read orders; calls stock-service | MariaDB `orders` |
| `notification-service` | Logs "Order X confirmed" from a Kafka event | none |
| `api-gateway` | Single entry point, routes by service name | none |

Each service owns its own database. Services never read each other's tables; they link only by `sku`.
Diagrams (architecture, ER, high level, solution architecture) are in [`docs/`](docs/).

## Tech stack

Java 21, Maven (multi-module), Spring Boot 3.5.15, Spring Cloud 2025.0.3 (Eureka, Gateway),
Spring Data MongoDB, Spring Data JPA + MariaDB, Apache Kafka, Resilience4j, Zipkin, Docker + Jib.

## Status

- [x] Parent project and architecture design (milestone 1)
- [ ] catalog-service (milestone 2)
- [ ] stock-service (milestone 3)
- [ ] Eureka discovery server (milestone 4)
- [ ] order-service (milestone 5)
- [ ] Service-to-service call (milestone 6)
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
2. **discovery-server**, then check the dashboard at <http://localhost:8761>.
3. **catalog-service** and **stock-service**. Both should appear on the Eureka dashboard.

Passwords are never committed. `stock-service` reads `DB_PASSWORD` from an environment variable
(set it in your IDE run configuration). `.env` is git-ignored.

Services use dynamic ports and register by name in Eureka. To test a service directly, give it a
fixed port with the program argument `--server.port=8081`. Through the gateway the port does not matter.

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
| `GET /stock/{sku}/available?qty=5` | `true` or `false` (unknown sku is `false`) |
| `PUT /stock/{sku}/reduce?qty=3` | new stock; 409 if there is not enough; 404 if the sku is unknown |

### order-service (planned)

| Call | Result |
|---|---|
| `POST /api/orders` | 201 placed; 409 no stock; 503 stock service unavailable (fallback) |
| `GET /api/orders`, `GET /api/orders/{id}` | 200; 404 if not found |

All errors are JSON with `status`, `message` and `path`.

## Failure behaviour (what the project is about)

| Stopped | Everything else | Expected response |
|---|---|---|
| catalog-service or its MongoDB | stock, orders keep working | Gateway/service 503 with a JSON message |
| stock-service | catalog, order reads keep working | Placing an order returns a clean 503 fallback message |
| order-service | catalog, stock keep working | Gateway 503 with a JSON message |
| notification-service | orders still succeed | Confirmations catch up when it restarts |
| Eureka | running services keep working from their cache | No new registrations |

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
└── docs/                   # diagrams and solution architecture (PDF)
```

## License

For learning purposes.