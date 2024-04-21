> [!NOTE]
> 책 내용 + 기타 레퍼런스를 보완한 요약본입니다.

# Chapter 13] 오토스케일링

Auto Scaling이란 **사람의 개입 없이** 워크로드의 용량을 늘리거나 줄이는 프로세스를 의미한다.
아래와 같이 크게 두 가지 범주로 나뉜다.

- 워크로드 오토스케일링
- 클러스터 오토스케일링

이에 대한 접근방식으로는 아래와 같은 사항을 염두에 두어야 한다.

- 비용 관리
  - Public Cloud Provider로부터 서버를 임대하거나 가상화된 인프라 사용에 대해서 비용을 고려해야 한다.
- 용량 관리
  - 보유하고 있는 정적 인프라 세트가 있는 경우, 오토스케일링으로 고정 용량 할당 크기를 동적으로 관리할 수 있다. 가령 비즈니스를 가장 바쁘게 제공해야 하는 시간에 워크로드 오토스케일링을 통해 용량을 동적으로 스케일링하고 필요할 때 많은 양의 클러스터를 활용할 수 있다.

오토스케일링이 없다면 아래 두 가지 단점이 발생한다.
- 지속적으로 애플리케이션 용량을 과도하게 프로비저닝해 비즈니스에 추가 비용을 발생시킨다.
- 엔지니어에게 수동 스케일링 작업을 경보 처리해 추가적인 작업 수고를 초래한다.

오토스케일링은 이미지 크기가 작고 시작 시간이 빠른, 더 작고 민첩한 워크로드에 적합하다.
주어진 노드에 컨테이너 이미지를 가져오는 데 필요한 시간이 짧고 컨테이너가 생성된 후 애플리케이션이 시작되는 데 걸리는 시간도 짧으면 워크로드가 확장 이벤트에 빠르게 응답할 수 있다. 용량도 훨씬 더 쉽게 조정할 수 있다. 이미지 크기가 GB를 초과하는 애플리케이션과 몇 분 동안 실행되는 시작 스크립트는 부하 변화에 응답하는데 부적합하다.

## 스케일링 유형

- 수평 스케일링 (Scale In/Out)
  - 동일한 워크로드에 대해서 Replicas를 변경하는 작업을 포함한다.
    - 이는 특정 애플리케이션 Pod 수 혹은 애플리케이션을 호스팅하는 클러스터 노드 수이다.
- 수직 스케일링 (Scale Up/Down)
  - 단일 인스턴스의 리소스 용량 변경을 포함한다.
    - 컨테이너에 대한 리소스 Requests / Limits가 변경된다.

**동적 스케일링이 필요한 시스템, 즉 부하가 자주 크게 변경되는 시스템에서는 가능하면 수평 스케일링이 선호된다.**
수직 스케일링으로 용량을 늘리려면 시스템 재시작이 필요하다.

## 워크로드 오토스케일링

주로 워크로드의 오토스케일링은 관련 메트릭을 모니터링하고 사용자 개입 없이 워크로드 용량을 조정하는 작업이 포함된다.
Kubernetes에서는 HPA / VPA를 일반적으로 사용하면서 워크로드를 오토스케일링한다.

### HPA (Horizontal Pod Autoscaler)

HPA 리소스와 `kube-controller-manager`에 Bundle로 제공되는 컨트롤러를 사용해 쿠버네티스에서 기본적으로 지원한다. CPU 또는 메모리 사용량을 워크로드 오토스케일링을 위한 메트릭으로 사용하면 HPA 사용에 대한 진입 장벽이 낮다.

[metrics-server](https://github.com/kubernetes-sigs/metrics-server) 를 사용해 HPA에서 Pod Metrics를 사용할 수 있다. `metrics-server`는 `kubelet`에서 컨테이너에 대한 CPU 및 메모리 사용량 메트릭을 수집한다. HPA 컨트롤러에서는 이러한 메트릭을 바탕으로 HPA 구성과 비교하여 Replicas의 조절 여부를 판단한다.

![HPA](../assets/61.png)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: sample
spec:
  selector:
    matchLabels:
      app: sample
  template:
    metadata:
      labels:
        name: sample
    spec:
      containers:
      - name: sample
        image: sample-image:1.0
        resources:
          requests:
            cpu: "100m"
---
apiVersion: autoscaling/v1
kind: HorizontalPodAutoscaler
metadata:
  name: sample
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: sample
  minReplicas: 1
  maxReplicas: 3
  targetCPUUtilizationPercentage: 75
```

#### 한계사항

- 모든 워크로드를 수평적으로 확장할 수 있는 것은 아니다
  - 개별 인스턴스 간 로드를 공유할 수 없는 애플리케이션의 수평 스케일링은 무의미하다.
  - 이는 일부 Stateful 워크로드 및 Leader Election을 사용하는 애플리케이션에 해당되는데 이들은 VPA를 고려해야 한다.

- 클러스터 크기는 확장을 제한한다
  - 애플리케이션이 확장되면 클러스터의 Worker 노드에서 사용 가능한 용량이 부족해질 수 있다.
  - 이 때 사전에 충분한 용량을 Provisioning 하거나 플랫폼 운영자에게 용량을 수동으로 추가하라는 경보 설정, 혹은 클러스터 오토스케일링을 사용해 해결할 수 있다.

- CPU / 메모리는 확장 결정에 사용하기에 적합한 메트릭이 아닐 수 있다
  - 워크로드가 확장 필요성을 더 잘 식별하는 사용자 정의 메트릭을 노출하면 사용할 수 있다.

### VPA (Vertical Pod Autoscaler)

HPA와는 다르게 VPA의 경우 `metrics-server` 외에도 3개의 개별 컨트롤러 컴포넌트 배포가 필요하다. 이런 이유 등으로 VPA는 HPA에 비해 상대적으로 덜 사용된다.

- Recommender
  - 해당 Pod에 대한 Pod Metrics 리소스의 사용량을 기반으로 최적의 컨테이너 CPU나 메모리 요청 값을 결정한다.
- Admission Plugins
  - Recommender의 추천에 따라 생성될 때 새 Pod에 대한 리소스 요청 및 제한을 변경한다.
- Updater
  - Admission Plugins로 적용된 업데이트 값을 가질 수 있도록 Pod를 evict한다.

![VPA](../assets/62.png)

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: sample
spec:
  containers:
  - name: simple
    image: sample-image:1.0
    resources:
      cpu: 100m
      memory: 50Mi
    limits:
      cpu: 100m
      memory: 50Mi
---
apiVersion: autoscaling.k8s.io/v1beta2
kind: VerticalPodAutoscaler
metadata:
  name: sample
spec:
  targetRef:
    apiVersion: v1
    kind: Pod
    name: sample
  resourcePolicy:
    containerPolicies:
      - containerName: '*'
        minAllowed:
          cpu: 100m
          memory: 50Mi
        maxAllowed:
          cpu: 1
          memory: 500Mi
        controlledResources: ["cpu", "memory"]
  updatePolicy:
    # Auto/Recreate - 오토스케일링 활성화, 권장사항의 자동 적용
    # Initial - Pod 생성 시에만 VPA 권장사항이 자동 적용. Admission Control이 수행되나 Pod Evict가 수행되지 않음
    # Off - 자동 변경하지 않음, 권장사항만 제시하며 수동으로 적용해야 함
    updateMode: Recreate
```

Off 모드의 VPA에서 리소스 추천을 받기 위해 `kubectl describe vpa <vpaName>` 명령 실행으로 아래처럼 Status란에서 그 출력 내용을 확인할 수 있다.

```
Recommendation:
  Container Recommendations:
    Container Name: coredns
    Lower Bound:
      Cpu:    25m
      Memory: 262144k
    Target:
      Cpu:    25m
      Memory: 262144k
    Uncapped Target:
      Cpu:    25m
      Memory: 262144k
    Upper Bound:
      Cpu:    427m
      Memory: 916943343
```

### 사용자 정의 측정 항목을 사용한 오토스케일링

일반적으로 CPU와 메모리 소비량으로 워크로드 오토스케일링이 수행되지만, 이러한 메트릭이 애플리케이션의 오토스케일링에 유용한 고려사항이 아닌 경우에는 사용자 메트릭을 대안으로 활용할 수 있다.

사용자 정의 메트릭을 오토스케일링 활용에 노출하기 위해서는 쿠버네티스의 `metrics-server` 대신 사용할 사용자 정의 메트릭 서버를 배포해야 한다. Datadog, Prometheus Adapter와 같은 일부 Provider가 이에 해당한다.

![사용자 정의 측정 항목이 있는 HPA](../assets/63.png)

### CPA (Cluster Proportional Autoscaler)

[CPA](https://github.com/kubernetes-sigs/cluster-proportional-autoscaler)는 클러스터의 노드 수(혹은 노드 하위 집합)를 기반으로 Replica를 확장하는 수평 워크로드 오토스케일링이다. HPA와는 다르게 메트릭 API에 의존하지 않는다. 따라서 `metrics-server`나 `prometheus-adapter`에 대한 의존성도 없다.
또한 쿠버네티스 리소스로 구성하지 않고 플래그를 사용해 대상 워크로드를 구성하고 ConfigMap으로 구성을 확장한다.

![CPA](../assets/64.png)

CPA는 사용 사례가 많지 않은데, **클러스터에 비례해서 확장해야 하는 워크로드는 주로 플랫폼 서비스로 제한**된다. 워크로드가 이미 HPA를 사용하고 있다면 CPA 도입이 필요한지를 평가하는 것이 중요할 것이다.

> #### CPA의 확장 방식
> 1. Linear
> 클러스터에 있는 노드 혹은 코어 수에 정비례해 애플리케이션을 확장한다.
> 2. Ladder
> 단계 함수를 사용해 Nodes:Replicas 혹은 Cores:Replicas 비율로 결정한다.

### 사용자 정의 오토스케일링

HPA, VPA, CPA, `metrics-server` 및 Prometheus Adapter가 아니더라도 사용자가 필요에 따른 오토스케일링을 직접 구현할 수 있다.

가령, 애플리케이션의 트래픽이 증가하는 시점을 알고 있다면, 관련 Deployment에 대해 Replicas를 업데이트하는 CronJob과 같은 것을 활용하여 구현 가능하다.

## 클러스터 오토스케일링

[CA - Cluster Autoscaler](https://github.com/kubernetes/autoscaler/tree/master/cluster-autoscaler)는 클러스터의 Worker Node를 수평으로 확장하기 위한 자동화된 솔루션을 제공한다.

> 이름은 `Cluster Autoscaler`지만 사실 잘 살펴보면 이것은 Node에 대한 스케일링이다.

리소스 부족으로 스케줄링이 안되는 워크로드가 있다면 노드를 Scale out하고, Cluster에 장시간 Utilization이 낮은 노드가 있다면 Pod를 다른 Node에 재배치하고 타겟 Node를 Scale in하는 역할을 수행한다.

![Cluster Autoscaling](../assets/65.png)

> ### Karpenter
> AWS에서 개발한 Cluster Autoscaler이다. Kubernetes의 Cluster Autoscaler에 비해 유연하고 노드 자동 확장에 좀 더 중점을 둔다. 리소스 요구사항 / Taints 및 기타 제약 조건을 고려해서 예약되지 않은 Pod 요구사항을 기반으로 노드를 프로비저닝 한다.
> AWS 환경에 최적화되어 있어 단순 오토스케일링 뿐만 아니라 다양한 인스턴스 타입 사용에 따른 비용 효율화 또한 가능하다.
> AWS 환경에서 CA를 사용하는 경우 AWS의 ASG에 의존하는 형태로 스케일링이 동작하지만 Karpenter는 ASG 없이 직접 노드를 관리하기 대문에 노드 관리를 보다 빠르고 세밀하게 제어할 수 있는 장점을 가진다.
> ![Comparison - CA and Karpenter](https://prismic-io.s3.amazonaws.com/qovery/ef274117-da29-4b46-aec2-d65ad42c3a1f_2022-07-07_01-41.png)

### 클러스터 Over-provisioning

Pod에 대한 Scale Out과는 다르게 Node의 Scale Out은 인스턴스를 새로 띄우는 것이므로 상대적으로 더 오랜 시간이 소요된다. 따라서 Scale out에 대한 시간 절약을 위해 미리 Provisioning을 수행할 수 있는데 이러한 작업을 수행하는 컴포넌트로 [Cluster OverProvisioner](https://github.com/deliveryhero/helm-charts/tree/master/stable/cluster-overprovisioner)가 있다.

```yaml
apiVersion: scheduling.k8s.io/v1beta1
kind: PriorityClass
metadata:
  name: overprovisioning
value: -1
globalDefault: false
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: overprovisioning
  namespace: kube-system
spec:
  replicas: 1
  selector:
    matchLabels:
      run: overprovisioning
  template:
    metadata:
      labels:
        run: overprovisioning
    spec:
      priorityClassName: overprovisioning
      containers:
      - name: reserve-resources
        image: k8s.gcr.io/pause
        resources:
          requests:
            cpu: 1600m
            memory: 550Mi

```

위와 같이 아무 역할을 하지 않는 Pod를 미리 생성하고 Pod의 Priority 를 다른 일반 Pod들보다 낮게 설정한다. 이 때 `PriorityClass`를 사용하여 value를 지정하고 이를 Pod에 연결하여 설정할 수 있다.

이렇게 되면 일반 Pod가 생성될 때 Node에 자리가 부족하다면 이 우선순위가 낮은 Pod를 내쫓고 그 자리를 차지하게 된다. 쫓겨난 Pod가 Scale Out를 트리거하는 방식으로 동작한다.

![Cluster Over Provisioning](../assets/66.png)
![Cluster Over Provisioning](../assets/67.png)