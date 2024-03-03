# SchemaHero

![SchemaHero](../assets/35.png)

[Project Main](https://schemahero.io)

## Overview

SchemaHero는 Kubernetes 환경에서 Database 스키마를 관리하기 위한 오픈소스 프로젝트로, 스키마를 정의하고, 버전을 관리하고, 배포하는 기능을 제공한다. 현재 CNCF Sandbox 프로젝트로 진행 중이다.

> SchemaHero is an open-source database schema migration tool that converts a schema definition into migration scripts that can be applied in any environment. Written as both a CLI utility and a Kubernetes Operator, SchemaHero eliminates the task of creating and managing sequenced migration scripts that are compatible with all environments that an application is running in.

### 도구가 없다면?

애플리케이션에서 데이터베이스 스키마를 변경할 때, 스키마 변경에 대한 SQL 스크립트를 작성하고, 이를 수동으로 실행하는 방식으로 진행한다. 이러한 방식은 다음과 같은 문제점을 가지고 있다.

- 스키마 변경에 대한 SQL 스크립트를 작성하는 것은 복잡하다.
- 스키마 변경에 대한 SQL 스크립트를 작성하고 수동으로 실행하는 것은 실수할 가능성이 높다.
- 스키마 변경에 대한 SQL 스크립트를 수동으로 실행하는 것은 반복적인 작업이다.
- 스키마 변경에 대한 SQL 스크립트를 수동으로 실행하는 것은 추적이 어렵다.

### 어떻게 동작하나?

Operator로 배포되어 동작하는 SchemaHero Operator는 아래 후술할 Custom Resource인 `Database`와 `Table`을 참조한다.

- `Database`는 데이터베이스 연결 정보를 정의한다.
- `Table`은 테이블 스키마를 정의한다.

배포된 `Database`별로 실제 데이터베이스에 연결하여 `Table`에 명시한 스키마를 Desired State로 유지하기 위해 계속해서 Reconcile을 진행하며, 변동사항이 발견되면 이에 대한 Migration을 생성하고 적용한다. 이 때 Configuration에 따라 곧바로 스키마를 변경시킬 수 있지만, Default로는 비활성화되어 있어 `.status` 필드에서 DDL을 검토하여 Approve 혹은 Reject할 수 있다.

### 어떤 데이터베이스를 지원하나?

- MySQL
- PostgreSQL
- CockroachDB
- SQLite
- Cassandra

## 주요 리소스

### Database

```yaml
apiVersion: databases.schemahero.io/v1alpha4
kind: Database
metadata:
  name: mydatabase
  namespace: default
spec:
  connection:
    postgresql:
    uri: postgres://user:password@host:port/dbname
```

Go 기반으로 작성된 드라이버가 사용되기 때문에 DB Connection String Syntax가 JDBC Driver를 사용할 때와는 다름을 유의해야 한다.

### Table

```yaml
apiVersion: schemas.schemahero.io/v1alpha4
kind: Table
metadata:
  name: schedule
  namespace: schemahero-tutorial
spec:
  database: airlinedb
  name: schedule
  schema:
    postgres:
      primaryKey: [flight_num]
      columns:
        - name: flight_num
          type: int
        - name: origin
          type: char(4)
          constraints:
            notNull: true
        - name: destination
          type: char(4)
          constraints:
            notNull: true
        - name: departure_time
          type: time
          constraints:
            notNull: true
        - name: arrival_time
          type: time
          constraints:
            notNull: true
```

### Migration

Migration의 경우 Operator가 생성하는 저수준 Custom Resource이므로 사용자가 별도로 건드릴 필요가 없다.

```
ID       DATABASE   TABLE     PLANNED  EXECUTED  APPROVED  REJECTED
a9626a8  airlinedb  schedule  9m30s    7m58s     8m0s
eaa36ef  airlinedb  airport   4h       4h        4h
fa32022  airlinedb  schedule  5s
```

```yaml
apiVersion: databases.schemahero.io/v1alpha4
kind: Migration
```

```sh
kubectl schemahero -n schemahero-tutorial describe migration fa32022
```

```
Migration Name: fa32022

Generated DDL Statement (generated at 2020-06-06T14:56:04-07:00):
  alter table "schedule" alter column "departure_time" type time, alter column "departure_time" drop not null;
  alter table "schedule" alter column "arrival_time" type time, alter column "arrival_time" drop not null;
  alter table "schedule" add column "duration" integer;


To apply this migration:
  kubectl schemahero -n schemahero-tutorial approve migration fa32022

To recalculate this migration against the current schema:
  kubectl schemahero -n schemahero-tutorial recalculate migration fa32022

To deny and cancel this migration:
  kubectl schemahero -n schemahero-tutorial  reject migration fa32022
```

## Krew Plugin

스키마 변동사항이 발생하면 Migration을 생성하고 적용하는 것은 Operator가 담당하지만, 이를 위한 CLI 도구로 `schemahero`가 제공된다. 이를 Krew를 통해 설치하면 `kubectl schemahero` 명령어로 사용할 수 있다.

```bash
kubectl krew install schemahero
```

승인하려면 아래 명령어를 실행한다.

```bash
kubectl schemahero approve migration mydatabase mytable
```

거절하려면 아래 명령어를 실행한다.

```bash
kubectl schemahero reject migration mydatabase mytable
```

## 한계

- Community Maintaining이 생각보다 활발하지 않다.
  - 현재 Unstable 버전이며 지원되지 않는 일부 기능이 있다.
    - 데이터베이스 타입별로 버전별 호환되는 CRD 버전이 다르며, 컬럼 타입도 현재 호환되지 않는 케이스가 일부 존재한다.
  - Auditing / Logging이 미제공 상태이다.

## 이미 애플리케이션과 데이터베이스가 연결되어 있을 때 SchemaHero로 마이그레이션하려면?

DB에 연결하여 현재 스키마를 추출하고, 이를 기반으로 `Table` CRD를 생성하여 배포하면 된다. 이후 Operator가 Reconcile을 통해 현재 스키마와 Desired State를 비교하여 `Migration`을 생성하고 적용한다.

```sh
kubectl schemahero generate \
    --driver postgres \
    --uri postgres://user:pass@host:5432/dbname \
    --dbname desired-schemahero-databasename \
    --output-dir ./imported
```

## Custom Type을 사용할 수 있나?

현재는 Cassandra에 한해 제한적으로 Custom Type을 생성할 수 있다.

```yaml
apiVersion: schemas.schemahero.io/v1alpha4
type: DataType
metadata:
  name: basic-info
spec:
  database: schemahero
  name: basic_info
  schema:
    cassandra:
        fields:
          - name: birthday
            type: timestamp
          - name: nationality
            type: text
          - name: weight
            type: text
          - name: height
            type: text
```

위와 같은 CR 생성의 결과로 아래와 같은 쿼리가 생성된다.

```sql
CREATE TYPE cycling.basic_info (
  birthday timestamp,
  nationality text,
  weight text,
  height text
);
```

## Credentials를 어떻게 관리하나?

CRD에서 정의한 필드를 통해서 Secret을 참조하여 연결정보를 관리할 수 있다. 물론 Vault도 사용할 수 있다.

```yaml
apiVersion: databases.schemahero.io/v1alpha4
kind: Database
metadata:
  name: rds-postgres
  namespace: default
spec:
  connection:
    postgres:
      uri:
        valueFrom:
          secretKeyRef:
            key: uri
            name: rds-postgres
```

```yaml
apiVersion: databases.schemahero.io/v1alpha4
kind: Database
metadata:
  name: my-db
  namespace: namespace
spec:
  connection:
    postgres:
      uri:
        valueFrom:
          vault:
            endpoint: http://<vault-endpoint>:8200
            connectionTemplate: postgres://{{ .username }}:{{ .password }}@postgres:5432/my-db
            serviceAccount: schemahero-vault
            serviceAccountNamespace: schemahero-vault
            secret: my-db
            role: schemahero
            agentInject: false
            kubernetesAuthEndpoint: /v1/auth/kubernetes_custom/login
```

## Managed DB (Cloud Provider가 제공하는) 을 사용할 수 있나?

가능하다. 별도 요구사항 없이 Connection String만 명시하면 된다.