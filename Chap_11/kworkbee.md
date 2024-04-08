> [!NOTE]
> 책 내용 + 기타 레퍼런스를 보완한 요약본입니다.

# Chapter 11] 플랫폼 서비스 구축

플랫폼 서비스는 애플리케이션 플랫폼에 추가 기능을 설치하기 위한 컴포넌트. 일반적으로 컨테이너화된 워크로드로 `*-system` 형태의 namespace에 배포되며 플랫폼 엔지니어링 팀에서 관리한다.

## 확장점

쿠버네티스는 굉장히 확장 가능한 시스템 아키텍처로 구성되어 있다.

### 플러그인 확장

#### 네트워크

CNI는 컨테이너가 연결할 네트워크 제공을 위해 플러그인이 충족해야 하는 인터페이스를 정의한다. Chapter 5에서 설명.

#### 스토리지

CSI는 스토리지 시스템을 컨테이너화된 워크로드에 노출하기 위한 시스템을 제공한다. 서로 다른 프로바이더의 스토리지를 노출하는 다양한 볼륨 플러그인이 있다. Chapter 4에서 설명.

#### 컨테이너 런타임

CRI는 kubelet이 사용 중인 런타임에 신경 쓰지 않도록 컨테이너 런타임으로 노출돼야 하는 작업에 대한 표준을 정의한다. Chapter 3에서 설명.

#### 디바이스

K8s Device Plugin Framework를 사용하면 Workload가 기본 노드 Device에 액세스 할 수 있다. Chapter 2에서 설명.

### 웹훅 확장

Kubernetes에서의 웹훅 확장은 Kubernetes API 서버가 핵심 API의 사용자 정의 변환을 수행하려고 호출하는 백엔드 서버 Role을 적용한다.

- AuthN / AuthZ
- 리소스 스키마 유효성 및 제어 유효성 검사 (Admission Control)

#### 인증 확장

OIDC와 같은 인증 확장은 API 서버에 대한 인증 요청 작업을 Offload할 수 있는 기회를 제공한다.

Chapter 10에서 설명.

또한 API 서버가 Webhook을 호출해 인증된 사용자가 리소스를 수행할 수 있는 작업을 승인하도록 할 수 있다. K8s에 기능 기반 액세스 제어 시스템이 내장되어 있으므로 보기 드문 구현 방식이다. 그러나 어떤 이유로든 시스템이 부적절하다고 판단되면 인증 확장 옵션을 사용할 수 있다.

#### Admission Control

특정 요청이 생성되면 API 서버는 Admission Webhook Configuration의 유효성 검사 및 변경에 따라 적용 가능한 Admission Webhook을 호출한다. Chapter 8에서 설명.

### Operator 확장

Kubernetes Operator는 K8s API 서버의 클라이언트로 작업자를 대신해 작업을 자동으로 수행한다. 이러한 Operator의 확장은 애플리케이션 플랫폼에 대한 Control Plane의 사용자 정의 확장으로 생각하면 된다. (API 리소스에 대한 컨트롤러 구현과 유사)

## Operator 패턴

### Kubernetes Controller

Kubernetes Control Plane에는 기본 리소스에 대해서 Current State를 Desired State로 Reconcile하는 역할을 수행하는 Controller가 내장되어 있다. 따라서 사용자는 기본 리소스에 대한 Desired State를 Manifest로 배포하면 Controller가 이에 맞게 Current State를 Desired State로 맞추고자 노력한다.

장애로 인해 Desired State가 충족되지 않으면 계속해서 Infinite Loop로 Retry한다. 따라서 최종적으로는 Self-healing 되며 원하는 상태로 만족하게 된다.

### Custom Resource

Kubernetes의 기본 리소스가 아니더라도 사용자 정의 애플리케이션에서 사용할 수 있도록 별도의 CRD (Custom Resource Definition)를 통해 사용자 정의 리소스를 선언할 수 있고, 클라이언트 라이브러리 SDK를 사용하여 Controller 동작 방식과 유사하게 애플리케이션을 구현하여 원하고자 하는 상태로 변경시킬 수 있다. CRD 명세는 OpenAPI v3 Schema를 따른다.

## Operator Use-cases

Operator의 실제 구현/사용사례는 아래와 같은 범주로 구분된다.

### Platform Utilities

K8s 위에 플랫폼 서비스를 제공하기 위한 케이스이다.

- `cert-manager`
  - 인증서 관리를 서비스 기능으로 제공. X509 인증서 생성 및 갱신 서비스를 제공해 인증서 관리의 상당한 수고와 가동 중지 시간 가능성을 제거.
- `kube-prometheus-stack`
  - 플랫폼에서 메트릭 수집, 저장 및 경보 플랫폼 서비스 제공
- `Rook`
  - `Ceph`와 같은 Provider를 통해 Block, Object 및 Filesystem 스토리지를 서비스로 관리하는 Storage Operator

### Universal Workload Operator

배포된 워크로드의 공통 패턴을 개발한 조직에서 범용 방식으로 복잡성을 추상화하기 위해 별도 구현하는 경우로, 워크로드와 관련된 리소스가 많을 경우 이를 별도의 리소스로 추상화하여 해당 리소스를 정의해 관련 리소스로 변환하기 위해 구현하는 케이스이다.

### App-specific Operator

Stateful 애플리케이션을 관리하기 위한 리소스 정의 및 이를 관리하는 Operator를 구현한 케이스이다. 대표적으로는 RDBMS와 같은 워크로드가 해당된다.

## Operator 개발

[이 블로그](https://11st-tech.github.io/2022/07/20/eurekube-operator/) 사례가 아주 잘 설명하고 있다. ([추가](https://11st-tech.github.io/2022/08/01/eurekube-operator-test/))

### Operator 개발 도구 사용

- kubebuilder
- metacontroller
- Operator Framework

### 로직 구현

- 구현 상태
- 원하는 상태
- 조정
- 구현 세부 정보
- Admission Webhook
- finalizer

> `finalizer`
>
> 리소스 삭제 시 특정 조건을 충족할 때까지 삭제를 방지하는 역할을 수행한다.
> - 메타데이터 파일 `.metadata.finalizers` 필드에 조건을 기술한다.
> - 삭제 명령이 호출되면 K8s API가 해당 리소스에 `.metadata.deletionTimestamp` 추가 후 Status Code 202를 Return 한다.
> - 해당 오브젝트는 컨트롤러가 finalizer 정의 조건을 충족할 때까지 Terminating 상태로 대기한다.
> - 조건이 충족되면 컨트롤러가 finalizer 충족된 키를 제거한다.
> - `.metadata.finalizers` 가 empty 가 되면 오브젝트가 제거된다.

## 스케줄러 확장

### 판단식 및 우선순위

K8s 스케줄러는 2단계로 진행되어 Pod가 스케줄링될 노드를 선택한다.

1. 필터링
  - Taint Predicates 등으로 호스팅 불가한 노드를 필터링한다.
  - (혹은 리소스 요청에 대한 노드의 충분한 리소스 보유 여부 확인)
  - 이러한 필터링으로 단일 노드가 필터되면 해당 노드에 배포하며 그렇지 않으면 다음 단계 진행

> 스케줄링 방법으로 노드 셀렉터, Node/Pod Affinity, Taint/Toleration, Cordon/Drain/PDB 등이 있다.

2. 채점
  - 우선순위를 사용해 특정 파드에 가장 적합한 노드 결정
  - 우선순위는 실행 가능한 노드별로 점수를 측정하는 기능 세트를 수행한 후 가장 높은 점수의 노드를 선택하여 워크로드를 배포하게 된다.

### 스케줄링 정책

스케줄링 정책은 Control Plane의 스케줄러 실행 때 `--policy-config-file` 플래그를 지정하는 방법이 있지만 더 선호되는 방식은 `ConfigMap`을 사용하는 것이다.

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: scheduler-policy-config
  namespace: kube-system
data:
  policy.cfg: |-
    apiVersion: v1
    kind: Policy
    predicates:
    - name: "PodMatchNodeSelector"
    argument:
      labelsPresence:
        labels:
        - "selectable"
        presence: true
```

이 정책이 적용되면

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: terminator
spec:
  containers:
  - image: registry.acme.com/skynet/t1000:v1
    name: terminator
  nodeSelector:
    device: gpu
```

`device: gpu` 및 `selectable: ""`을 레이블로 가지는 노드에만 스케줄링된다.

### 스케줄링 프로필

스케줄링 프로필을 사용하면 스케줄러로 컴파일되는 플러그인의 활성화/비활성화가 가능하다. 스케줄러를 실행할 때 `--config` 플래그에 파일 이름을 전달해 프로필 지정이 가능하다.

### 다중 스케줄러

스케줄러는 여러 개를 배포하는 것이 가능하며 여러 스케줄러를 실행했을 때 Pod 명세에 `schedulerName`을 지정할 수 있다.

### 사용자 정의 스케줄러

스케줄러 또한 사용자가 직접 개발하고 이를 배포하여 스케줄링할 수도 있다.