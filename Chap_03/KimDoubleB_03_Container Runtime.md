
책 정리를 기반으로 궁금해진 것, 궁금해왔던 것들을 들을 확인해보았다.

# 개요

쿠버네티스 자체는 컨테이너를 생성, 시작, 중지하는 법을 알지 못한다. 
대신 이런 작업을 **컨테이너 런타임**이라는 컴포넌트가 담당해 진행한다. 이 컨테이너 런타임에 대해 알아보자.

컨테이너 런타임을 간단하게 설명하면 다음과 같다.
- **Linux**: cgroups 및 namespace 같은 커널 기능을 이용하여 컨테이너 프로세스를 생성하는 녀석
- **Kubernetes**: kubelet과 함께 동작하며 쿠버네티스 node에서 컨테이너를 생성 및 관리하는 녀석

그럼 2가지 의문점이 든다.
- 왜 쿠버네티스는 몰라야하는가? 명색의 컨테이너 기반 오케스트레이션인데 몰라도 되나?
- kubelet이 컨테이너를 관리한다고 했다. kubelet 내부에 컨테이너 런타임이 있다고 보면 되나?

이 의문점에 대해 답을 하기 위해 OCI, CNI 그리고 컨테이너 런타임을 살펴보자.

# OCI (Open Container Initiative)
2013년 Docker가 등장하고 컨테이너가 인기를 얻으며 생태계가 확장되어가기 시작했다.
그러다보니 우후죽순 관련된 프로젝트들이 생겨나며 별도의 표준을 가지기 시작하며, 표준과 명세의 필요성이 두드러졌다.

이에 따라 여러 단체들이 모여 컨테이너 명세를 정의하기 위한 오픈소스 프로젝트로 OCI를 만들었다.
- https://opencontainers.org/
- https://github.com/opencontainers


OCI에서는 Container에 대해 **3가지의 명세**를 정의했다.
- OCI 런타임 명세 ([Runtime spec](https://github.com/opencontainers/runtime-spec/blob/main/spec.md))
	- 컨테이너를 어떻게 실행할지에 대한 설정 및 CLI 명령어 표준
- OCI 이미지 명세 ([Image spec](https://github.com/opencontainers/image-spec/blob/main/spec.md))
	- 컨테이너를 만들 정보를 정의, 파일 규격에 대한 표준
- OCI 배포 명세 ([Distribution spec](https://github.com/opencontainers/distribution-spec/blob/main/spec.md))
	- 컨테이너 정의 파일을 어떻게 배포할지에 대한 HTTP API 표준
	- 컨테이너 이미지 레지스트리와 관련된 내용 (ex. docker hub, ECR, harbor 등)
- 이 명세들은 계속해서 개선되어져 가고 있다 ([참고 link](https://opencontainers.org/posts/blog/2023-07-21-oci-runtime-spec-v1-1/)).


## OCI Runtime Spec

컨테이너 인스턴스화 및 실행 방법을 정의한다.
- 컨테이너 구성 스키마
	- 루트 파일 시스템, 실행할 명령, 환경 변수, 사용자 및 그룹, 리소스 제한 등 정보
- 생성, 시작, 종료, 삭제 및 상태 등 생명주기, 진행하는 여러 단계 구분

OCI 프로젝트에는 런타임 명세를 구현하는 저수준 컨테이너 런타임 `runc`도 있다.
![](https://private-user-images.githubusercontent.com/37873745/294755889-3c6bb963-3d8a-43d4-9b0f-4f28147e0be6.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MDQ2Mjg2NTMsIm5iZiI6MTcwNDYyODM1MywicGF0aCI6Ii8zNzg3Mzc0NS8yOTQ3NTU4ODktM2M2YmI5NjMtM2Q4YS00M2Q0LTliMGYtNGYyODE0N2UwYmU2LnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNDAxMDclMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjQwMTA3VDExNTIzM1omWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPTJjNDEwZTdkNTkwODA1MDU5ZDAwYzdkYzE1NTM2YjNjYmNmMjc0ZGExNWQyNzNlNGY1MjljMmEwMTc0NDJkYmImWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0JmFjdG9yX2lkPTAma2V5X2lkPTAmcmVwb19pZD0wIn0.jMhAQ6B3KXKScbSUbvAcL6pRSPdbd9TX3FnBh0HYfPE)
- 상위 수준의 컨테이너 런타임인 docker, containerd, CRI-O는 `runc`를 이용해 OCI 사양에 맞게 컨테이너를 생성함.
- `runc`를 이용해 OCI 런타임 사양을 준수하면서, 이미지 가져오기, 네트워킹 구성, 스토리지 처리 등과 같은 상위 수준 기능 개발에 집중할 수 있음.


## OCI Image Spec
컨테이너 이미지 구성을 정의한다. 크게 4가지로 나누고 있다.

![](https://private-user-images.githubusercontent.com/37873745/294755880-f7d2e7ca-0eed-446b-a9e7-2aefbcf4d31b.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MDQ2Mjg2NTMsIm5iZiI6MTcwNDYyODM1MywicGF0aCI6Ii8zNzg3Mzc0NS8yOTQ3NTU4ODAtZjdkMmU3Y2EtMGVlZC00NDZiLWE5ZTctMmFlZmJjZjRkMzFiLnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNDAxMDclMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjQwMTA3VDExNTIzM1omWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPWRiOTNlNTk5NDA1Y2Y1OThjYzExYjEwOWM5YjY5OTE3MmI3NjUwZmUxYTEzZWI4YWEyNjcwZmFhYmExYzc3ODkmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0JmFjdG9yX2lkPTAma2V5X2lkPTAmcmVwb19pZD0wIn0.cNt4PahvaHHRlCcp7u3HhQluRlMAqaNRDnxS_Q7CsnU)
![](https://private-user-images.githubusercontent.com/37873745/294755882-2c6b7583-af11-45e4-b347-8e23fbddd71b.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MDQ2Mjg2NTMsIm5iZiI6MTcwNDYyODM1MywicGF0aCI6Ii8zNzg3Mzc0NS8yOTQ3NTU4ODItMmM2Yjc1ODMtYWYxMS00NWU0LWIzNDctOGUyM2ZiZGRkNzFiLnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNDAxMDclMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjQwMTA3VDExNTIzM1omWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPTc1Yzk3ZWRmNzA3ZWMzZGVkOTlkM2RjNzFhYjRkMzVkMDA4NmNhN2ExYjAwMDg1MjljMTZmNTZlZDQ3NTI0YWEmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0JmFjdG9yX2lkPTAma2V5X2lkPTAmcmVwb19pZD0wIn0.5tdZvhWmMMMvlblpSH0JkICV1n4OW-I0J0Tg-hxWlH0)
- **Image index**
	- 각 CPU/OS 환경 별로 어떠한 manifest가 있는지 기록된 JSON 양식 파일
- **Image manifest**
	- 컨테이너 이미지에 대한 정보가 담긴 JSON 파일로, Config/Layer 정보가 있음.
- **Image config**
	- 컨테이너 이미지가 어떻게 만들어졌고, 어떻게 실행 가능한지 (환경변수, 커맨드 등)에 대한 메타데이터.
	- 이미지 엔트리포인트, 커맨드, 작업 디렉터리, 환경 변수 등이 포함.
- **Image layer**
	- TAR 형식으로 압축된 각 레이어에 대한 파일
	- Image manifest는 하나 이상의 레이어를 참조. 참조는 SHA256 다이제스트를 사용해 특정 레이어를 가리킴.
	- 최종 컨테이너 이미지 파일 시스템(rootfs)은 manifest에 나열된 대로 각 레이어를 적용한 결과


이미지 파일로 이를 직접 확인해보자.
![](https://private-user-images.githubusercontent.com/37873745/294755884-aa99bd07-dbc5-40f2-a4b4-54f61ae0e605.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MDQ2Mjg2NTMsIm5iZiI6MTcwNDYyODM1MywicGF0aCI6Ii8zNzg3Mzc0NS8yOTQ3NTU4ODQtYWE5OWJkMDctZGJjNS00MGYyLWE0YjQtNTRmNjFhZTBlNjA1LnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNDAxMDclMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjQwMTA3VDExNTIzM1omWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPTM0ZmU0MzBlYzZiMDcyMWRhOTFmYjFmZTVlNmUwYzZiNzg3OWQ2NTM2ZmQ3ZDk1ZDEzY2IzYjE3YWQ4NzEzMDQmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0JmFjdG9yX2lkPTAma2V5X2lkPTAmcmVwb19pZD0wIn0.i-aWqpwabYVSw7HByLUHdKdmxeYQurtCcVIHptB0uBE)
![](https://private-user-images.githubusercontent.com/37873745/294755886-4f91207c-03d3-4808-9e54-29407b797619.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MDQ2Mjg2NTMsIm5iZiI6MTcwNDYyODM1MywicGF0aCI6Ii8zNzg3Mzc0NS8yOTQ3NTU4ODYtNGY5MTIwN2MtMDNkMy00ODA4LTllNTQtMjk0MDdiNzk3NjE5LnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNDAxMDclMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjQwMTA3VDExNTIzM1omWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPWRlZDg3MjExOWVkOGU5NDM4OGY4ODA5Y2ZlZjEzZGE3ZWM1YmY3ZGY4NTU4NTBjOWFjNWYxMzUxNzZiOGE2MmImWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0JmFjdG9yX2lkPTAma2V5X2lkPTAmcmVwb19pZD0wIn0.FUXBZkWY3IfFZGw9cRuPkweNq0qS7xZXu45s3gXcxJk)
- 직접 받지 않고 `docker manifest inspect` 명령어를 통해서도 확인이 가능하다.

앞에서 설명한 이미지를 이루는 파일들이 존재하는 것을 확인할 수 있다.
- 빨간색: Image manifest
- 초록색: Image config
- 파란색: Image layer
- [참고](https://gist.github.com/KimDoubleB/db2b3a0dc9897d6490ebb649098c4b36)


**OCI에서 컨테이너 image spec은 더욱이 중요하다.**
- 다양한 도구 및 컨테이너 기반 플랫폼 간에 이식 가능하도록 보장하기 때문.
- 다양한 컨테이너 이미지 빌드 도구들이 이를 참조하고 있음.
	- ex) kaniko, Buildah, Jib, Cloud Native Buildpack
- 컨테이너 이미지 빌드하는데 도구에 관계없이 컨테이너 이미지를 실행할 수 있도록 함.



**경험**
- `jib`, `buildpack`으로 하는 것의 차이는 무엇일까요?
	- 이전에 혼자 개발하던 시기 EC2에서 docker 이미지를 올려 테스트했던 경험.
	- buildpack으로 빌드했을 때는 OOM이 났었는데, Jib으로 했을 때는 OOM이 나지 않았다.
		- 뭔가 내부적으로 컨테이너 이미지를 만들 때 차이가 있구나?
	- Dockerfile 같이 명세를 내가 적어준 것이 아닌 이상 어떤 식으로 파일이 구성되는지 확인하기 어렵다.
	- 이제는 알 것 같다. Image manifest 파일을 보면 된다.

알아두면 좋을 이야기
- OCI 전에는 비공식적으로 거의 docker가 표준이었음.
- runc, containerd 다 원래 docker 내에서 만들었던 프로젝트였음.
- docker 내부에서는 containerd를 이용하고 있음. 


# CRI (Container Runtime Interface)

kubelet에서 컨테이너 런타임의 구현 세부정보를 추상화 하는 인터페이스
- 말로만 하니 어렵게 느껴진다.
- OCI는 컨테이너 자체에 대한 명세를 의미한다면, CRI는 쿠버네티스 환경에서 컨테이너를 다루기 위한 인터페이스.

맞춤형 애플리케이션 플랫폼을 구축할 수 있는 쿠버네티스
- 컨테이너 오케스트레이션 => **가장 중요한 확장 지점은 컨테이너 런타임 인터페이스 (CRI)**
- 1.5 버전에 등장했다. 이전 버전에서 새 컨테이너 런타임 지원을 추가하려면 직접 쿠버네티스 코드를 아주-잘 알아야했다. CRI가 도입된 이후로는 단순히 인터페이스(CRI)를 준수해 호환성을 보장할 수 있다.

## 잘 이해가 안가요
아직 잘 이해하지 못했다면, 이러한 질문이 떠오를 수 있다.
**"CRI가 필요해? 그냥 OCI로 다 커버하면 안돼?"**


'컨테이너 런타임'의 입장에서 생각해보자.
- **OCI: 컨테이너 런타임이 컨테이너를 생성/관리 등 할 때 지켜야하는 Spec** 
	- 컨테이너란 이런 거고, 이런 역할이 있고, 이런 것을 준수해야 해
	- OCI spec에 맞춰 구성하지 않으면 다른 컨테이너 툴에서 제대로 동작하지 않는다.
- **CRI: kubelet에서 컨테이너 런타임을 다루기 위한 인터페이스** 
	- 쿠버네티스에선 컨테이너 생성 로직을 몰라도 돼. 이 인터페이스로만 요청할거야. 그니깐 너네(컨테이너 런타임)가 이런거 구현해놔.
	- 컨테이너 런타임이 이 인터페이스를 지키지 않으면 쿠버네티스에서 컨테이너 관리 시에 해당 컨테이너 런타임을 사용할 수 없다 (그래서 이를 위한 Adapter들이 존재 -> ship/plugin 등).

## CRI 구조 및 기능
- gRPC 및 protobuf로 구현
- 2가지 서비스 정의 ([CRI API protobuf](https://github.com/kubernetes/cri-api/blob/master/pkg/apis/runtime/v1/api.proto))
	- `RuntimeService`:  파드 생성/삭제, 컨테이너 시작/종료 등 파드 관련 작업처리 
		- (ex. RunPodSandBox, CreateContainer,StartContainer)
	- `ImageService`: 컨테이너 이미지 확인/가져오기/제거 등 컨테이너 이미지 관련 작업처리 
		- (ex. ImageStatus, PullImage)
- Pod 및 Container 관리, 컨테이너 내 명령 실행(exec), 컨테이너에 연결(attach), 컨테이너 포트 전달(PortForward) 등의 역할을 맡는다.

전체적인 뷰를 보면 다음과 같다.
![](https://private-user-images.githubusercontent.com/37873745/294755872-73c0dbc5-9607-402c-bbc5-f7184a77ae33.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MDQ2Mjg2NTMsIm5iZiI6MTcwNDYyODM1MywicGF0aCI6Ii8zNzg3Mzc0NS8yOTQ3NTU4NzItNzNjMGRiYzUtOTYwNy00MDJjLWJiYzUtZjcxODRhNzdhZTMzLnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNDAxMDclMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjQwMTA3VDExNTIzM1omWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPWRkYjFkM2FhNjY3M2I2YmUzYjQwZDI4YzliNmRjZjA0NGVmNWZjMDBlNGNkZDBkMTI0OTkzMjE5OWY1MDM4OGMmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0JmFjdG9yX2lkPTAma2V5X2lkPTAmcmVwb19pZD0wIn0.d_2JER_2Kwg2RXYWu3dG4xzzFwAiQMRXunV-TNQQJ8M)


# 컨테이너 런타임 종류: 어떤 컨테이너 런타임이 좋아요?

OCI, CRI를 통해 컨테이너 관련 기능에서는 맞춤형 애플리케이션 플랫폼이 될 수 있게 되었다.
즉, 컨테이너 런타임을 내 맛대로 골라 사용해도 기능 동작에는 문제가 없을 것이라는 말이다.

그럼 어떠한 컨테이너 런타임을 사용하는 것이 가장 좋을까?
일단 CNCF 산하 [Container Runtime 프로젝트](https://landscape.cncf.io/card-mode?category=container-runtime&grouping=category)를 살펴보자.
![](https://private-user-images.githubusercontent.com/37873745/294755873-bb92c43b-d02f-40a4-b53e-823f1156b14d.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MDQ2Mjg2NTMsIm5iZiI6MTcwNDYyODM1MywicGF0aCI6Ii8zNzg3Mzc0NS8yOTQ3NTU4NzMtYmI5MmM0M2ItZDAyZi00MGE0LWI1M2UtODIzZjExNTZiMTRkLnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNDAxMDclMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjQwMTA3VDExNTIzM1omWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPTAzMzNhOTgzOTM3ZTMxYWZjMWQwNzJjMzc2ODJjNGQ3M2NlOTQ0YzAzZWViMjViYjdjMDQyNDFhMTE4M2U5ZGMmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0JmFjdG9yX2lkPTAma2V5X2lkPTAmcmVwb19pZD0wIn0.BfK-CffjohrQxp_ja7ssNGxphnYnzo-xtOpcbARs2M0)
- ~~여기서 그냥 큰 사각형 놈들로 고르면 된다~~


컨테이너 런타임도 종류가 엄청 많은 것을 볼 수 있다.
- 각자 내부적으로 동작하는 방식이 다르며, 각자의 특장점이 존재한다.
	- ex) runc/crun/runhcs? 보안? 언어(Go? Rust?)? 특수상황 고려(격리환경, 익숙한 CLI 필요)? 등
- 그렇기에 현재 상황을 보고 제일 적절한 것을 고르는게 좋다. ~~답은 너에게 있다~~


사실 일반적으로는 정말 저기 큰 사각형인 `containerd`, `cri-o` 그리고 `docker`를 사용하는 편이 좋다.
- 이들이 가장 많이 사용하는 구현체이므로 관련 정보들도 많고, 가장 안전한 방법이라고 생각한다.
- 작년, 데이터독에서 조사한 Container runtime 채택률을 참고하자 ([링크](https://www.datadoghq.com/container-report/))
![](https://private-user-images.githubusercontent.com/37873745/294755874-8c06044b-d5f9-4760-b59c-aa6bac4a5484.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MDQ2Mjg2NTMsIm5iZiI6MTcwNDYyODM1MywicGF0aCI6Ii8zNzg3Mzc0NS8yOTQ3NTU4NzQtOGMwNjA0NGItZDVmOS00NzYwLWI1OWMtYWE2YmFjNGE1NDg0LnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNDAxMDclMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjQwMTA3VDExNTIzM1omWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPTIxNWYxNmE0NDRlZTVmY2MxNzJjYmFkZDc1ZjczOTMyOTQ5M2UxNzcwNzJiOGVkNGE4OTc0NjhmNmU5Y2IxYjEmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0JmFjdG9yX2lkPTAma2V5X2lkPTAmcmVwb19pZD0wIn0.9N3pl7E2MU1O87h1ciQ2jRJPySRGzaqGn_a-GBR0N9g)


"아 그럼 나는 docker를 사용할래. 그래프를 보니 제일 많이 사용하기도 하고 개발할 때 경험한 것도 있으니 제일 익숙해~!"
- 는 좋지 않은 생각이다. 
- Docker는 엔진 구조 상 쿠버네티스에 필요하지 않은 기능들도 가지고 있는 모둠 선물 보따리 같은 녀석이라 무겁다. 그래서 쿠버네티스에서도 사용을 권장하지 않는다.
	- 도커의 엔진구조. 실제로 컨테이너 런타임으로서 필요한 부분은 빨간 부분만 해당된다.
		- ![](https://private-user-images.githubusercontent.com/37873745/294755879-c8986b64-45e9-4d25-8153-920b109b0498.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MDQ2Mjg2NTMsIm5iZiI6MTcwNDYyODM1MywicGF0aCI6Ii8zNzg3Mzc0NS8yOTQ3NTU4NzktYzg5ODZiNjQtNDVlOS00ZDI1LTgxNTMtOTIwYjEwOWIwNDk4LnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNDAxMDclMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjQwMTA3VDExNTIzM1omWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPWNmMTIzMGQ3NTBmZDBkZjYyMzkxZjdjNzg0MDM1ZjhjYjFlNGE2NmVjZmVkYTZkOWZhZWRiYjQyZjk4NWFkMzYmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0JmFjdG9yX2lkPTAma2V5X2lkPTAmcmVwb19pZD0wIn0.vbDnjJVktvusynYDgOHHC6cWSw90eQHAHu_DOYcFlMw)
- 실제로 docker -> containerd로 넘어가며 여러 성능 메트릭이 좋아진 사례가 많다 ([참고](https://www.youtube.com/watch?v=uDOu6rK4yOk))
	- ![](https://private-user-images.githubusercontent.com/37873745/294755876-96d52469-1fbd-4fe1-8c48-43db35abd01d.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MDQ2Mjg2NTMsIm5iZiI6MTcwNDYyODM1MywicGF0aCI6Ii8zNzg3Mzc0NS8yOTQ3NTU4NzYtOTZkNTI0NjktMWZiZC00ZmUxLThjNDgtNDNkYjM1YWJkMDFkLnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNDAxMDclMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjQwMTA3VDExNTIzM1omWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPTc3YTNkMTEwNzRlNjk1ZTY5ZmMyYTNiZDcwZTc3NjFkZTgzY2Y0NDA4MGM1MGU0OWUwZDY5ZGM0NmZiMzE2N2MmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0JmFjdG9yX2lkPTAma2V5X2lkPTAmcmVwb19pZD0wIn0.yfYA5OjnLaRVzzys2ffS1DV0_tdgiMbplM_bdW1Z4dc)
- 참고) 쿠버네티스 1.24 버전 릴리즈가 되면서 dockershim이라는 docker를 CRI로 이용할 수 있게 해주는 모듈이 쿠버네티스에서 제거되었다 (정확히 말하면 kubelet 컴포넌트에서 제거). 그래서 아예 사용을 못하는 줄 아는 사람들이 있는데, 사실이 아니다. 
	- 대신 [`cri-dockered`](https://github.com/Mirantis/cri-dockerd) 어댑터를 사용하면 된다 ([참고](https://kubernetes.io/blog/2022/02/17/dockershim-faq/#can-i-still-use-docker-engine-as-my-container-runtime)). 하지만 위에서 말했듯 성능상 좋지도 않고, 지원도 줄어들고 있는 상황이여서 사용할 필요는 없어 보인다.
	- 또한, docker를 사용했어도 내부적으론 containerd를 사용하고 있어서 기능 내부적으로 차이는 없다고 봐도 무방하다. 대신 docker cli 등의 기능은 사용하지 못하게 된다.


# 결론

**이제는 아래의 질문에 답할 수 있다.**
- 컨테이너 이미지가 무엇이고, 그 표준이 무엇인지?
- 표준은 누가 정하고, 어떻게 구성되는지?
- 컨테이너 런타임이란 무엇이고, 어떤 종류가 있는지?
- 쿠버네티스에서 컨테이너를 누가 다루는지? 어떻게 다루고 있는지?
- 쿠버네티스에 컨테이너를 직접적으로 제어하는 코드가 왜 없는지? 없어도 문제가 없는지?

컨테이너 표준과 컨테이너 런타임들은 계속해서 개선되어지고 있고 발전해나가고 있다.

쿠버네티스를 사용함에 있어 제일 핵심적인 요소로서 그 내부를 자세히 알지는 못해도 되지만 어떻게 활용되고, 어떤 것들이 있는지? 또 최근 트렌드는 어떤지? 같은 것들을 알아두면 운영에 많은 도움이 될 것이라고 생각한다.