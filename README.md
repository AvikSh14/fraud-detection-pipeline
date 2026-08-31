# Real Time Fraud Detection Pipeline

A streaming data platform that models how payment companies catch fraudulent
transactions the moment they occur. A Kotlin service generates a realistic stream
of card transactions into Apache Kafka. An Apache Flink job reads that stream and
applies time based aggregations and anomaly rules to flag suspicious activity. A
Next.js dashboard shows the live stream, the alerts, and how detection accuracy
shifts as different attack patterns are launched.

I am building this as a learning project, but holding it to production standards:
typed configuration, input validation, tests, structured logging, protected
branches, and a proper review workflow.

> Status: active development. The transaction generator and local Kafka setup
> work today. The Flink processor and the dashboard are next.

## How it works

1. **Transaction generator (Kotlin).** A long running service that uses coroutines
   to emit a steady, configurable stream of realistic card transactions into Kafka.
   Transactions are keyed by card ID so each card's history stays in order on one
   partition. Deliberate fraud scenarios such as card testing and account takeover
   are published with a ground truth label on a separate topic, so detection
   accuracy can be measured honestly.
2. **Stream processor (Apache Flink).** Reads the transaction stream and applies
   windowed aggregations, event time watermarks for late or out of order data, and
   anomaly rules to decide which transactions look fraudulent. Flagged transactions
   are written to PostgreSQL for auditing.
3. **Dashboard (Next.js).** A live view of the transaction stream, real time fraud
   alerts, controls to launch attack simulations on demand, accuracy metrics that
   compare what was detected against what was actually fraud, and short
   explanations of how the detection logic works.

## Status

### Done

* Single node Apache Kafka cluster running in KRaft mode, so no ZooKeeper, set up
  with Docker Compose. Internal, external, and controller traffic are configured on
  separate listeners, and data survives restarts.
* A Kotlin Kafka producer that publishes JSON transactions and waits for full
  acknowledgement before moving on.
* A transaction data model and a generator that produces realistic random
  transactions, keyed by card ID so related transactions land on the same
  partition. Covered by unit tests.
* Verified the full path from producer to broker to consumer, including how Kafka
  routes messages to partitions by key.
* Repository hardening: branch rulesets, automated dependency updates, secret
  scanning that blocks leaked credentials before they are pushed, and a security
  disclosure policy.

### In progress

* Turning the generator into a real streaming service: a configurable send rate
  driven by coroutines, asynchronous sends with delivery callbacks, clean
  shutdown, layered configuration from a config file with environment variable
  overrides, and structured logging with throughput stats.
* Publishing ground truth fraud labels on a dedicated Kafka topic so accuracy can
  be evaluated later.

### Next

* Realistic per card spending behaviour and injected fraud attack patterns such as
  card testing, account takeover, and impossible travel between locations.
* A control API so attack simulations can be started and tuned live from the
  dashboard.
* The Apache Flink detection job, writing flagged transactions to PostgreSQL.
* The Next.js dashboard.

## Tech stack

| Area | Tools |
| --- | --- |
| Languages and runtime | Kotlin, Kotlin Coroutines, JVM 21 |
| Streaming and messaging | Apache Kafka in KRaft mode, Apache Flink |
| Storage | PostgreSQL |
| Frontend | Next.js, TypeScript, React, Tailwind CSS |
| Build and tooling | Gradle with the Kotlin DSL and version catalogs, Hoplite, kotlinx.serialization |
| Infrastructure | Docker, Docker Compose |
| Testing | JUnit 5, kotlin-test, Testcontainers (planned) |
| Practices | Conventional Commits, pull request workflow with protected branches, automated dependency updates, CI (planned) |

## Repository layout

```
backend/event_generator    Kotlin transaction generator (Gradle, JDK 21)
backend/flink_processor     Apache Flink stream processor (Gradle, JDK 21)
dashboard                   Next.js dashboard
infrastructure              Docker Compose for Kafka and supporting services
```

## Running locally

Requires Docker and JDK 21.

Start Kafka:

```bash
cd infrastructure
docker compose up -d
```

Create the transactions topic:

```bash
docker compose exec kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --create --topic transactions --partitions 3 --replication-factor 1
```

Run the generator and its tests:

```bash
cd backend/event_generator
./gradlew run
./gradlew test
```

Watch the transactions arrive:

```bash
cd infrastructure
docker compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 --topic transactions --from-beginning
```

Stop Kafka when you are done:

```bash
docker compose down
```

## Security

See [SECURITY.md](SECURITY.md) for how to report a vulnerability.
