# Geo Data Server

[![Actions Status](https://github.com/gridsuite/geo-data-server/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/gridsuite/geo-data-server/actions)
[![Coverage Status](https://sonarcloud.io/api/project_badges/measure?project=org.gridsuite%3Ageo-data-server&metric=coverage)](https://sonarcloud.io/component_measures?id=org.gridsuite%3Ageo-data-server&metric=coverage)
[![MPL-2.0 License](https://img.shields.io/badge/license-MPL_2.0-blue.svg)](https://www.mozilla.org/en-US/MPL/2.0/)

## Description

The **geo-data-server** is a microservice of the [GridSuite](https://github.com/gridsuite) platform dedicated to **geographical data management for power network elements**.

It provides the following capabilities:

- **Retrieve geographical coordinates** of substations and lines for a given network, with optional filtering by country or by specific element IDs.
- **Calculate missing substation positions** using a centroid-based iterative algorithm that exploits the network topology (neighbours graph).
- **Fall back to default country-level positions** for substations with no available GPS data.
- **Store and manage** geographical data (substations and lines) in a relational database.
- **Expose coordinates** in network traversal order, including substation endpoints added at each line extremity.
- Run data retrieval **asynchronously** using a dedicated execution service.

---

## Technical Stack

- Spring Boot (Web, Data JPA, Actuator)
- PostgreSQL
- Liquibase
- API documentation: OpenAPI / Swagger (`springdoc`)
- Micrometer / Prometheus

---

## Development Scripts

Build Docker image

```shell
mvn install -DskipTests -Dpowsybl.docker.install
```

Please read [liquibase usage](https://github.com/powsybl/powsybl-parent/#liquibase-usage) for instructions to automatically generate changesets. After you generated a changeset do not forget to add it to git and in `src/resources/db/changelog/db.changelog-master.yml`.

---

## Interactions with Other Microservices

```
┌──────────────────────┐
│   geo-data-server    │──► network-store-server  (read network topology to resolve substations and lines)
└──────────────────────┘
```

---

## REST API - Data Query

Two endpoints expose geographical data for a given network: one for substations and  one for lines.

Both accept an optional list of element IDs in the request body (using `POST` to bypass URL length limits), as well as optional country filters and a variant ID.

---

## REST API - Supervision

Two endpoints allow manually importing known coordinates into the database: one for substations and one for lines.

Each entry is upserted by ID — entries absent from the request are not deleted. There is no delete endpoint.

---

## Substation Position Calculation

Missing substation positions are computed **at query time on every data query** — calculated coordinates are never persisted. Only positions explicitly imported via the [REST API - Supervision](#rest-api---supervision) are stored in the database.

When a substation has no GPS position stored in the database, the service attempts to infer it using the following strategy:

1. **Step 1** – If the substation has at least 2 neighbours with known positions, its coordinate is computed as the centroid of those neighbours.
2. **Step 2** – If only one neighbour is known, a slight offset is applied relative to that neighbour.
3. **Default fallback** – If no neighbour position is available, the substation is placed at a default country-level reference coordinate.

Positions are computed iteratively (up to a configurable number of iterations, default: 50) until no more substations can be resolved or all are placed.

---


## Asynchronous Execution

Geo-data computations run on a dedicated fixed-size thread pool (configured via `max-concurrent-requests`), freeing HTTP threads during potentially long iterative calculations. Excess requests are queued rather than spawning unbounded threads. 

---

## Micrometer Observability

The service exposes two Micrometer gauges on the execution thread pool


---

