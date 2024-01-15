4장 - 스토리지

# 스토리지 고려사항


스토리지를 제공해야 한다면, 인프라 및 애플리케이션에서 다음 사항들의 지원을 확인 및 분석 해봐야 함.
- 액세스 모드
- 볼륨확장
- 볼륨 프로비저닝
- 백업 및 복구
- 블록 디바이스 및 파일, 오브젝트 스토리지
- 임시데이터
- 스토리지 프로바이더 선택

	
쿠버네티스에서는 어떻게 이를 구현하고, 제공하고 있는지 알아보자.

## 액세스 모드

애플리케이션을 지원할 수 있는 3가지의 액세스 모드를 제공한다.
- RWO: Read Write Once
	- 단일 파드가 볼륨을 읽고 쓸 수 있다.
- ROX: Read Only Many
	- 여러 파드가 볼륨을 읽을 수 있다.
- RWX: Read Write Many
	- 여러 파드가 볼륨을 읽고 쓸 수 있다.

Cloud native 애플리케이션에서는 `RWO`가 가장 일반적인 패턴
- Amazon EBS, Azure Disk Storage 같은 프로바이더에서도 `RWO`로 제한하고 하나의 노드에만 연결된다.

Legacy 애플리케이션에서는 `RWX` 지원이 필요할 수 있다.
- 이를 위해 Network FileSystem (NFS) 액세스 등을 이용하도록 구축된다.
- 근데 보통 NFS로 서비스하는 것보다 MQ/DB 등과 같이 API를 통해 데이터를 공유하는 것이 좋다.
- 그래도 필요하다면, 보통 Amazon Elastic File System, Azure File Share 등을 이용한다.


## 볼륨 확장

데이터가 쌓임에 따라 더 큰 볼륨이 필요한 상황이 온다.
새로운 볼륨으로 마이그레이션을 할 수도 있지만 **볼륨 확장** 기능을 제공한다.

1. PVC를 통해 쿠버네티스에 추가 스토리지를 요청
2. 스토리지 프로바이더(Storage Class)를 통해 볼륨 크기 확장
3. 더 큰 볼륨을 사용하도록 파일 시스템 확장


## 볼륨 프로비저닝

**정적 프로비저닝**
- PV yaml -> PV 생성
- PVC yaml -> PVC 생성
	- PV와 바인딩
- Pod yaml -> Pod 생성
	- Pod에 PVC/볼륨 마운트


**동적 프로비저닝**
정적 프로비저닝은 그때마다 PV 만들어줘야해서 너무 귀찮아. 관리자들이 힘들지 않게 자동화 할 수 없을까? => 동적 프로비저닝
- StorageClass 생성 -> add on? / Cloud provider
- PVC yaml -> PVC, PV 생성
	- PVC manifest에 Storage class 명시해야 함
	- PV 자동생성 및 PVC/PV 바인딩
- Pod yaml -> Pod 생성
	- Pod에 PVC/볼륨 마운트


**동적할당 원리**

![](https://private-user-images.githubusercontent.com/37873745/296548665-3fef8693-0df5-4f45-9e76-ec00f55eb117.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MDUyNDAxODksIm5iZiI6MTcwNTIzOTg4OSwicGF0aCI6Ii8zNzg3Mzc0NS8yOTY1NDg2NjUtM2ZlZjg2OTMtMGRmNS00ZjQ1LTllNzYtZWMwMGY1NWViMTE3LnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNDAxMTQlMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjQwMTE0VDEzNDQ0OVomWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPTA0MjUyNGI0OGE3ZWQ5NDU3NzQzYzgyOGIzNGFkOGQ0Y2ZlZGUzZTllZjllN2Y1YWNlNjVjNjYyNjhlYzUzNjgmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0JmFjdG9yX2lkPTAma2V5X2lkPTAmcmVwb19pZD0wIn0.EuA9xHxKlzRQQX6u83VhYKQIwabmsy4795lHU9Q1tko)

- 사용자가 요청한 PVC의 요구 사항이 인프라 관리자가 미리 생성한 PV 중 어떠한 PV와도 일치하지 않을 시 K8S Dynamic Provisioning을 시도
	- Storage Class에 정의된 PV 및 물리적 Disk의 특징에 기반해 Dynamic Provisioning이 수행됨
- Storage class는 Dynamic Provisioning에 의해 새롭게 생성될 PV, 물리적 Disk의 특성을 정의하는 일종의 템플릿이라고 보면 됨.
	- 즉, Dynamic Provisioning을 사용하려면 관리자가 사전에 Storage class를 미리 정의해 놓아야 함.


## 백업 및 복구

백업/자동 복원 기능은 스토리지의 가장 복잡한 측면이다.
- 백업 전략과 스토리지 시스템의 가용성 보장 간의 균형을 생각해야 함.

쿠버네티스 및 애플리케이션 상태 백업 솔루션으로 많이 알려진 것은 [Velero](https://velero.io/)가 있음.
- 쿠버네티스 오브젝트를 클러스터 간에 마이그레이션하거나 복원/백업 할 수 있음. 스냅숏 관련 기능들도 지원함.
- 백업 및 복구 훅도 지원해서 백업/복구 수행 전 컨테이너에 명령을 실행할 수 있음.
	- 백업 전 트래픽을 중지하거나 flush를 트리거하는 등


## 블록 디바이스 및 파일, 오브젝트 스토리지
파일 스토리지: 가장 일반적인 스토리지 유형
- 파일 시스템이 맨 위에 있는 블록 디바이스
- 애플리케이션이 블록에 직접 읽고 쓸 수 있도록 제공

나머지 내용은 아래 그림들로 대체.
![](https://private-user-images.githubusercontent.com/37873745/296548669-cbee2167-3cb2-4531-a7b4-1f78b1835ede.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MDUyNDAwMjIsIm5iZiI6MTcwNTIzOTcyMiwicGF0aCI6Ii8zNzg3Mzc0NS8yOTY1NDg2NjktY2JlZTIxNjctM2NiMi00NTMxLWE3YjQtMWY3OGIxODM1ZWRlLnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNDAxMTQlMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjQwMTE0VDEzNDIwMlomWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPWE1MmE3YWViNjYwMjMzOWJhYzBjY2ZlMDI2N2JjYmU3NjMzMWQ0M2JmNTRkNGIwY2QzYThkMTM1OTE3ZWViNzUmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0JmFjdG9yX2lkPTAma2V5X2lkPTAmcmVwb19pZD0wIn0.5isuj_BxLOs6yGxqk88k4Ry2MySe_iS9wCYczGQOLQc)

![](https://private-user-images.githubusercontent.com/37873745/296548670-c2d29baf-f654-4475-9231-82e22c3cde6f.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MDUyNDAwMjIsIm5iZiI6MTcwNTIzOTcyMiwicGF0aCI6Ii8zNzg3Mzc0NS8yOTY1NDg2NzAtYzJkMjliYWYtZjY1NC00NDc1LTkyMzEtODJlMjJjM2NkZTZmLnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNDAxMTQlMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjQwMTE0VDEzNDIwMlomWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPTU5NzY0ZmYwZmZjMDI1NmZiMjRmNzkyZmRiYjM2NzQ4YzJiNjU1ODA0NmQ4NmE3YTU1MTNlNjUyOTYzZWUyZjEmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0JmFjdG9yX2lkPTAma2V5X2lkPTAmcmVwb19pZD0wIn0.GLV963rnxtULCeEUUHN9DAvqeH7zx_B5x6sG3O3sTgA)

![](https://private-user-images.githubusercontent.com/37873745/296548671-210198a3-1414-4a0a-92f7-354295956b07.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MDUyNDAwMjIsIm5iZiI6MTcwNTIzOTcyMiwicGF0aCI6Ii8zNzg3Mzc0NS8yOTY1NDg2NzEtMjEwMTk4YTMtMTQxNC00YTBhLTkyZjctMzU0Mjk1OTU2YjA3LnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNDAxMTQlMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjQwMTE0VDEzNDIwMlomWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPTc4NjhiNmI4OWNmZjcyMDhmNjUwNTRmOTllYTVkZTE4OTNkNGJhMzZiZTA0MGVhZTk1ZTVjOGU3NDhiMDQ1MTUmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0JmFjdG9yX2lkPTAma2V5X2lkPTAmcmVwb19pZD0wIn0.5T7sJRROT4FYUd3aGcFIkT2d5YMbw15NJm77pIeYDdQ)

![](https://private-user-images.githubusercontent.com/37873745/296548675-81c0363d-96b0-4e21-af32-3eaad8a44d66.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MDUyNDAwMjIsIm5iZiI6MTcwNTIzOTcyMiwicGF0aCI6Ii8zNzg3Mzc0NS8yOTY1NDg2NzUtODFjMDM2M2QtOTZiMC00ZTIxLWFmMzItM2VhYWQ4YTQ0ZDY2LnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNDAxMTQlMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjQwMTE0VDEzNDIwMlomWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPTQxNDY1Njc0N2ExYTdkNzU0OGNlMWUxMGNkM2VkNDUzNjVmMzVlY2Q5MmMyZDliZWMxZGNiZmZmYzM0NDdiYzcmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0JmFjdG9yX2lkPTAma2V5X2lkPTAmcmVwb19pZD0wIn0.JblcGrhCT_LXOeeUf68-gyj1NGdBqjOYYHRLF0okUSs)

![](https://private-user-images.githubusercontent.com/37873745/296548676-92701cb9-ef76-41e3-8d0c-9897b5c9ac6c.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MDUyNDAwMjIsIm5iZiI6MTcwNTIzOTcyMiwicGF0aCI6Ii8zNzg3Mzc0NS8yOTY1NDg2NzYtOTI3MDFjYjktZWY3Ni00MWUzLThkMGMtOTg5N2I1YzlhYzZjLnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNDAxMTQlMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjQwMTE0VDEzNDIwMlomWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPTc5Njc0MmExZWIyOWU2ZDlhZDM5ZjAwYTFlZDc1NTJiNzU4MzZlODhhMGM5ZmIwY2Q5NWM2ZjM1NjlkZDBiNTQmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0JmFjdG9yX2lkPTAma2V5X2lkPTAmcmVwb19pZD0wIn0.P7Gu63K_00-VYegISnlazqyJUEaiXqjZaeaP28CofuQ)


## 임시데이터
쿠버네티스에서는 기본적으로 자체 파일시스템에 쓰는 임시 저장소를 사용함
- 파드에 문제가 생겨 재생성되면 데이터가 제거됨 (`emptyDir` 설정)
- 동일한 파드의 컨테이넉 간에 파일을 공유하는데 사용
- 호스트 스토리지 용량을 너무 많이 소비하지 않도록 관리하는게 필요
	- `LimitRange`: PVC 요청 당 min/max 제한
	- `ResourceQuota`: PVC 수 및 누적 스토리지 용량 제한


## 쿠버네티스 스토리지 기본 요소
Persistent Volume, Persistent Volume Claim, Storage Class

![](https://private-user-images.githubusercontent.com/37873745/296548677-29503a9a-12d7-45a4-ba4b-1adb426b0164.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MDUyNDAwMjIsIm5iZiI6MTcwNTIzOTcyMiwicGF0aCI6Ii8zNzg3Mzc0NS8yOTY1NDg2NzctMjk1MDNhOWEtMTJkNy00NWE0LWJhNGItMWFkYjQyNmIwMTY0LnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNDAxMTQlMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjQwMTE0VDEzNDIwMlomWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPWFmMTk5ZmQxMWM5MTAzM2NlZGFkZDE3NTNjNWViMWYzNzc5YmI3YjIxNTI3Y2JjZjFkOTJmYTczNDZhOTFiYmMmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0JmFjdG9yX2lkPTAma2V5X2lkPTAmcmVwb19pZD0wIn0.qeGikxUxcw5hiYjJSpBjJfVByUunS69XK05iaRXwnCc)

![](https://private-user-images.githubusercontent.com/37873745/296548679-8923a7bc-d29b-491b-a374-532d1fa7554a.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MDUyNDAwMjIsIm5iZiI6MTcwNTIzOTcyMiwicGF0aCI6Ii8zNzg3Mzc0NS8yOTY1NDg2NzktODkyM2E3YmMtZDI5Yi00OTFiLWEzNzQtNTMyZDFmYTc1NTRhLnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNDAxMTQlMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjQwMTE0VDEzNDIwMlomWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPWMwZDNmZGZiY2VkZTU1YzM5MzAyNDk0M2U5ZWZlMDZlYmI0ZTRlODhkZGNlM2JhMWM5OGJmNTFlNjhjMzBlZGImWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0JmFjdG9yX2lkPTAma2V5X2lkPTAmcmVwb19pZD0wIn0.z8x29_0HDQL181Bvd3xedZ3TVSN21HGEBCI6KG0IRTY)


StroageClass
- AWS EBS를 사용한다고 해보자.

```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
	name: aws-ebs
	annotations:
		storageclass.kubernetes.io/is-default-class: "true"
provisioner: kubernetes.io/aws-ebs
parameters:
	type: gp2
	fsType: ext4
	encrypted: "true"
	kmsKeyId: <your-key-id> # optional

---

apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: ebs-sc
provisioner: ebs.csi.aws.com
volumeBindingMode: WaitForFirstConsumer
parameters:
  csi.storage.k8s.io/fstype: xfs
  type: io1
  iopsPerGB: "50"
  encrypted: "true"
allowedTopologies:
- matchLabelExpressions:
  - key: topology.ebs.csi.aws.com/zone
    values:
    - us-east-2c
```

지원되는 파라미터 옵션 설정
- https://github.com/kubernetes-sigs/aws-ebs-csi-driver/blob/master/docs/parameters.md
- [EBS Volume Type](https://docs.aws.amazon.com/ko_kr/AWSEC2/latest/UserGuide/ebs-volume-types.html)




## 컨테이너 스토리지 인터페이스: CSI

컨테이너 스토리지 인터페이스
- 워크로드에 블록 및 파일 스토리지를 제공하는 방법
- CSI 구현을 Driver라 하며, 프로바이더와 연동할 수 있다.

Storage class에 명시된 프로비저너가 PVC 상태 모니터링 및 PV 생성을 담당하게 됨.
- 프로비저너는 CSI Driver로 파일 시스템에 따라 다르며 쿠버네티스 인트리에 있는 CSI Driver를 사용하거나 외부의(External) CSI Driver를 사용할 수도 있음.


![](https://private-user-images.githubusercontent.com/37873745/296548681-de13da23-755b-4ae2-a04b-38654fc9f44d.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MDUyNDAwMjIsIm5iZiI6MTcwNTIzOTcyMiwicGF0aCI6Ii8zNzg3Mzc0NS8yOTY1NDg2ODEtZGUxM2RhMjMtNzU1Yi00YWUyLWEwNGItMzg2NTRmYzlmNDRkLnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNDAxMTQlMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjQwMTE0VDEzNDIwMlomWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPThmNmQ1YWVkZWVjNjQxNmRmY2I3OGRjZjdiOGU4Yjk4YjU4MTBlODBiODM1Y2M5MGU0Mjk5N2Y4MTgzNDJmYmImWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0JmFjdG9yX2lkPTAma2V5X2lkPTAmcmVwb19pZD0wIn0.XtgArxaRMU9PUP445EXi5x5On-fojolTEdZ7V6CKSas)

![](https://private-user-images.githubusercontent.com/37873745/296548667-756625dd-a691-4dbb-8398-b4db230e0f30.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MDUyNDAwMjIsIm5iZiI6MTcwNTIzOTcyMiwicGF0aCI6Ii8zNzg3Mzc0NS8yOTY1NDg2NjctNzU2NjI1ZGQtYTY5MS00ZGJiLTgzOTgtYjRkYjIzMGUwZjMwLnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNDAxMTQlMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjQwMTE0VDEzNDIwMlomWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPTFkY2MxY2I5NDAwNWUzNDU0MTA5ZjdiNjc3NzYzY2Q3NTI1OWQ3OWM4MGNhYTMwMzBmMmUwZjA5MDhkNTFkYjEmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0JmFjdG9yX2lkPTAma2V5X2lkPTAmcmVwb19pZD0wIn0.cN03jV2sWBmfXah49sta_NgsFd1K7_9yTbcm1UW9Vg8)

CSI Driver (== Provisoner/프로비저너)
- 구현은 Controller Plugin과 Node Plugin으로 나뉨.
- 이 2개의 플러그인 하나의 바이너리로 묶고 `X_CSI_MODE`와 같은 환경변수를 통해 하나를 활성화
- Driver는 kubelet에 등록되고, CSI 명세는 엔드포인트에서 개발됨.

**CSI Controller**
- **볼륨 생성 및 삭제, 스냅숏 생성 및 볼륨 확장 등 담당**
- Persistent Storage system에서 볼륨을 관리하기 위한 API를 활용.
- 쿠버네티스 컨트롤 플레인은 직접적으로 CSI Controller와 소통하지 않음. 쿠버네티스 이벤트에 실행되고 새로운 PersistentVolumeClaim이 생성될 때 CSI 명령으로 변환.
- 사이드카 형태로 배포되며 4가지의 외부 컨트롤러를 가짐
	- Provisioner, Attacher, Resizer, Snapshotter

**CSI Node**
- 일반적으로 컨트롤러 플러그인과 같은 코드를 실행하지만, 모드를 '노드 모드'로 실행함.
- **연결된 볼륨 마운트, 파일시스템 설정 및 파드 볼륨 마운트 등 담당**
- 이런 작업은 kubelet에 의해 수행됨.



설명?
- Provisioner, Attacher, Controller, NodeServer로 이루어짐.
- 프로비저너 Provisioner
	- 클러스터에 PVC가 생성되는 걸 모니터링하고 있다가 PVC가 생성되면 PV 생성하는 걸 담당
- 어태쳐 Attacher
	- 파드가 PVC를 사용하려할 때 해당 컨테이너에 PV 마운트 하는 것을 담당
- 컨트롤러 Controller
	- 쿠버네티스 컨테이너에서 사용할 볼륨을 스토리지 서버에서 생성 및 삭제
- 노드서버 NodeServer
	- 파드가 배포될 노드에서 스토리지 볼륨에 마운트할 수 있게 환경 만드는 것을 담당



## Storage as a Service 구현

![](https://private-user-images.githubusercontent.com/37873745/296548682-5c57b0ac-b999-4afe-ae89-81180f79a85f.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MDUyNDAxODksIm5iZiI6MTcwNTIzOTg4OSwicGF0aCI6Ii8zNzg3Mzc0NS8yOTY1NDg2ODItNWM1N2IwYWMtYjk5OS00YWZlLWFlODktODExODBmNzlhODVmLnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNDAxMTQlMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjQwMTE0VDEzNDQ0OVomWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPTViYjRkY2QyOTc1ODUwZGFmNWU1NjliN2FkYmE3YzMzMWJmZjlhYmZhOGM2MTlkY2QyZDE3NTExNmE0MDRmNTYmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0JmFjdG9yX2lkPTAma2V5X2lkPTAmcmVwb19pZD0wIn0.JzrVmusDJQQ-CjI4uVErmlt5mHCbKmE__CB-rpo80BE)

![](https://private-user-images.githubusercontent.com/37873745/296548684-247610bf-7b02-4018-8bd4-6ef1b427ff5e.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MDUyNDAxODksIm5iZiI6MTcwNTIzOTg4OSwicGF0aCI6Ii8zNzg3Mzc0NS8yOTY1NDg2ODQtMjQ3NjEwYmYtN2IwMi00MDE4LThiZDQtNmVmMWI0MjdmZjVlLnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNDAxMTQlMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjQwMTE0VDEzNDQ0OVomWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPTE1NmNlOTliMzNiNTQyYmY1NDgwNjVmYTM4MzIwYzgyZjFkOWY0ZjkwMTI0MGYzODRlMDJhNGIzMjhmMTczYTImWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0JmFjdG9yX2lkPTAma2V5X2lkPTAmcmVwb19pZD0wIn0.76vIOK-oHbsRg67h4oIwaGpQawslwzc7MudeTFkkwRc)

AWS 프로바이더는 적절한 액세스 권한 확인을 원함. 3가지 옵션이 있음.
1. 쿠버네티스 노드의 인스턴스 프로파일 업데이트
	- 쿠버네티스 수준에서 자격증명을 걱정할 필요가 없고, AWS API에 대한 보편적인 권한이 제공됨
2. 특정 워크로드에 IAM 권한을 제공할 수 있는 자격 증명 서비스를 도입 ([ref](https://docs.aws.amazon.com/eks/latest/userguide/iam-roles-for-service-accounts.html))
	- 가장 안전하며 많이 활용되는 것으로 보임?
3. CSIDriver에 탑재되는 시크릿에 자격 증명 추가
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: aws-secret
type: Opaque
stringData:
  key: <your-aws-access-key>
  keyid: <your-aws-key-id>
---
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: aws-ebs-csi
provisioner: ebs.csi.aws.com
parameters:
  csi.storage.k8s.io/provisioner-secret-name: aws-secret
  csi.storage.k8s.io/provisioner-secret-namespace: default
```

CSI 컴포넌트 설치
- 먼저컨트롤러가 배포로 설치
- 리더 선택
- 모든 노드에서 파드로 실행하기 위해 데몬셋 형태로 제공되는 노드 플러그인 설치
- kubelet이 CSINode 오브젝트를 생성해 CSI가 활성화 된 노트들 리포팅
	- `kubectl get csinode`


StorageClass 생성
- 상황에 맞는 HDD/SDD 스토리지 옵션 설정해 `StorageClass` manifest 작성 및 생성
- `meta.annotaitons.storageclass.kubernetes.io/is-default-class`: 스토리지 클래스 정의 지정안하고 PVC 생성 시, 기본 블록으로 사용함
- `volumeBindingMode`: `WaitForFirstConsumer` 로 지정 시, PVC 사용할 때까지 PV 프로비저닝을 하지 않음. 요금 청구 방지.

Snapshot
- 볼륨 데이터의 주기적인 백업을 위해 Snapshot 기능도 제공함.
	- PV와 마찬가지로 CSI Driver 측에서 제공해야 함.
- 2가지 CRD로 제공
	- `kubectl api-resources | grep volumesnapshot`
- StroageClass - PV/PVC 관계와 유사함
	- `VolumeSnapshotClass` - `VolumeSnapshot`
		- `VolumeSnapshotContent` 생성
	- `VolumeSnapshotContent`: 클러스터 내 볼륨의 스냅샷. 클러스터 리소스.
	- `VolumeSnapshot`: 볼륨의 스냅샷 요청. PVC와 유사.
- 
- 


# 덤 / 주의

## 그럼 PVC가 제거되면 PV도 제거 되는가?

**PV Reclaim policy**
PV Status가 Bound -> Released되었을 때, 저장된 데이터와 PV를 어떻게 처리할 것인가에 대한 정책

**즉 PVC가 삭제되었을 때, 바인딩 되었던 PV를 어떻게 처리할 것인가?**에 대한 정책

![](https://private-user-images.githubusercontent.com/37873745/296548660-6c642e96-6bea-43d9-88a0-ca63f0e11f08.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MDUyNDAxODksIm5iZiI6MTcwNTIzOTg4OSwicGF0aCI6Ii8zNzg3Mzc0NS8yOTY1NDg2NjAtNmM2NDJlOTYtNmJlYS00M2Q5LTg4YTAtY2E2M2YwZTExZjA4LnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNDAxMTQlMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjQwMTE0VDEzNDQ0OVomWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPTNiMzI1NDc1MmFhY2U0MTBjMzM2ZjMwZjkxMzQ3ZTVhZDVmMzVmNTU1NTdkNWNjNzdlMzQ3MWIyODMxOGVmODImWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0JmFjdG9yX2lkPTAma2V5X2lkPTAmcmVwb19pZD0wIn0.Nv226SY1iMYg9OMoeDIlOW2QvbpIQ_lRECruAimatSo)

- `Retain`: 삭제되지 않는다. PVC는 삭제되어도 PV는 유지 (default)
- `Recycle`: data 삭제
- `Delete`: 볼륨 삭제 및 data 삭제

## 볼륨 이용 시, 특이사항
- 볼륨은 다른 볼륨 내 마운트 될 수 없음.
- 볼륨은 다른 볼륨의 내용을 가리키는 하드링크를 만들 수 없음.
	- ![](https://private-user-images.githubusercontent.com/37873745/296548666-3f5a9a8d-092a-462a-9bfc-9a38f24a3d37.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MDUyNDAxODksIm5iZiI6MTcwNTIzOTg4OSwicGF0aCI6Ii8zNzg3Mzc0NS8yOTY1NDg2NjYtM2Y1YTlhOGQtMDkyYS00NjJhLTliZmMtOWEzOGYyNGEzZDM3LnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNDAxMTQlMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjQwMTE0VDEzNDQ0OVomWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPTc3ODhmM2NhNTMwMDdmMDY1MTAyZTc4NmNiOWRkM2RhOGRjYzk4YTliZmY2NzQwMTAzZDI2ZjhhMWMxOTg1ZmMmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0JmFjdG9yX2lkPTAma2V5X2lkPTAmcmVwb19pZD0wIn0.1fo9wfK0UD2__m_luU3tLNPHODUsa85XA4RnBYLX2fU)

	- 하드링크? 같은 i-node를 가리키는 파일 => [Ref](https://6kkki.tistory.com/10)

### 쿠버네티스에서 자체적으로 PVC의 다운 사이징을 지원하지 않음
- 그래서 여러가지 방법으로 줄이는 방식/프로세스가 있는 듯 ([Ref](https://blog.srcinnovations.com.au/2023/11/29/changing-kubernetes-pvc-storage-class-and-downsizing-them-at-the-same-time/))
- 볼륨을 늘릴 때 이 점을 염두에 두어야 함.

### 쿠버네티스 기본 한도
- 쿠버네티스 스케줄러에는 노드에 연결될 수 있는 볼륨 수에 대한 기본 한도가 있다.

|클라우드 서비스|노드 당 최대 볼륨|
|---|---|
|[Amazon Elastic Block Store (EBS)](https://aws.amazon.com/ebs/)|39|
|[Google Persistent Disk](https://cloud.google.com/persistent-disk/)|16|
|[Microsoft Azure Disk Storage](https://azure.microsoft.com/ko-kr/services/storage/main-disks/)|16|



## FUSE란 무엇인가?
- Filesystem in USEr space
- 컴퓨터에 OS와 같은 권한이 아닌 사용자가 커널 코드를 편집하지 않고도 자신의 파일 시스템을 만들 수 있음.
	- 즉, 유저 레벨에서 파일 시스템을 만들고, 해당 파일 시스템에서 발생하는 이벤트를 다룰 수 있다.
	- 파일 시스템에 이벤트(open, read, write, close)가 발생했을 때 사용자의 다른 이벤트로 우회시켜 처리할 때 유용하다
- http://taewan.kim/cloud/mounting_oci_objectstorage_bucket_on_linux_mac/
	- ![](https://private-user-images.githubusercontent.com/37873745/296548687-2b4966b4-9a48-402a-b2e6-69b82f3f8a02.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MDUyNDAxODksIm5iZiI6MTcwNTIzOTg4OSwicGF0aCI6Ii8zNzg3Mzc0NS8yOTY1NDg2ODctMmI0OTY2YjQtOWE0OC00MDJhLWIyZTYtNjliODJmM2Y4YTAyLnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNDAxMTQlMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjQwMTE0VDEzNDQ0OVomWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPTZhZWUwOThkMmMyMmFlM2ExNWM1N2MxZDdjYTkyZjBlZWUyYjU2MWZlYmUwODFlMGJhNjMyNTYzMTZkNjQ1ZDcmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0JmFjdG9yX2lkPTAma2V5X2lkPTAmcmVwb19pZD0wIn0.41kgduGZZvaUIvjzsjeHvgAArq_OSjYDTPkSmUaimeo)
	- 파일시스템을 개발하기 위해서는 리눅스 커널 개발이 필요합니다. 리눅스 커널을 직접 편집하는 파일 시스템 개발은 매우 복잡합니다. FUSE를 이용하면 사용자 커널 코드를 편집하지 않고 새로운 파일 시스템을 만들수 있습니다. 물론 FUSE 모듈의 편리한 개발 댓가로 20-30% 성능 저하가 발생합니다. 
	- FUSE는 **Filesystem in USEr space**의 약자입니다. FUSE 모듈은 파일 시스템을 구현한 코드가 User Space(유저 스페이즈)에서 실행될 수 있도록 커널 인터페이스와 User Space를 연결하는 브릿지 역할을 담당합니다.FUSE는 리눅스를 비롯해, Mac OS X, 윈도우즈, 솔라리스에서 사용 가능합니다.

https://linuxism.ustd.ip.or.kr/948
https://yjaeseok.tistory.com/300

## iSCSI란 무엇일까?
- https://lilo.tistory.com/61
- Internet Small Computer System Interface
- IP 기반으로 블록 디바이스를 공유
-  규모가 작은 기업 환경에서는 서버용 디스크에 IP를 이용해 SCSI 저장소를 공유하는 iSCSI가 효율적


## S3으로는 Volume을 사용할 수 없을까? 
=> [S3fs](https://github.com/s3fs-fuse/s3fs-fuse)를 이용하면 되긴 됨
- [s3fs 를 사용하여 EC2에 S3 Mount 어떻게 하나요?](https://support.bespinglobal.com/ko/support/solutions/articles/73000615595--aws-s3fs-%EB%A5%BC-%EC%82%AC%EC%9A%A9%ED%95%98%EC%97%AC-ec2%EC%97%90-s3-mount-%EC%96%B4%EB%96%BB%EA%B2%8C-%ED%95%98%EB%82%98%EC%9A%94-)
- https://bluese05.tistory.com/22
	- public network여서 느린 것도 있지만, FUSE 기반이다보니 kernel level에서 처리되는 file system보다 성능이 떨어질 수밖에 없다.
	- IO가 빈번한 환경인 경우, mount가 떨어져 나가는 현상도 경험할 수 있으니 이런 상황에서는 사용을 포기하는게 건강에 좋다.
- http://taewan.kim/cloud/mounting_oci_objectstorage_bucket_on_linux_mac

s3fs보다 개선된 goofys를 이용해보자.
- https://dev.to/otomato_io/mount-s3-objects-to-kubernetes-pods-12f5
	- Mount S3 Objects to Kubernetes Pods
	- goofys를 이용한다. -> s3fs보다 훨씬 빠르다
		- https://bluese05.tistory.com/23
	- goofys를 이용해 pod 연결 코드
		- https://kyle79.tistory.com/276

- 근데 보통 s3를 이용해 fs를 구성하지 말라고 한다. 목적도 다름. 성능이 좋지도 않음
	- https://stackoverflow.com/a/51677039
	- https://stackoverflow.com/a/19477160


### 프로젝티드 볼륨 (Projected Volumes)
https://kubernetes.io/ko/docs/concepts/storage/projected-volumes/

여러 기존 볼륨 소스(sources)를 동일한 디렉토리에 매핑.
- 아래와 같은 볼륨 유형 소스를 프로젝트(project)할 수 있음
- [`시크릿(secret)`](https://kubernetes.io/ko/docs/concepts/storage/volumes/#secret)
- [`downwardAPI`](https://kubernetes.io/ko/docs/concepts/storage/volumes/#downwardapi)
- [`컨피그맵(configMap)`](https://kubernetes.io/ko/docs/concepts/storage/volumes/#configmap)
- [`서비스어카운트토큰(serviceAccountToken)`](https://kubernetes.io/ko/docs/concepts/storage/projected-volumes/#serviceaccounttoken)


특이사항
- 모든 소스는 파드와 같은 네임스페이스에 있어야 한다.

