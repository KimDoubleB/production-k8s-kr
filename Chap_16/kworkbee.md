> [!NOTE]
> 책 내용 + 기타 레퍼런스를 보완한 요약본입니다.

# Chapter 16] 플랫폼 추상화

## 플랫폼 노출

서비스 개발팀 개발자가 쿠버네티스 환경에 개발한 서비스를 배포하고 운영하기까지의 과정에 있어 플랫폼의 구현 세부사항을 그들에게 노출하는 범위를 최소화하는 것이 좋다. 플랫폼 엔지니어링 팀 엔지니어들은 플랫폼의 추상화를 통해 적절한 수준에서의 플랫폼을 노출하여 적은 오버헤드로도 가치 있는 소프트웨어 Serving이 가능하도록 지원하여야 한다.

## 자체 서비스 온보딩

- 클러스터 프로비저닝 및 구성
  - 단일 테넌트 모델
  - 다중 테넌트 클러스터
    - namespace
    - RBAC
    - NetworkPolicy
    - Quota
    - LimitRange
    - PodSecurityPolicy

- CI/CD 도구

## 추상화 범위

### 커맨드라인 툴

기본적으로 쿠버네티스 클러스터에 대한 어떠한 처리를 진행하고자 한다면 커맨드라인 도구인 `kubectl`을 사용하게 된다. 그러나 출력 형식의 부족한 가시성과 사용성 등의 이유로 이 도구만으로는 관리의 어려움이 초래될 수 있다.

더 나은 경험을 제공하는 `kubens`, `kubectx`와 같은 커맨드 툴, 클러스터 상호작용을 더 쉽게 할 수 있도록 사용자 경험을 제공하는 GUI 툴인 OpenLens나 Octant를 활용하 수 있을 것이다.

### 템플릿을 통한 추상화

쿠버네티스 단일 애플리케이션을 배포하는데 있어 Deployment, (필요에 따라 StatefulSet), Service, PVC, ConfigMap, Secret과 같은 다양한 오브젝트를 구성해야 한다. 매 애플리케이션마다 이러한 오브젝트들을 처음부터 끝까지 구성하는 것은 매우 번거롭고 귀찮은 일이다.

Helm이나 Kustomize 같은 템플릿 도구를 사용해서 애플리케이션 배포를 위한 공통의 Chart를 구성하고, Values 주입을 통해 custom한 값을 설정할 수 있도록 지원된다면 상대적으로 편의가 향상될 것이다.

### 쿠버네티스 초기 추상화

자체 Operator를 구축해서 CRD에 일치하는 Custom Resource를 구성하고 이를 배포하면 여기서 정의된 사양에 맞게 Operator가 애플리케이션에 필요한 오브젝트를 배포하도록 구축할 수 있고, 플랫폼 팀은 이러한 배포 과정을 Programatically하게 코드로 구현할 수 있을 것이다.

### 쿠버네티스를 보이지 않게 만들기

서비스 개발팀이 쿠버네티스를 사용한다는 사실조차 알 수 없을 정도로 소프트웨어 수명주기에서 필요한 모든 작업을 추상화할 수 있다. Web을 매개로 하는 인터페이스를 통해 Self-Service를 활성화할 수 있을 것이다.

> #### [Internal Developer Platform](https://internaldeveloperplatform.org/)
>
> - [당근 Kontrol](https://speakerdeck.com/outsider/danggeun-gaebalja-peulraespomeun-eoddeon-munjereul-haegyeolhago-issneunga?slide=72)
> - [카카오페이증권 Wallga](https://tech.kakaopay.com/post/kakaopaysec-devops-platform/#cicd-platform---wallga)
> - 11번가 Wheelhouse