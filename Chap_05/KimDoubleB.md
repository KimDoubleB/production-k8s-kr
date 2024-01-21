# 개요

**쿠버네티스 네트워크**
- 쿠버네티스에서 제일 복잡하게 다가오면서도, 제일 흥미롭게 다가오는 부분.
- 어떻게 서버간 소통할 것인가? 이걸 어떻게 자동화했는가?
- 네트워크를 소프트웨어로 (거의) 자유롭게 컨트롤 할 수 있다.
	- 하지만 Trade off가 있는 법. 불필요한 홉, SPOF 문제 등이 발생.

<br/>

**책을 읽기 전 궁금점**
- 쿠버네티스는 컨테이너 오케스트레이션, 플랫폼. 이미 네트워크가 다 구성되어져 있는 것 아닌가? 어떻게 커스텀이 가능한 것인가?
- IPv6 지원?
- 우리가 보통 사용하는 CNI는 무엇인가? 
- eBPF에 대한 자세한 내용?

<br/>

책 자체의 순서는 이상하다.
- 네트워크 상에서 고려해야 될 사항들이 나온 이후에 CNI가 나오는데, 고려사항에서 계속 CNI가 등장하여 이해 하기가 어렵다.
- ~~번역이 거지같다~~

<br/>

# 네트워크 고려사항

파드 네트워크를 구현하기 위해 고려해야 할 사항은 다음과 같다.

<br/>

**IPAM (IP Adress Management)**
- IP 주소를 관리하는 주체. IP를 어떤 범위로 구성하고, 사용할 것인가?
- 파드는 일시적인 것. 많이 생길수도 있고, 없어질 수도 있다. IP 할당을 빠르게 실행하고, IP Pool을 효율적으로 관리해야 함.
- 뒤에서 이야기 할 CNI 플러그인에서 이를 구현한다. Pod network의 CIDR을 지정해 사용하게 된다.

<br/>

**라우팅 프로토콜**
- 쿠버네티스에서는 파드/서비스 등 애플리케이션이 지속적으로 추가/제거 배포되기 때문에 라우팅 프로토콜을 이용해 동적 라우팅을 사용한다.
- 이러한 라우팅 프로토콜도 쿠버네티스에서 직접 구현하는 것이 아니고 CNI에서 담당하게 된다. 즉, 쿠버네티스 자체는 라우팅 하는 방법을 알지 못한다.
- 워크로드 경로 분산 시 일반적으로 가장 많이 사용되는 BGP(Boarder Gateway Protocol). Calico, Kube-Router 등 많은 프로젝트에서 이를 활용한다.
	- 네트워크 경로를 찾고, 전파하는 방법. Reachability information과 Policy를 기반으로 제일 좋은 경로를 찾아내는 것.
	- Calico에서는 커널 라우팅 테이블을 수정하며 설정함.

<br/>

**캡슐화 및 터널링**
- 클러스터 내 네트워크를 외부로 부터 감추는 것.
	- 캡슐화를 통해 터널링을 구현한다고 이해하면 된다.
	- 이를 통해 오버레이 네트워크를 구현할 수 있다.
- 캡슐화는 기존 네트워크 패킷을 한단계 더 래핑하여 클러스터 내 네트워크 통신을 위한 패킷으로 만드는 것을 의미한다.
	- 내부 패킷 IP은 파드 IP를 참조하는 반면, 외부 패킷 IP는 호스트/노드 IP를 참조한다.
- VXLAN, Geneve, GRE 같은 터널링 프로토콜들이 존재한다.
	- VXLAN은 클러스터 내 전체 이더넷 프레임을 UDP 패킷 내에 넣어 활용하는 방식.
	- ![d](../assets/Pasted%20image%2020240121193321.png)
- 단점도 존재함
	- 전체적인 트래픽을 보기 어렵고, 이해하기 어렵다.
	- 캡슐화 하고, 디캡슐화 하는 비용이 든다.
	- 패킷의 크기가 커진다 (위 그림처럼).

<br/>

**IPv4, IPv6**
- IPv6에 대한 지원 (하위 항목들은 GA 기준)
	- k8s 1.0에서는 오직 IPv4만 지원했음
	- k8s 1.18에서 IPv6 지원이 생겨났음. 대신 IPv4 사용할지, IPv6 사용할지 결정했어야 했음.
	- k8s 1.20에서야 IPv4/IPv6를 같은 클러스터 내에서 활용할 수 있게 되었음. 어떤 서비스는 IPv4를 이용하고, 어떤 서비스는 IPv6를 이용함.
	- k8s 1.23에서는 IPv4/IPv6 듀얼스택이 GA 되었음.
- IPv6의 중요성은 커져가고 있다.
	- IPv4는 계속 부족해지기 있기 때문에 IPv6 사용 가속화가 진행되고 있다.
	- Private network라 문제가 없을 수 있지만, 외부 전체 네트워크가 IPv6화 되간다면, 똑같이 영향 받지 않을까? (이점이 더 크다)
	- AWS 같은 경우에도 [IPv6 채택 가속화를 위해 IPv4 유료화를 선언](https://aws.amazon.com/ko/blogs/korea/new-aws-public-ipv4-address-charge-public-ip-insights/)했다.
- IPAM을 CNI가 담당하니 CNI에서도 이를 지원해야 사용이 가능하다. Calico, Cilium 같은 많이 사용하는 CNI Plugin에서는 다 지원한다.

<br/>

**암호화된 워크로드 트래픽**
- 클러스터 내부의 파드 간 트래픽에서는 보통 암호화 되지 않는다. HTTPS로 외부 Gateway를 통해 데이터가 들어오면, 내부에선 HTTP로 통신하는 경우 (SSL offloading 같이).
- 싱글 클러스터라고 하면 안전할 수 있지만, 멀티 클러스터를 구성하고 외부망을 타게 된다면 보안이 필요하게 된다.
	- 무조건 적인 것은 아니다. 보안을 구성하기 위해서는 언제나 Trade off (비용)가 있다.
- 트래픽 암호화는 어떻게 할 것인가?
	- 특정 암호화 매커니즘을 사용할 것인가? 
	- 내부망을 위한 인증서를 만들어 활용할 것인가? 
	- mTLS (mutual TLS) 이용이 가능한 서비스 메시를 사용할 것인가?

<br/>

**Network Policy**
- 인그레스 및 이그레스 트래픽을 정의할 수 있는 방화벽 규칙, 보안 그룹.
- 쿠버네티스에서 네트워크 API의 일부로 [`NetworkPolicy`](https://kubernetes.io/docs/concepts/services-networking/network-policies/) 리소스가 존재한다. 하지만 기본적으로 구현되어있지는 않고, CNI Plugin에서 이를 구현한다면 사용이 가능해진다.
- NetworkPolicy는 namespace 리소스인 것을 유의해야 함. 이 리소스가 없다면 기본적으로 워크로드(파드)와의 모든 통신을 허용하게 된다.
- NetworkPolicy API에서는 간단한 방화벽 구성 등만을 지원하기에 CNI 플러그인마다 더 강력한 기능들을 제공하기 위해 CR, 자체적인 API를 제공하는 경우가 많다.


<br/>


# CNI (Container Network Interface)
https://github.com/containernetworking/cni


**CNI는 쿠버네티스에서 워크로드에 네트워크 리소스를 사용하기 위한 표준**
- 컨테이너의 네트워크 인터페이스를 설정할 수 있도록 도와주는 명세, 라이브러리로 구성된다.
- CRI, CSI와 같이 네트워크 표준을 정하고, 이 표준에 맞게 구현한 플러그인을 쿠버네티스가 사용해 클러스터 네트워크를 만들게 된다.

<br/>

## 정확히 어떤 역할이야?
**"네트워크를 구성한다는 것"이 정확히 어떤 뜻일까?**
- Container network namespace를 삽입하고, bridge 작업 등 호스트에 적절한 작업을 수행하는 것
- 네트워크 인터페이스에 IP를 할당하고, IP 대역에 맞는 라우팅 정보를 설정하는 것
- 네트워크 네임스페이스? 그와 관련된 작업이 뭐야?
	- [커피고래님의 글](https://coffeewhale.com/k8s/network/2019/04/19/k8s-network-01/)을 참고해보자.


<br/>

만약 CNI, 그의 구현체가 없었다면, **파드의 생성/배포 때마다 네트워크 네임스페이스를 생성하고 이를 클러스터 내 저장하고, 라우팅 설정 하는 등의 작업을 반복**해야 한다. 이런 것들을 자동화 한 것이라고 이해하면 된다.
- Network를 담당하는 소프트웨어이며 쿠버네티스 입장에 맞춘 것이 CNI, CNI plugins.
	- ex) 저기요 CNI plugin씨... 제가 파드를 시작했는데 이거 네트워크로 전체/다른 클러스터와 또 모든 파드들과 소통할 수 있게 만들어주실 수 있나요...?

<br/>


## `kube-proxy`와의 관계

**그럼 `kube-proxy`의 역할은 뭐야?**

![d](../assets/Pasted%20image%2020240121202543.png)
- 쿠버네티스 기본 컴포넌트인 `kube-proxy`에 대해 이해가 부족했던 나머지, CNI와 차이가 궁금해졌다.
- `kube-proxy`는 Service가 생성되면 이를 Pod IP로 치환하는 역할을 맡는다. Pod, Pod IP 간의 통신에 영향을 주거나 하는 등 작업은 CNI 관할이다.
	- `kube-proxy`의 모드에 따라 다르지만, 기본 모드인 iptable 모드라면 Service 생성에 따라 iptables를 수정/적용하는 액션을 수행하게 된다. ([문서](https://kubernetes.io/docs/reference/networking/virtual-ips/#proxy-modes))
- CNI는 `kube-proxy`에 영향을 주기도 한다. 특정 CNI Plugin을 이용하면, `kube-proxy`의 역할을 대체할 수도 있다.
	- 예로 Cilium을 이용하면 Cilium이 `kube-proxy`를 대체한다.
	- [Kubernetes Without kube-proxy](https://docs.cilium.io/en/stable/network/kubernetes/kubeproxy-free/)


<br/>

## CNI 사양
- ADD, DELETE, CHECK, VERSION
	- [api 참고](https://github.com/containernetworking/cni/blob/main/libcni/api.go)
- `컨테이너의 네트워크 연결성`과 `컨테이너 삭제에 따른 네트워크 리소스 해제`에만 집중한다.
	- 이외의 네트워크 리소스의 구체적인 사안에 대해서는 제한을 두지 않기에 구현체가 다양하고, 각기 특징들이 존재한다.

<br/>

## Architecture
https://github.com/containerd/cri/blob/v1.19.0/docs/architecture.md

![d](../assets/Pasted%20image%2020240121203446.png)

- CNI는 쿠버네티스와 단 둘이 동작하는 것이 아닌 CRI가 함께 동작한다. (위 containerd에서의 예제)
- CRI 명령 시, CNI에 대한 정보를 넘겨주어 CRI가 소통할 수 있게 구현되는 것으로 이해했다.
	- kubelet - CRI/CNI 라고 생각해야 겠다.
- 하긴, 컨테이너 내 네트워크 네임스페이스를 생성하고 이를 연결하는 동작을 하는데, 컨테이너를 담당하는 런타임과 함께 통합되어 동작해야 할 것 같긴 하다.

![d](../assets/Pasted%20image%2020240121203855.png)
![d](../assets/Pasted%20image%2020240121203649.png)
![d](../assets/Pasted%20image%2020240121203654.png)

- https://ronaknathani.com/blog/2020/08/how-a-kubernetes-pod-gets-an-ip-address/
- https://www.linkedin.com/posts/schakraborty007_kubelet-api-cri-activity-7033771239722672128-pL8n/?originalSubdomain=
- https://addozhang.medium.com/introduction-to-container-network-interface-cni-25309a64b23e

<br/>

## Plugins

위에서 말했지만, **CNI는 명세이지 구현체가 아니다.**
- CNI 명세에 맞게 구현한 여러 구현체(플러그인)들이 존재하고, 이러한 구현체를 클러스터 내 설치해야지만 클러스터 내 네트워크가 동작하기 시작한다.
- Calico, Cilium, Flannel 등이 존재한다.
- ![d](../assets/Pasted%20image%2020240121201441.png)

<br/>

**Calico**
- 가장 대중적으로 널리 사용되는 CNI Plugin
- 키워드: BGP, Network Policy(Global), IP in IP, VXLAN
	- Border Gateway Protocol (BGP)를 사용해 route를 공유하고 교환

<br/>

**Cilium**
![d](../assets/Pasted%20image%2020240121210134.png)
- eBPF를 활용하는 최초의 CNI
	- eBPF (extended Berkeley Packet Filter)
	- **커널 소스를 바꾸거나 추가모듈 추가 필요 없이 OS 커널 공간에서 실행하는 기술**
- 완전한 기능을 갖춘 `kube-proxy` 대체 구현체를 제공
	- eBPF Map에서 서비스에 대한 경로를 구성해 O(1)만큼 빠르게 해결한다.
- 카카오에서는 Cilium을 CNI로 이용한다.
	- [카카오가 K8s CNI Cilium의 메모리 누수 버그를 해결하는 방법](https://www.youtube.com/watch?v=8DxePykmO-A)
	- CNI plugin pod에 문제가 생기면 치명적인 것을 알 수 있다.


<br/>

**WeaveNet**
- [관련 자료](https://yuminlee2.medium.com/kubernetes-weave-net-cni-plugin-810849203c73)

<br/>


**멀터스**
![d](../assets/Pasted%20image%2020240121205041.png)
- 파드가 둘 이상의 네트워크에 연결돼야 하는 경우
- 다른 여러 CNI를 사용할 수 있는 CNI


<br/>

**Cloud provider**
- Amazon VPC CNI
	- ENI(Elastic Network Interface)를 이용해 IP Pool을 관리한다.
	- [ Node EC2 스펙이 무엇인가에 따라서 ENI 개수가 제한](https://docs.aws.amazon.com/ko_kr/AWSEC2/latest/UserGuide/using-eni.html#AvailableIpPerENI)되어있어, 할당 가능 IP 수랑 잘 비교해 사용해야 한다. IP가 부족해 할당이 되지 않는 사례가 발생할 수 있다.
- Azure CNI


<br/>

# 덤
쿠버네티스의 네트워크에 대해 나눠본다면, 이렇게 5가지로 나눠볼 수 있다.
- **1. container networking**
	- ex) CNI, Calico, Cilium
- **2. kubernetes service**
	- Kubernetes
- **3. kubernetes dns**
	- ex) coreDNS
- **4. outside access**
	- ex) emissary, envoy, metalLB
- **5. service mesh**
	- ex) linkerd, Istio


<br/>


여러 Kubernetes cluster 구성 툴들의 **기본 CNI Plugin**는 무엇일까?
- minkube: [kindnet](https://github.com/aojea/kindnet) ([문서](https://minikube.sigs.k8s.io/docs/handbook/network_policy/))
- k3s: [flannel](https://github.com/flannel-io/flannel/blob/master/README.md) ([문서](https://docs.k3s.io/installation/network-options))
- eks: Amazon VPC CNI

<br/>


관련해 커피고래 님의 블로그 번역 글 내용이 좋다.
- https://coffeewhale.com/k8s/network/2019/04/19/k8s-network-01/
- https://coffeewhale.com/packet-network1
- https://coffeewhale.com/calico-mode

<br/>


서비스 메시 타입
![d](../assets/Pasted%20image%2020240121173330.png)
- 5장과 6장 -> 6장이 라우팅 영역이라 서비스 메시인데, 5장과 연관되어 비슷한 자료가 계속 나온다. 그래서 그냥 백업.

<br/>


SDN
- Logical control plane
![d](../assets/Pasted%20image%2020240121170107.png)
![d](../assets/Pasted%20image%2020240121170151.png)

<br/>


**CIDR (Classless Inter-Domain Routing)**
- 라우팅/네트워킹 부분을 구성하는 중요 비트수 사양을 IP 주소 자체에 추가하자.
- `192.168.0.15/24` => 24 bit netmask (`255.255.255.0`)
	- 처음 24비트가 네트워크 라우팅에서 중요하다라는 것을 이야기하는 것.
- CIDR을 이용하면 연속적인 IP 주소블록을 서브넷보다 더 잘 제어할 수 있음.

<br/>

BGP란 무엇인가?
- 학교에서 배울 때 정리한 내용 참고.
- Inter-AS routing
	- BGP (Border Gateway Protocol): the de facto(사실상) inter-AS(domain) routing protocol
	- Determine “good” routes to other networks based on reachability information and policy
	- Allows subnet to advertise its existence to rest of Internet.
- 2개의 BGP로 이루어짐
	- **External BGP (eBGP)**: obtain subnet reachability information from neighboring ASes
	- **Internal BGP (iBGP)**: propagate reachability information to all AS-internal routers
	- 즉, 다른 AS간에는 eBGP connection, 같은 AS간에는 iBGP connection이 존재.
- BGP basics
	- BGP Session: Two BGP routers exchange BGP messages over semi-permanent TCP connection
	- Advertising paths to different destination network prefixes
	- ![](https://lh7-us.googleusercontent.com/f8ZwrTjFNfNzG3tfEYP5lxwfMwqwl7u8Yx11460oE5gkWAHV73tyqrP1Mhohcvsei4kC3o7o3XfTFZ9sczGHTrL586rNMD6RWtqeQlcfJ3xyq2G_rTdei7YD-aHRktlO-mTJOqmowBZMz-o2bLAZ)
- When 3a (AS3 gateway router) advertises path AS3, X to 2c (AS2 gateway router), AS3 promises to AS2 it will forward datagrams towards X.
		- 1. AS2에서 AS3를 거쳐 X로 갈 수 있다는 것을 알 수 있게 된다. 
	- 2. AS3가 AS2에게 3a를 거쳐가는 것을 허용해주는 것 - Policy 맺는 것
- Policy-based routing
	- Gateway receiving route advertisement uses import policy to accept/decline path
	- AS policy also determines whether to advertise path to other neighboring ASes
- BGP route selection: Hot potato routing
	- ![](https://lh7-us.googleusercontent.com/wOkFoUbTvd0vP663Yg6o3d_SADkjw64ePNDR-VnGnzVImd4IchHBnTagmvr2icABHw1vhIv6MvlwENco1omUhjfuVO6_9wRtCY5oZVXm64DXQcgtDLAuaNPrxWOkLWrepY4i8jDSWkXoKZM56T6U)
	- 2d router에서는 X까지 가는데 iBGP를 통해 얻은 정보가 2개가 존재한다. 2a로 보내서 AS1, AS3를 거쳐 가는 방식과, 2c로 보내서 AS3를 거쳐가는 방식. 이렇게 보면 당연히 AS3 하나만을 거쳐가는 방식이 좋아 보일 수 있다. 하지만 hot potato routing은 단순히 다른 AS와 연결되어 있는 gateway router 중에 더 가까운 gateway router를 택해 보낸다. 즉, 전체적으로 보면 더 돌아가는 길일지라도 intra-AS routing을 통해 가장 가까운 gateway router로 보낸다.
	- Choose local gateway that has least intra-domain cost -> don’t worry about inter-domain cost
	- Each router selects the closest egress point -> based on path cost in intra domain protocol.


<br/>

IP 주소 이해
- IP 주소를 사용하면 Network interface를 통해 네트워크 리소스에 연결할 수 있음.
- IP 주소는 자체 네트워크에서 고유해야 함. 네트워트 간에 서로 격리될 수 있으며, 서로 다른 네트워크 간에 액세스를 제공하기 위해 브릿지 또는 변환(translated) 될 수 있음.
- Network Address Translation(NAT) 시스템을 이용하면 패킷이 네트워크 경계를 통과할 때 주소를 변경해 올바른 목적지로 이동할 수 있도록 할 수 있음. 이를 통해 격리된 네트워크에서 동일한 IP 주소를 이용하고, 네트워크 간 서로 통신할 수 있음.

IPv4, IPv6
- IP 프로토콜에는 2가지 개정판이 있음. IPv4, IPv6. IPv6는 IPv4에 대해 프로토콜 개선과 주소공간의 한계를 해결한 것으로 서서히 대체해가고 있음.
- **IPv4는 32비트 주소**. 8비트 세그먼트는 마침표로 나뉘며 0~255 숫자로 표기.
	- 각 세그먼트 8비트의 표현을 옥텟(octet)이라고 함.
	- `192.168.0.5`
	- `1100 0000 - 1010 1000 - 0000 0000 - 0000 0101`
- **IPv6는 128비트 주소**. 16진수 4자리로 구성된 8개의 세그먼트로 표기.
	- 0~9, a~f를 이용해 16진수 표현
	- `1203:8fe0:fe80:b897:8990:8a7c:99bf:323d`
	- 각 옥텟에서 앞의 0을 제거하고 표현할 수 있으며, 0으로만 표현된 옥텟 그룹은 `::`로 표현이 가능
		- `...:00BC:18bc:0000:0000:0000:00ff:...`
		- == `...:BC:18bc::ff:...`

<br/>

그럼 위 IP 범위의 IP 주소는 모두 사용이 가능한가?
- 아님. 예약되는 것이 있음
- 클래스 A
    - **`0---`**: IPv4 주소의 첫 번째 비트가 "0"이면 해당 주소가 클래스 A에 속함을 의미합니다. 즉, **`0.0.0.0에서` `127.255.255.255까지의`** 모든 주소가 클래스 A에 속한다는 뜻입니다.
    - 첫 번째 옥텟을 네트워크가 사용, 나머지 주소는 호스트 정의
    - `127.0.0.0`에서 `127.255.255.255`는 루프백 범위로 예약됨. 그렇지만 대부분 `127.0.0.1` 사용
	    - **루프백이란? 네트워크상 자신을 나타내는 가상적인 주소이며, 자신에게 다시 네트워크 입력이 들어온다고 하여 루프백 주소라고 함.**
    - `10.0.0.0`에서 `10.255.255.255`까지의 주소는 사설 네트워크 할당을 위해 예약
- 클래스 B
    - **`10--`**: 클래스 B에는 **`128.0.0.0에서` `191.255.255.255까지의`** 모든 주소가 포함됩니다. 이는 첫 번째 비트에 "1"이 있지만 두 번째 비트에 "1"이 없는 주소를 나타냅니다.
    - 첫/두 번째 옥텟을 네트워크가 사용, 나머지 주소는 호스트 정의
    - `172.16.0.0`에서 `172.31.255.255`까지의 주소는 사설 네트워크 할당을 위해 예약
- 클래스 C
    - **`110-`**: 클래스 C는 **`192.0.0.0에서` `223.255.255.255까지의`** 주소로 정의됩니다. 이는 처음 두 비트는 "1"이지만 세 번째 비트는 "1"이 없는 모든 주소를 나타냅니다.
    - 첫/두/셋 번째 옥텟을 네트워크가 사용, 나머지 주소는 호스트 정의
    - `192.168.0.0` ~ `192.168.255.255`는 개인용으로 예약
- 클래스 D
    - **`1110`**: 이 클래스에는 처음 세 비트가 "111"이지만 다음 비트가 "0"인 주소가 포함됩니다. 이 주소 범위에는 **`224.0.0.0에서` `239.255.255.255까지의`** 주소가 포함됩니다.
    - 멀티 캐스팅 프로토콜을 위해 예약되어 있음.
- 클래스 E
    - **`1111`**: 이 클래스는 **`240.0.0.0에서` `255.255.255.255`** 사이의 주소를 정의합니다. 4개의 "1" 비트로 시작하는 모든 주소가 이 클래스에 포함됩니다.
    - 향후 실험적 사용을 위해 예약되어 있음. 거의 사용안함.

외울필요는 없고, 모든 주소를 사용할 수 없다는 것만 알아두면 됨.

네트워크를 더 작은 네트워크 섹션으로 나누는 것을 서브넷팅(subnetting)이라고 함.
- 네트워크 주소와 호스트 주소를 분리하고, 세분화하기 위한 방법
- 클래스 별 정해진 네트워크 사용 옥텟에 따라 호스트 분리하는 것?



