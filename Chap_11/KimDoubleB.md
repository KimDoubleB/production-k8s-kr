# 11장 - 플랫폼 서비스 구축

플랫폼 서비스 - 애플리케이션 플랫폼에 기능을 추가하기 위해 설치되는 컴포넌트

플랫폼 서비스를 구축하는 개념의 핵심은 사용자의 수고를 없애려는 노력이다.
여기에는 자동화 이상의 것이다. **자동화가 핵심이지만, 자동화 된 컴포넌트의 통합이 가장 중요하다.**

<br/>


## 확장점

앞서 여러 장에서 살펴봤듯 쿠버네티스는 여러 확장 가능한 포인트들을 제공한다.
모든 요구사항을 하나의 구현체가 만족할 수 없기도 하고, 
쿠버네티스는 그 자체로도 복잡한 분산 소프트웨어 이기 때문에 모든 요구사항을 충족시킬 수 없다.

이러한 확장은 세 가지의 확장으로 구분해볼 수 있다.
1. 플러그인 확장
2. 웹훅 확장
3. 오퍼레이터 확장


<br/>

### 플러그인 확장

워크로드를 실행하는데 중요하고 필수적인 인접 시스템과 쿠버네티스를 통합하는 데 도움이 되는 광범위한 확장 클래스
- 네트워크: CNI
- 스토리지: CSI
- 컨테이너 런타임: CRI
- 디바이스: 쿠버네티스 디바이스 플러그인 프레임워크. 워크로드가 기본 노드 디바이스에 엑세스 할 수 있음 (ex. GPU 이용)


<br/>

### 웹훅 확장

쿠버네티스 API 서버의 핵심 API 기능을 변환/커스텀하기 위한 확장으로 외부 백엔드를 호출해 적용한다.

- 인증 확장: OIDC 같은 인증확장을 통해 인증요청 작업을 오프로드 한다.
- 어드미션 컨트롤러: 작업을 수행하려고 API 서버에 요청이 전달되면, 어드미션 웹훅 구성에 따라 어드미션 웹훅을 호출해 유효성 검사 및 변경 작업을 수행한다.


<br/>

### 오퍼레이터 확장

웹훅 확장과 달리 오퍼레이터 확장은 API 서버의 클라이언트 입장의 확장이다.
오퍼레이터 소프트웨어는 실제 운영 엔지니어와 마찬가지로 쿠버네티스 API의 클라이언트로 상호작용한다.

오퍼레이터 사용의 주된 목적은 **작업자의 수고를 덜어주고, 작업자를 대신해 작업을 자동으로 수행하는 것**이다.

<br/>

## 오퍼레이터 패턴

오퍼레이터 패턴은 쿠버네티스를 쿠버네티스로 확장하는 것
- 새로운 쿠버네티스 리소스(CRD/CR)를 생성하고, 정의된 상태를 조정하는 컨트롤러를 개발해 운영하는 것
- 2가지 핵심 메커니즘: 컨트롤러, 사용자 정의 리소스(CRD)


### 쿠버네티스 컨트롤러

컨트롤러는 리소스 타입을 관찰하고 원하는 상태를 충적해 리소스 생성, 변형, 삭제 수행
- 사용자는 리소스 매니페스트를 제출해 원하는 시스템 상태(spec)를 선언. 컨트롤러는 기존 상태(status)가 선언된 상태가 되도록 일치하는 작업을 수행.
- Watch 메커니즘은 모든 쿠버네티스 컨트롤러 기능의 핵심으로, 쿠버네티스 API 서버에서 리소스 변경에 응답해야 하는 컨트롤러에 노출되는 etcd 기능
- 컨트롤러도 동일한 작업을 수행해 다른 컨트롤러에서 작업을 트리거할 수 있다.

고민 해볼 거리
- 원하는 상태로 무한 루프. 실패한다면? 재시도 -> 지수 백오프 적용
- 프로세스 간 Deadlock이 발생한다면? -> 자가 치유/Self healing, FST (Finite State Machine)
- 고가용성을 위해 replica로 구성? -> 요청에 따라 하나의 컨트롤러만 동작할 수 있도록 리더 선출 기능 (Raft/Quorum)

사실 뭔가 새로운 매커니즘이라기 보단 쿠버네티스의 기본 매커니즘.
- `spec`: 원하는 상태(`desired state`)를 의미
- `status`: 현재 상태(`current state`)를 의미
- 쿠버네티스 시스템을 통해 `spec`, `status` 가 다르다면, `spec` 에 맞게 변경해나가게 된다.


### 사용자 정의 리소스

CRD (Custom Resource Definition)
- 리소스의 원하는 상태를 제공 (spec)
- 기존 상태에 대한 중요한 정보를 기록 (status)

CRD는 필드를 정의하려고 Open API v3 스키마 명세를 이용
- 필드를 선택/필수/기본값 설정하고, 이를 통해 유효성 검사를 할 수 있다.


<br/>

## 오퍼레이터 개발

Kubernetes Client 개발 키트(SDK)를 이용하면 오퍼레이터를 구현할 수 있다.
하지만 숙련도가 필요해, 이를 이용하기 어렵다면 아래의 프레임워크 들을 통해 틀을 만들고 개발할 수 있다.
- kube-builder
- metacontroller
- Operator framework

Kubernetes 생태계에서는 go언어가 거의 주로 활용되므로, go언어를 활용해 구현하는 것이 제일 좋은 방향 같다.

<br/>

## Reconciliation
현재 상태(status)를 원하는 상태(spec)와 일치하도록 수행하는 프로세스

트리거는 언제 되는가?
- 컨트롤러 시작 또는 재시작
- 리소스 생성
- 컨트롤러 자체 변경/리소스 변경
- 리소스 삭제
- 시스템의 정확한 view를 보장하려고 API와 주기적으로 재동기화를 수행

<br/>

---

# OwnerReferences / Finalizer

쿠버네티스에서 일부 리소스들은 다른 특정 리소스로 하여금 만들어져 운영되는 경우가 있습니다. 

- 다시 말해서, 특정 리소스가 일부 리소스의 `owner` 일 수 있고, 이러한 `owner` 리소스에 대해 일부 리소스가 `dependents` 리소스 일 수 있습니다. 즉, **리소스 간에 종속적인 관계**가 존재할 수 있는 것입니다.
- 예를 들면, `ReplicaSet` 은 여러 `Pod` 들의 `owner` 라고 말할 수 있을 겁니다. 또한, 이 `Pod` 들은 `ReplicaSet` 에 대해 `dependents` 리소스라고 할 수 있겠죠.

## Owner references

이러한 종속관계는 `dependent` 리소스의 `metadata.ownerReferences` 필드를 통해 나타낼 수 있습니다.

[Kubernetes API Reference Docs](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.22/#ownerreference-v1-meta)

- `metadata.ownerReferences` 는 `apiVersion`, `kind`, `name`, `uid` 필드를 통해 `owner` 리소스를 명시할 수 있습니다.
- 추가적으로 `controller`, `blockOwnerDeletion` 필드를 통해 쿠버네티스 특정 동작을 설정할 수 있습니다.

기존 종속적인 관계가 있는 리소스(ReplicaSets, DaemonSets, Deployments, Jobs and CronJobs, and ReplicationControllers 등)에서는 자동으로 이러한 `ownerReferences` 가 지정되어 동작합니다.

한번 테스트 해볼까요?

```yaml
apiVersion: apps/v1
kind: ReplicaSet
metadata:
  name: my-repset
spec:
  replicas: 3
  selector:
    matchLabels:
      pod-info: sample
  template:
    metadata:
      labels:
        pod-info: sample
    spec:
      containers:
      - name: nginx
        image: nginx
```

위 `ReplicaSet` 을 배포해봅시다.

```bash
$ kga
NAME                  READY   STATUS    RESTARTS   AGE
pod/my-repset-29t76   1/1     Running   0          45s
pod/my-repset-nbbhh   1/1     Running   0          45s
pod/my-repset-s86vj   1/1     Running   0          45s

NAME                        DESIRED   CURRENT   READY   AGE
replicaset.apps/my-repset   3         3         3       45s
```

3개의 `Pod` 가 만들어졌습니다.

이 중 한 개의 `Pod` metadata를 살펴봅시다.

```bash
$ k edit pod/my-repset-29t76

...

apiVersion: v1
kind: Pod
metadata:

	...

  **ownerReferences:
  - apiVersion: apps/v1
    blockOwnerDeletion: true
    controller: true
    kind: ReplicaSet
    name: my-repset
    uid: 766396cd-d067-481a-8003-81aafcf80670**

...
```

`ownerReferences` 필드가 설정되어져 있는 것을 볼 수 있습니다.

딱 봐도 `Pod` 을 생성한 `ReplicaSet` 을 가르키는 것 같네요. 

`ReplicaSet` uid 를 확인해보면 확실해지겠죠?

```bash
$ kubectl get rs my-repset -o yaml | grep uid | cut -d ":" -f 2
**766396cd-d067-481a-8003-81aafcf80670**
```

uid도 같은 것을 보아 owner - dependent 관계가 자동으로 설정되어져있음을 볼 수 있습니다.

당연히 수동으로 직접 이러한 `owner` - `dependent` 관계를 만들기 위해 `ownerReferences` 를 설정할 수 있습니다.

한번 해볼까요?

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: pod-owner
spec:
  containers:
  - name: container
    image: kubetm/init
```

위 Pod를 생성해 uid를 확인해봅시다.

```bash
$ k edit pod pod-owner

apiVersion: v1
kind: Pod
metadata:

  ...

  name: pod-owner
  namespace: default
  **uid: 2ab40d57-1eed-488e-8271-0721ead830c1**

	...
```

이를 이용해 `ownerReferences` 필드에 사용해봅시다.

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: pod-child
  ownerReferences:
    - apiVersion: v1
      kind: Pod
      name: pod-owner
      uid: 2ab40d57-1eed-488e-8271-0721ead830c1
spec:
  containers:
  - name: container
    image: kubetm/init
```

위 Pod을 생성해봅시다. 정상적으로 만들어지는 것을 볼 수 있습니다.

- `uid` 가 제대로 설정되지 않는다면 해당 Pod는 만들어지지 않습니다. 바로 `Terminating` 상태가 됩니다.

그래서, `ownerReferences` 를 통해 연결해주면 어떤 동작이 달라질까요?

- `delete` 동작에 종속관계가 사용되어 제거됩니다. 좀 추상적으로 이야기했지만, owner-dependents 관계에 따라 kubernetes garbage selector에 의해 함께 제거될 수 있습니다.

## Deletion

`controller`, `blockOwnerDeletion` 필드 설정 없이 연결만 해주었다고 합시다.

- `dependent` 리소스를 제거하는 경우
    - `owner` 리소스에 영향 없이 `dependent` 리소스만 제거됩니다.
- `owner` 리소스를 제거하는 경우
    - `owner` 리소스 제거 후, `dependent` 리소스가 제거됩니다.
    - 병렬적으로 제거되지 않고, 순차적으로 동작합니다. 별다른 설정이 없다면 `owner` 리소스가 먼저 제거됩니다.
    

### Cascading deletion

위에서 언급했듯 `owner` 리소스를 제거하면 `dependent` 리소스가 제거됩니다. 이렇게 종속관계 리소스들이 흐르듯 제거가 되는 것을 `Cascading deletion` 이라고 합니다.

쿠버네티스에서는 3가지의 Cascading deletion을 제공하고 있습니다.

- **background cascading deletion**
    - `dependent` 리소스가 삭제되기 전에 `owner` 리소스가 삭제됩니다.
- **foreground cascading deletion**
    - `owner` 리소스가 삭제되기 전에 `dependent` 리소스가 먼저 삭제됩니다.
    - `owner` 리소스가 `Terminating` 상태가 된 후, `dependent` 리소스가 삭제됩니다. `dependent` 리소스가 다 완전히 삭제되면, `onwer` 리소스도 삭제됩니다.
- **orphan**
    - `ownerReferences` 를 무시합니다. 즉, 특정 리소스를 제거했을 때 `owner` 관계를 따지지 않고 그 특정 리소스만을 제거합니다.

쿠버네티스는 **background cascading deletion을 default로 사용하고 있습니다.**

- 이는 [공식문서](https://kubernetes.io/docs/tasks/administer-cluster/use-cascading-deletion/#use-foreground-cascading-deletion)와 `[kubectl` reference docs](https://kubernetes.io/docs/reference/generated/kubectl/kubectl-commands#delete)에서 확인할 수 있습니다.

그럼 다른 cascading deletion을 사용하고 싶은 경우엔 어떻게 할까요?

쿠버네티스 API를 사용하면 되지만, 이를 쉽게 활용하게 만든 `kubectl` 사용법만 설명하겠습니다.

- `kubectl delete` 에서 `--cascade` 옵션을 사용하면 됩니다.
    - `background` [default], `foreground`, `orphan`
- ex) `kubectl delete pod pod-owner --cascade foreground`

이 방식을 사용하는데 finalizer를 사용한다.

- https://kubernetes.io/docs/concepts/overview/working-with-objects/owners-dependents/#ownership-and-finalizers
    - kube-api-server, managing controller
