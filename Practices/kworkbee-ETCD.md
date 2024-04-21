# ETCD

![ETCD](https://etcd.io/etcd-horizontal-white.png)

관련해서 기본 동작 원리를 [kakao Tech](https://tech.kakao.com/2021/12/20/kubernetes-etcd/)에서 잘 설명하고 있으니 참고하면 좋을 것 같습니다.

Kubernetes에서는 배포되는 모든 오브젝트 및 구성 정보를 ETCD라고 하는 Backing Storage에 보관합니다. 동작 중인 클러스터에서 ETCD 데이터가 유실되면 클러스터 내 모든 컨테이너와 리소스가 Orphan 상태가 될 수 있습니다.

## 데이터 저장 구조

`key-value` Pair로 데이터가 저장됩니다. `kube-apiserver`로 요청된 Configuration을 ETCD에 Desired State로 등록한 뒤, Controller가 이 데이터를 바탕으로 Current State -> Desired State로 맞추기 위한 Reconciliation을 수행합니다.

## 설치

Managed Kubernetes Cluster 서비스를 사용한다면 Control Plane에 이미 구성이 되어 있을 것이고, 별도로 ETCD를 설치해야 한다면 [Bitnami ETCD Chart](https://github.com/bitnami/charts/tree/main/bitnami/etcd)를 사용할 수 있습니다.

## GUI Tools

딱히 아직까지 GUI Tools로 사용성이 좋아보이는 도구가 별로 없어보이지만.. 그나마 [ETCD Manager](https://etcdmanager.io/)를 사용할 수 있을 것 같습니다.

## 신뢰성 / RSM (Replicated State Machine)

![RSM](https://tech.kakao.com/storage/2021/12/01-2.png)

ETCD 그 자체가 RSM 형태로, 분산 컴퓨팅 환경에서 서버(노드)가 몇 개 다운되어도 잘 동작하는 시스템을 만들기 위해 이러한 형태를 채택하고 있습니다. 이는 동일한 데이터에 대해 여러 노드에 걸쳐 복제하는 방법입니다.

그림과 같이 Command가 들어 있는 Log 단위로 데이터를 처리하며, 순서대로 이를 처리하는 특징을 가집니다.

분산된 노드 간 데이터 복제에 있어서 여러 문제가 발생할 수 있으며, 이러한 문제 해결을 위해 Consensus 확보가 굉장히 중요합니다. 이것을 확보한다는 것은 다음 4가지 속성을 만족하는 것입니다.

- Safety : 항상 올바른 결과를 반환한다.
- Available : 노드가 일부 다운되어도 항상 응답해야 한다.
- Independent from Timing : 네트워크 지연이 발생해도 로그 일관성이 깨지면 안된다.
- Reactivity : 모든 노드에 복제되지 않아도 조건을 만족하면 빠르게 요청에 응답해야 한다.

이러한 부분들을 만족하기 위해 ETCD는 Raft 알고리즘을 사용합니다.

Raft 알고리즘의 핵심은 크게 3가지로 요약됩니다.

1. Leader Election

**기본적으로 홀수 개의 노드**가 운영되며 이들 중 Leader가 선출됩니다. 각 노드는 기본적으로 Follower라고 부르며 Leader가 될 수 있습니다. Candidate가 되어야 Leader가 될 자격이 주어지는데 Candidate가 되면 다른 노드들에게 자신을 Leader로 뽑아달라는 요청을 보내고, 요청을 받은 노드들이 응답을 보내어 과반수 이상 득표하면 Leader로 선출됩니다.

2. Log Replication

Leader는 클라이언트 요청을 받아 Log에 기록하고, 이것을 Follower 노드에 전파합니다. Follower들이 Leader로부터 Log를 받아 자신의 Log에 기록합니다.

3. Consistency

모든 노드는 동일한 Log Entries를 가져야합니다. 만약 Leader에 문제가 발생하면 다른 Candidates 중 하나가 새롭게 Leader가 됩니다.

> ### Quorum
>
> 과반수 이상 득표되어야 Leader가 되는데, 이렇게 Consensus를 확보하기 위한 최소한의 표를 Quorum이라고 하며 이 값은 floor(n / 2) + 1 로 계산할 수 있습니다.

## 장애 대응

Quorum 이상의 노드에 문제가 발생하면 결과적으로 Consensus를 달성할 수 없어 ETCD 클러스터 전체가 손상되는 Critical한 이슈에 빠집니다. 이에 대응하기 위한 작업은 아래와 같습니다.

- Backup API 사용

주기적으로 CronJob을 실행해 데이터베이스 내용을 백업합니다. 이러한 백업 Snapshot을 별도 스토리지에 저장해둡니다.

- [`Disaster Recovery`](https://github.com/bitnami/charts/blob/main/bitnami/etcd/values.yaml#L858-L930) 활성화

ETCD 노드가 재시작될 때 Snapshot으로부터 복원하는 작업이 진행됩니다.

