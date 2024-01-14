> [!NOTE]
> 책 내용 + 기타 레퍼런스를 보완한 요약본입니다.

# Chapter 4] 컨테이너 스토리지

## Summary

### 스토리지 고려사항

- 액세스 모드
    - 애플리케이션이 데이터를 스토리지와 어떻게 Interaction할 것인지?
    - RWO (ReadWriteOnce)
        - 한 번에 한 Node에서만 읽고 쓸 수 있다. (해당 Volume이 mount된 Node에서만 작업 가능)
        - 주로 단일 Node에 특정 Pod가 Binding된 상황에서 사용
        - e.g. MySQL 데이터베이스
    ```yaml
    apiVersion: v1
    kind: Pod
    metadata:
      name: rwo-example
    spec:
      containers:
      - name: my-container
        image: nginx:latest
        volumeMounts:
        - name: my-volume
          mountPath: /data
      volumes:
      - name: my-volume
        persistentVolumeClaim:
          claimName: my-pvc
    ---
    apiVersion: v1
    kind: PersistentVolumeClaim
    metadata:
      name: my-pvc
    spec:
      accessModes:
      - ReadWriteOnce
      resources:
        requests:
          storage: 1Gi

    ```
    - ROX (ReadOnlyMany)
        - 여러 Node에서 **읽기**만 가능
        - 여러 Node가 데이터를 공유해야 하는 Read-only case에 사용
        - e.g. 웹 서버 Static 컨텐츠 저장 용도의 NFS 서버
    - RWX (ReadWriteMany)
        - 여러 Node에서 읽고 쓰는 경우로 여러 Node 간 데이터 공유가 필요한 case에 사용
        - e.g. 공유 파일 업로드 / 다운로드 용도의 NFS 서버
- 볼륨 확장
    - PVC 크기 조절
    - 신규 PVC 생성
- 볼륨 프로비저닝
    - 정적
        - 클러스터 관리자가 수동으로 PV 생성, 개발자는 PVC를 통해 요구사항과 일치하는 사용 가능한 PV에 Binding
    - 동적
        - PVC에 대한 PV 생성 과정을 자동화
        - 클러스터 관리자가 PV 생성 템플릿 StorageClass를 정의
            - StorageClass에서는 PV 생성 책임이 있는 Provisioner를 지정
            - 개발자가 StorageClass를 참조하는 PVC를 생성하면 Provisioner가 PVC 요구사항에 일치하는 PV를 자동으로 생성
        - **클러스터 관리자의 개입이 최소화되므로 가능한 한 동적 프로비저닝이 선호된다**
- 백업 및 복구
    - [Velero](https://github.com/vmware-tanzu/velero)
- 블록 / 파일 / 오브젝트 스토리지
    - 블록 스토리지는 데이터를 고정 크기 블록으로 나누어 저장하는 방식으로, 빠른 입출력 속도를 제공 (e.g. SAN)
    - 파일 스토리지는 데이터를 파일 형태로 저장하고 디렉터리 구조를 통해 관리하는 방식으로, 사용자가 각 파일을 이름으로 직접 접근 가능 (e.g. NAS)
    - 오브젝트 스토리지는 데이터를 객체 단위로 저장하며 각 객체는 고유 ID로 식별하는 방식으로, 이 ID를 통해 데이터 검색 및 접근이 가능하고 대용량 비정형 데이터를 처리하는 데 적합
    - PV의 `volumeMode`는 `Block`과 `Filesystem` 두 가지로 분류되고 일반적으로는 `Filesystem`임. S3의 경우에도 `Filesystem` 사용
- 임시 데이터
    - `emptyDir`
      - Pod 내 임시 데이터를 저장하기 위한 빈 디렉터리로 Pod 실행되는 동안에만 존재하고, 삭제될 경우 함께 제거됨
      - 컨테이너 간 데이터 공유 가능
    ```yaml
    apiVersion: v1
    kind: Pod
    metadata:
      name: test-pod
    spec:
      containers:
      - name: test-container
        image: k8s.gcr.io/busybox
        volumeMounts:
        - name: cache-volume
          mountPath: /cache
      volumes:
      - name: cache-volume
        emptyDir: {}
    ```
    - `hostPath`
      - Host Node 파일이나 디렉터리를 Pod에 Mount해서 Volume으로 사용하며, Node 상 다른 Pod와 Volume 공유
      - 보안 유의 (무분별한 공유 자제, ReadOnly 권장)
      - Pod가 제거되어도 Volume이 죽는 것은 아님
      - Pod가 다른 Node에 배포되는 경우 의도하는 Volume을 사용할 수 없음
    ```yaml
    apiVersion: v1
    kind: Pod
    metadata:
      name: hostpath-example
    spec:
      containers:
      - name: my-container
        image: nginx:latest
        volumeMounts:
        - name: hostpath-volume
          mountPath: /usr/share/nginx/html
      volumes:
      - name: hostpath-volume
        hostPath:
          path: /path/on/host
          type: Directory
    ```
- Storage Provider 선택

### PV / PVC

```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: pv-example
spec:
  capacity:
    storage: 10Gi
  volumeMode: Filesystem
  accessModes:
    - ReadWriteOnce
  persistentVolumeReclaimPolicy: Retain
  storageClassName: standard
  mountOptions:
    - hard
    - nfsvers=4.1
  nfs:
    path: /path/to/storage
    server: nfs-server.example.com
```

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: example-pvc
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 1Gi
```

### StorageClass

```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: example-storageclass
provisioner: kubernetes.io/aws-ebs
parameters:
  type: gp2
  fsType: ext4
```

### CSI (Container Storage Interface)

- v1.9부터 추가된 외부 스토리지 시스템과 통합하기 위한 표준 인터페이스

![CSI Architecture](/assets/11.png)
> Image From : [OpenShift](https://access.redhat.com/documentation/ko-kr/openshift_container_platform/4.13/html/storage/using-container-storage-interface-csi)

- 구성 요소
  - CSI Driver
    - CSI Spec 구현체로 CSI Provider가 이 Driver를 제공
    - Driver 컨테이너와 `node-driver-registrar` / `liveness-probe` 컨테이너로 구성되고 registrar가 `kubelet`에 드라이버를 등록 요청한다. `liveness-probe`는 드라이버 상태를 주기적으로 모니터링한다.
  - CSI Provider
  - CSI Controller
    - Storage Provisioning과 관리를 담당
    - Driver 컨테이너와 아래 컴포넌트들의 사이드카 컨테이너로 구성
      - Provisioning (`external-provisioner`)
        - PVC Object를 모니터링하고 PV 생성
      - Attachment (`external-attacher`)
        - PVC가 생성되면 연계된 컨테이너에 PV를 Mount (`VolumeAttachment` Object 모니터링)
      - Snapshot (`external-snapshotter`)
        - 볼륨의 스냅샷을 담당하며 `VolumeSnapshot` / `VolumeSnapshotContent` Object를 모니터링한다.
      - Scalability (`external-resizer`)
        - PVC의 용량 변경을 모니터링한다.
      - Volume Lifecycle Management
      - Topology Management
  - Kubelet Plugin

## Takeaway

- `ephemeral-storage`는 Pod가 Node 스토리지를 사용하는 부분에 대한 리소스 제한만을 정의하는 것
  - `stdout` 로그, `emptyDir` 데이터 (`medium` (RAM이 아닌)), 컨테이너의 쓰기 가능한 레이어 (영구 볼륨 / `hostPath` / NFS 등이 마운트된 영역을 제외한 모든 영역)에 기록된 데이터의 총합으로 계산