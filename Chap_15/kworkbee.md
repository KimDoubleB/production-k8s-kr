> [!NOTE]
> 책 내용 + 기타 레퍼런스를 보완한 요약본입니다.

# Chapter 15] CI/CD 파이프라인

## 컨테이너 이미지 빌드

- 애플리케이션을 특정 형태로 패키징하고 빌드하여 배포 환경에서 정상적으로 실행하기 위해 사용 가능한 올바른 종속성과 구성이 환경에 포함되어 있는지 확인해야 한다.
- 애플리케이션 소스코드를 컨테이너 이미지로 빌드할 때 발생할 수 있는 여러 가지 보안적 요소들을 고려할 필요가 있다.
  - 빌더를 Root로 실행 가능한가?
  - Docker Socket을 mount해도 되는가?
  - Daemon 실행에 관심이 있는가?
  - Build를 Container화 할 것인가?
  - 쿠버네티스 워크로드간 실행 가능한가?
  - Layer Caching을 얼마나 활용할 계획인가?
  - 도구 선택이 빌드 배포에 미치는 영향은 무엇인가?
  - 어떤 Frontend 또는 이미지 정의 메커니즘을 사용하고자 하고, 무엇을 지원하는가?

> [컨테이너 빌드에 대한 권장사항 - Google Cloud](https://cloud.google.com/architecture/best-practices-for-building-containers?hl=ko)

### Base Image 선택

- 이미지가 평판이 좋은 Vendor에서 Publish 했는지
- 지속적으로 업데이트되는 이미지인지
- 오픈소스 빌드 프로세스 또는 사양을 준수하는 이미지인지
- 불필요한 도구나 라이브러리 없이 최소한으로 구성된 이미지인지

> #### 최소 크기의 이미지
> alpine
> - 경량화된 Linux 배포판으로 애플리케이션 실행 위한 최소한의 환경 제공
> - 작은 이미지 크기와 빠른 시작 시간으로 컨테이너화된 애플리케이션 구축에 유리
> distroless
> - 매우 경량화된 형태로 실행에 필요한 최소한의 요소만을 포함
> - 시스템 라이브러리 및 기타 도구를 제거하여 보안적으로 안전하며 적은 이미지 크기를 가짐
> scratch
> - 빈 이미지로 아무것도 포함되지 않음
> - 개발자가 필요한 모든 것을 셋업하여 빌드해야 하므로 이미지 크기를 극도로 최소화할 수 있으며 주로 정교한 이미지를 만들거나 특정 요구사항 만족을 위해 사용할 수 있다.

### 런타임 사용자

빌드를 위한 `Dockerfile` 구성에 있어서 별도 명시가 없으면 기본적으로는 `root` 사용자로 프로세스가 실행된다. 따라서 `root`로 접근해야 하는 요구사항이 존재하지 않는 이상 공격 벡터를 제거하기 위하여 일반 사용자로 접근하도록 별도로 명시하는 것이 권장된다.

```Dockerfile
FROM gcr.io/distroless/base
USER nonroot:nonroot
COPY ./my-app /my-app
CMD ["./my-app", "serve"]
```

### 패키지 버전 고정

패키지 버전을 지정하지 않으면 애플리케이션을 손상시킬 수 있는 다른 패키지를 사용하게 될 위험이 있으므로 컨테이너 이미지에 포함되는 의존성에는 지정된 패키지 버전을 명시할 것을 권장한다.

### 빌드와 런타임 이미지 비교

빌드 과정에서 사용되는 의존성과 애플리케이션 런타임 시점에서 사용되는 의존성에 차이가 있으므로 빌드 구성시 각 단계에 따른 분리 조치가 필요하다.

```Dockerfile
FROM golang:1.12.7 as build
WORKDIR /my-app
COPY go.mod .
RUN go mod download
COPY main.go .
ENV CGO_ENABLED=0
RUN go build -o my-app

FROM gcr.io/distroless/base
USER nonroot:nonroot
COPY --from=build --chonw=nonroot:nonroot /my-app/my-app /my-app
CMD ["/my-app", "serve"]
```

### [CNB (Cloud Native Buildpack)](https://buildpacks.io/)

개발자가 별도로 `Dockerfile`을 구성하지 않아도 해당 도구가 애플리케이션 소스코드를 분석하고 컨테이너 이미지를 자동으로 생성하도록 지원한다.

![Buildpacks - Intro](https://buildpacks.io/images/what.svg)
![Buildpacks - How it works](https://buildpacks.io/images/how.svg)
![Builder](https://buildpacks.io/images/create-builder.svg)

다음과 같이 동작한다.

1. Detecting
애플리케이션 소스 코드를 분석해 필요한 종속성과 빌드 환경을 감지한다. 이 과정에서 사용된 언어와 프레임워크, 라이브러리를 식별한다.
2. Building
감지된 종속성과 빌드 환경에 따라 애플리케이션을 빌드한다. 이는 필요한 실행 파일과 라이브러리를 다운로드하고 구성하는 단계를 포함한다.
3. Exporting
빌드가 완료되면 애플리케이션을 컨테이너 이미지로 패키징한다. 이 이미지에는 빌드에 필요한 모든 파일과 설정이 포함된다.
4. Optimizing
최적화를 위한 다양한 방법이 포함되며 이미지 크기를 줄이고 실행 속도를 향상시키는 등의 프로세스가 포함된다.

## Image Registry

이렇게 빌드된 이미지를 보관할 별도의 장소를 구성해야 한다. Image Registry는 서버와 Blob 저장소, 데이터베이스 세 가지 구성요소로 구성된다.

- Artifactory, Nexus
- AWS ECR, GCR, ACR

### 취약점 스캐닝

이미지 상의 보안 취약점이 미칠 수 있는 영향이 상당하므로 스캐닝하여 이를 감지하고 보완할 수 있는 프로세스가 함께 도입되어야 한다.

특히 정적 스캐닝의 경우 처음에는 유용할 수 있으나, 시간이 지남에 따라 발견되는 취약점 또한 존재할 수 있으므로 이에 대한 사항을 감지하고 이미지 업데이트를 할 수 있는 절차가 필요하다. 발견에 따른 조치는 CI/CD 파이프라인을 트리거함으로써 달성 가능하다.

### 검역 워크플로

이미지 요청에 대한 제약사항을 Admission Controller와 결합하여 반영할 수 있다. 가령, 이미지 Pull 요청이 있을 때 외부의 레지스트리가 아닌 사내 레지스트리를 사용하는지, 이미지를 가져온 후 검역소로 보내 이미지 스캐닝 후 취약점 등의 이상이 없는지를 확인할 수 있다.

![Quarantine Workflow](https://toddysm.com/wp-content/uploads/2022/09/quarantine-patters-build-container-image-flow.svg)

### 이미지 서명

이미지 게시자가 이미지의 해시를 생성하고 해당 ID를 레지스트리에 푸시하기 전에 이미지와 연결해 이미지에 암호로 서명할 수 있다. 그런 다음 사용자가 게시자의 공개 키에 서명된 해시의 유효성을 검사해 이미지 진위를 확인할 수 있다.

![Image Sign Process](https://miro.medium.com/v2/resize:fit:1400/format:webp/1*ilfnz74LfuSpnhDpCFGDKQ.png)

이러한 서명 프로세스를 CI/CD 워크플로우에 통합시켜 파이프라인 시작 부분에서 이미지를 생성하고 파이프라인 각 단계 후에 서명할 수 있다. 테스트 완료 후 서명할 수 있으며, 릴리즈 관리 팀에서 배포 승인 후 다시 서명할 수 있다. 그런 다음 배포할 때 지정한 여러 당사자가 서명했는지 여부에 따라 이미지 배포를 운영 환경으로 전환할 수 잇다.

## Continuous Deployment

### 파이프라인에 빌드 통합

코드 커밋이 신규로 생성되면 즉시 새 이미지 빌드가 트리거되도록 파이프라인을 구성할 수 있을 것이다. Cloud Native 파이프라인 도구 `Tekton`으로는 아래와 같이 자체 CRD를 통해 정의할 수 있다.

```yaml
apiVersion: tekton.dev/v1beta1
kind: Task
metadata:
  name: git-clone
spec:
  workspaces:
  - name: output
    description: "..."
  params:
  - name: url
    type: string
  - name: revision
    type: string
    default: master
  results:
  - name: commit
  steps:
  - name: clone
    image: "gcr.io/tekton-releases/github.com/tektoncd/pipeline/cmd/git-init:v0.12.1"
    script: |
      <... snippets ...>
```

```yaml
apiVersion: tekton.dev/v1beta1
kind: Task
metadata:
  name: buildpacks-phases
spec:
  params:
  - name: BUILDER_IMAGE
    type: string
  - name: PLATFORM_DIR
    type: string
    default: empty-dir
  - name: SOURCE_SUBPATH
    type: string
    default: ""
  resources:
    outputs:
    - name: image
      type: image
  workspaces:
  - name: source
  steps: |
    <... snippets ...>
```

### Push 기반 배포

![Push based Deployment](https://assets-global.website-files.com/622642781cd7e96ac1f66807/64b6277cc28963be1e445f19_image2.png)

### 롤아웃 패턴

- 카나리
  카나리 릴리즈가 새 버전의 애플리케이션이 클러스터에 배포되고 작은 트래픽 하위 집합 (메타데이터, 사용자 또는 기타 속성 기반)이 새 버전으로 전달된다. 적은 비율로 새 버전을 배포시켜 오류 시나리오를 면밀히 모니터링하고 시간이 흐르며 신뢰도가 높아짐에 따라 트래픽 비율을 천천히 증가시킬 수 있다.
- 블루/그린
  카나리와 유사하나 트래픽의 빅뱅 Cut-over가 더 많이 포함되는 방식이다. 서로 다른 버전에 대한 서버 셋을 미리 구축하고, 라우팅을 순간적으로 전환시켜 새로운 버전을 배포하는 방식으로 빠른 롤백이 가능하다. 단 자원이 두배로 요구되는 단점이 있다.
- A/B 테스트
  기능적인 업그레이드를 위한 것이 아니라, 서로 다른 두 가지 버전의 애플리케이션 중 유용성, 인기도, 눈에 띄는 정도 등의 다양한 이유로 인해 애플리케이션 기능을 테스트 하는 방법이다.

### GitOps

GitOps는 명령식으로 클러스터에 변경사항을 Push하는 방식을 대신하는 모델로 클러스터에서 실행 중인 리소스와 Git 저장소의 내용을 지속적으로 Reconciliation을 수행하는 컨트롤러를 특징으로 한다. ArgoCD와 Flux가 대표적인 도구이다.

![GitOps](https://blogs.vmware.com/cloud/files/2021/02/GitOps-in-a-nutshell-1024x409.png)

GitOps를 사용하는 이점은 다음과 같다.
- 본질적으로 선언적이므로 배포 자체의 문제 또는 배포가 임시로 삭제되면 정상 상태로의 조정이 시도된다.
- Git은 형상 관리 저장소가 되며 기본적으로 변경사항에 대한 강력한 감사 로그를 얻는 것 외에도 기존 전문 지식과 도구에 대한 친숙함을 활용할 수 있다. 사용자는 클러스터에 변경사항의 게이트로서 Pull Request 워크플로우를 사용할 수 있으며, 대부분의 버전 관리 시스템에서 노출하는 확장 지점(웹훅, 워크플로우, 액션 등)을 통해 필요에 따라 외부 도구와 통합할 수 있다.

> 단점도 물론 존재한다. Secret과 같이 Credential을 포함하는 데이터는 Plain 상태 그대로 Repo에 반영하는 것은 보안적인 관점에서 매우 위험하다. (SealedSecret과 같은 프로젝트를 사용해야 한다.) 또한 현재 동기화 상태를 지속적으로 모니터링해야 하며 장기간 Current State != Desired State 를 유지하게 되면 이에 대한 경고 알림을 구현할 필요가 있다.