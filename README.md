Mini Online Shop (Microservices)

A small online shop built as a set of Spring Boot microservices. It is a learning project that practises the patterns used in real systems: service discovery, an API gateway, synchronous service-to-service calls with resilience, asynchronous events over Kafka, distributed tracing, and a full Docker Compose runtime.

Table of contents
1. Architecture
2. Tech stack
3. Services and ports
4. Prerequisites
5. Configuration and secrets
6. Build and run with Docker
7. Run locally from IntelliJ
8. API reference
9. Trying the order flow
10. Testing with Postman
11. How the services talk to each other
12. Resilience
13. Observability
14. Databases
15. Troubleshooting
16. Project layout
17. Known gaps and roadmap
18. Security notes


1. Architecture
                         +------------------+
   Client / Postman ---> |   api-gateway    |  :8080 (only public entry point)
                         +--------+---------+
                                  | routes by path, resolves names via Eureka
          +-----------------------+------------------------+
          |                       |                        |
 /api/products/**            /stock/**                /orders/**
          |                       |                        |
  +-------v-------+       +-------v-------+        +-------v-------+
  | catalog-service|      | stock-service |<-------| order-service |
  |   MongoDB      |      |   MariaDB     |  REST  |   MariaDB     |
  +----------------+      +---------------+ (Rest- +-------+-------+
                                             Client)       | Kafka event
                                                           v
                                                 +---------+---------+
                                                 | notification-svc  |
                                                 +-------------------+

  discovery-server (Eureka) :8761   every service registers and looks up others here
  zipkin :9411                      every service reports trace spans here

Key ideas:

One front door. Clients only talk to the gateway. The other services are not published on the host.
Discovery by name. Services register in Eureka and call each other as http://stock-service, never by IP or port.
Sync + async. Order to Stock is a synchronous REST call (we need the answer now). Order to Notification is an asynchronous Kafka event (the order should not fail because an email is slow).
Database per service. Catalog uses MongoDB. Stock and Order use separate MariaDB databases (stockdb, ordertb).
2. Tech stack
Area	Technology
Language / build	Java 21, Maven (mvnw wrapper)
Framework	Spring Boot, Spring Cloud
Gateway	Spring Cloud Gateway
Discovery	Netflix Eureka
Service calls	Spring RestClient with @LoadBalanced builder
Resilience	Resilience4j (retry, circuit breaker, fallback)
Messaging	Apache Kafka
Data	MongoDB 7, MariaDB 11, Spring Data JPA / MongoDB
Tracing	Micrometer Tracing + Zipkin
Containers	Docker, Docker Compose, Jib (image build, no Dockerfiles)

3. Services and ports

Service	Role	Port on host	Notes
api-gateway	Single entry point, routing	8080	Routes below
discovery-server	Eureka registry + dashboard	8761	http://localhost:8761
catalog-service	Products (MongoDB)	none (dynamic)	server.port=0, reached via gateway
stock-service	Stock levels (MariaDB)	none	Reached via gateway or order-service
order-service	Orders (MariaDB), calls stock, publishes events	none	Reached via gateway
notification-service	Consumes order events from Kafka	none	Logs or sends notifications
mongodb	Catalog database	27017	
mariadb	Stock and order databases	3306	
kafka	Event broker (KRaft, single node)	9094 (external)	Containers use kafka:9092
zipkin	Trace UI	9411	http://localhost:9411

Gateway routes:

Path	Goes to
/api/products/**	catalog-service
/stock/**	stock-service
/orders/**	order-service
4. Prerequisites
JDK 21
Docker Desktop (running, with at least 6 GB RAM allocated; the stack is 10 containers)
Git
Optional: IntelliJ IDEA, Postman (web or desktop), curl.exe or PowerShell

Check:

powershell
java -version
docker info

If docker info fails with a pipe error, Docker Desktop is not running yet. Start it and wait until it says "Engine running".

5. Configuration and secrets

Secrets live only in a local .env file next to docker-compose.yml. It is gitignored and must never be committed.

Copy the template:
powershell
copy .env.example .env
Edit .env and set your own values:
MARIADB_ROOT_PASSWORD=<choose-a-password>
DB_PASSWORD=<same-value-as-above>

DB_PASSWORD is what the Spring services use to log in as root, so it must equal MARIADB_ROOT_PASSWORD.

Verify it is not tracked:
powershell
git ls-files .env      # must print nothing
git check-ignore .env  # must print .env

Other configuration reaches the containers through environment variables in docker-compose.yml (datasource URLs, Kafka address, Eureka URL, Zipkin endpoint). Eureka's address uses a normal placeholder property (eureka.client.service-url.defaultZone=${EUREKA_SERVER_URL:http://localhost:8761/eureka/}) because map-style properties do not bind reliably from environment variables.

6. Build and run with Docker

Images are built by Jib, so there are no Dockerfiles. The Docker engine must be running.

First time (or after any code/properties change)
powershell
# 1. Compile and package all modules
.\mvnw clean package -DskipTests

# 2. Build the images into your local Docker
.\mvnw jib:dockerBuild

# 3. Start everything
docker compose up -d
Rebuild a single service
powershell
.\mvnw -pl order-service -am package -DskipTests
.\mvnw -pl order-service jib:dockerBuild
docker compose up -d --force-recreate order-service

Changing Java code or application.properties requires steps 1 and 2 again. Changing only docker-compose.yml or .env needs just docker compose up -d.

Daily commands
powershell
docker compose ps                       # status
docker compose logs -f order-service    # follow logs
docker compose restart stock-service    # restart one service
docker compose down                     # stop, keep data
docker compose down -v                  # stop and DELETE database volumes
Startup expectations

Cold start is slow (each Spring service can take 1 to 7 minutes on a laptop). Eureka heartbeats are 30 s, so a service can take another 30 to 60 s after it is "up" before others can resolve it. A 503 right after startup usually just means "wait a minute".

Confirm everything registered: open http://localhost:8761 and look for CATALOG-SERVICE, STOCK-SERVICE, ORDER-SERVICE, NOTIFICATION-SERVICE, API-GATEWAY.

7. Run locally from IntelliJ

For debugging you can run a service in IntelliJ while the infrastructure runs in Docker:

powershell
docker compose up -d mongodb mariadb kafka zipkin discovery-server

Then run the Spring Boot service from IntelliJ. Set DB_PASSWORD in the run configuration's environment variables (never in the properties file). Defaults point at localhost.

8. API reference

All calls go through the gateway on port 8080.

Catalog
Method	Path	Description
POST	/api/products/add	Create a product (name, price, sku)
GET	/api/products	List products
Stock
Method	Path	Description
GET	/stock/...	Check availability for a SKU and quantity
POST / PUT	/stock/...	Reduce stock (called by order-service)
Orders
Method	Path	Description
POST	/orders	Place an order
GET	/orders/{id}	Fetch an order

Exact stock/order paths and payloads follow your controllers and DTOs. Keep this section in sync with them when they change.

Status codes you will see
Code	Meaning
200 / 201	Success
400	Invalid request body
404	Unknown product or SKU
409	Not enough stock (business error, not a failure)
503	A dependency (usually stock-service) is down or slow; clean fallback response
9. Trying the order flow

Seed data in stockdb.stock: LAP-001, TSHIRT-RED-M, SKU-001 (100 each), SAMSUNG-S24-001, SAMSUNG-S25-001 (50 each).

PowerShell (Invoke-RestMethod handles JSON cleanly; curl.exe needs painful quoting):

powershell
# create a product
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/products/add `
  -ContentType 'application/json' `
  -Body '{"name":"Laptop","price":1200,"sku":"LAP-001"}'

# list products
Invoke-RestMethod http://localhost:8080/api/products

# read an error body (409 / 503)
try {
  Invoke-RestMethod -Method Post -Uri http://localhost:8080/orders `
    -ContentType 'application/json' -Body $json
} catch {
  $r = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
  $r.ReadToEnd()
}

Expected outcomes:

Enough stock: order is created with status PLACED, stock decreases, a Kafka event is published, notification-service logs it.
Quantity above stock: 409 Not enough stock, nothing changes.
Stop stock-service (docker compose stop stock-service) and order again: 503 fallback after retries, and after repeated failures the circuit opens and calls fail fast.
10. Testing with Postman

Postman runs on your machine and the stack publishes the gateway on localhost:8080, so Docker is not a problem.

Base URL: http://localhost:8080
Set header Content-Type: application/json for POST bodies.
Only the gateway (8080), Eureka (8761), Zipkin (9411) and the databases/Kafka are published. Individual services are not reachable directly.
Postman web cannot reach localhost unless the Postman Desktop Agent is installed and running. If requests fail with "could not get response", install the agent or use the desktop app.
11. How the services talk to each other

Order to Stock uses Spring RestClient built from a load-balanced builder:

java
@Bean @LoadBalanced
RestClient.Builder loadBalancedRestClientBuilder(ObservationRegistry registry) {
    var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(1));
    factory.setReadTimeout(Duration.ofSeconds(3));
    return RestClient.builder().requestFactory(factory).observationRegistry(registry);
}
java
RestClient stock = builder.baseUrl("http://stock-service").build();
stock.get().uri("/stock/{sku}", sku).retrieve().body(StockResponse.class);
http://stock-service is a Eureka name; the load balancer picks a live instance.
Timeouts are mandatory. Without them a hung dependency hangs your threads.
.observationRegistry(registry) keeps trace headers flowing. A custom builder without it breaks tracing (you get 1-span traces in Zipkin).
Errors are translated into domain exceptions: 409 to InsufficientStockException, 404 or network failure to StockServiceException.
12. Resilience

Configured with Resilience4j on StockClient:

Pattern	Where	Why
Timeout	Request factory (1 s connect, 3 s read)	Never wait forever
Retry	isAvailable only (idempotent GET)	Survive blips; never retry a write blindly
Circuit breaker	isAvailable, reduce	Stop hammering a dead service
Fallback	Throws StockServiceException, mapped to a clean 503	No stack traces to clients
Ignored by breaker	InsufficientStockException (409)	A business answer, not a fault

Known design caveat: checking stock and then reducing it is a check-then-act race. Under concurrent orders the stock can oversell. The fix is an atomic UPDATE stock SET quantity = quantity - :qty WHERE sku = :sku AND quantity >= :qty (check the updated row count) or optimistic locking with @Version.

13. Observability
Eureka dashboard: http://localhost:8761 shows registered instances.
Zipkin: http://localhost:9411 shows request traces across services. A healthy order trace shows gateway, order-service and stock-service in one trace. The http get/put/post spans to the Eureka server are heartbeat noise.
Logs: docker compose logs -f <service>.
Thread dump of a Docker service: docker kill --signal=SIGQUIT <container> then docker logs <container>.

Tracing properties (set via compose):

Spring Boot 3.x services: management.zipkin.tracing.endpoint
Spring Boot 4 services: management.tracing.export.zipkin.endpoint
Sampling: management.tracing.sampling.probability=1.0 (development only; lower it in production)
14. Databases

Open a MariaDB shell without typing the password on the command line:

powershell
"SHOW DATABASES;" | docker compose exec -T mariadb sh -c 'mariadb -uroot -p"$MARIADB_ROOT_PASSWORD"'
Database	Owner	Notes
stockdb	stock-service	table stock(id, quantity, sku UNIQUE)
ordertb	order-service	orders and order lines
catalog_db (MongoDB)	catalog-service	products: id, name, price, sku

Seed or adjust stock:

powershell
"USE stockdb; INSERT INTO stock (sku, quantity) VALUES ('LAP-001', 100) ON DUPLICATE KEY UPDATE quantity = VALUES(quantity);" |
  docker compose exec -T mariadb sh -c 'mariadb -uroot -p"$MARIADB_ROOT_PASSWORD"'

Data persists in the Docker volumes mariadb-data and mongodb-data. docker compose down -v wipes them.

15. Troubleshooting
 Symptom	Likely cause	Fix
 dockerDesktopLinuxEngine pipe not found	Docker Desktop not running	Start it; wait for docker info to succeed
 Compose: variable DB_PASSWORD is not set	Missing .env	Create .env (section 5)
 Service missing in Eureka	Still starting, or wrong Eureka URL	Wait 1 to 2 min; check docker compose logs <svc> for localhost:8761
 catalog-service registers at localhost:8761	Env var did not bind to the map property	Use the ${EUREKA_SERVER_URL:...} placeholder in its properties and rebuild the image
 503 right after start	Registry lag	Wait about 60 s and retry
 409 Not enough stock for a valid SKU	stock table empty or low	Seed stock (section 14)
 404 on /products	Wrong path	Use /api/products
 1-span traces in Zipkin	Trace propagation missing	Ensure custom RestClient.Builder sets .observationRegistry(...); gateway has SPRING_REACTOR_CONTEXT_PROPAGATION=auto
 Changes not visible after restart	Old image still used	Rebuild with jib:dockerBuild, then --force-recreate
 Jib or Docker errors after wsl --shutdown	Engine not up, bad .wslconfig	Restart Docker Desktop; inspect type "$env:USERPROFILE\.wslconfig"
 Postman "could not get response"	Web Postman cannot see localhost	Use desktop app or Desktop Agent
16. Project layout
mini-online-shop/
├── pom.xml                  parent POM (modules)
├── docker-compose.yml       full runtime (10 containers)
├── .env.example             template for secrets (copy to .env)
├── .gitignore               must contain .env
├── docker/
│   └── mariadb/init.sql     creates stockdb and ordertb on first start
├── discovery-server/        Eureka
├── api-gateway/             Spring Cloud Gateway
├── catalog-service/         products, MongoDB
├── stock-service/           stock levels, MariaDB
├── order-service/           orders, StockClient (RestClient), Kafka producer
└── notification-service/    Kafka consumer
17. Known gaps and roadmap
 Order unitPrice and total are 0: wire prices from catalog (GET /api/products/{sku} plus a CatalogClient using RestClient)
  Fix the stock check-then-act race (atomic update or @Version)
  Confirm end-to-end Zipkin traces for every hop
  Rebuild catalog-service with the Eureka placeholder property
  Remove DEBUG logging from catalog-service
  Add .env.example and push the cleaned git history
  Remove the stray stock-service/api-gateway folder
  Evidence for circuit breaker open/close behaviour (milestone 9.9)
  Tracing properties for all services (milestone 10.3)
  Authentication (JWT at the gateway), validation, pagination
  Integration tests with Testcontainers
  CI pipeline (build, test, image)
18. Security notes
Never commit .env or any password. If one is committed by mistake: git rm --cached .env, add .env to .gitignore, and amend or rewrite the commit before pushing. If it was already pushed, rotate the password.
The stack runs MariaDB as root for convenience. For anything beyond local learning, create a dedicated user per service with minimal privileges.
MongoDB and MariaDB ports are published to the host for development. Remove those ports: entries when deploying anywhere shared.
The gateway has no authentication yet. Do not expose it to the internet as is.