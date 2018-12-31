# dubbo-practice  
  
## dubbo学习实践 模块式开发实践  
  
### 特别注明，本文引用了部分Dubbo官网上面的资料，学习Dubbo最好的方式就是去官网。  
  
#### 1.Dubbo产生的背景  
  
随着互联网的发展，网站应用的规模不断扩大，常规的垂直应用架构已无法应对，分布式服务架构以及流动计算架构势在必行，亟需一个治理系统确保架构有条不紊的演进。  
![](https://github.com/YufeizhangRay/image/blob/master/Dubbo/%E8%83%8C%E6%99%AF.jpeg)  
  
单一应用架构  
当网站流量很小时，只需一个应用，将所有功能都部署在一起，以减少部署节点和成本。此时，用于简化增删改查工作量的数据访问框架(ORM)是关键。  
  
垂直应用架构  
当访问量逐渐增大，单一应用增加机器带来的加速度越来越小，将应用拆成互不相干的几个应用，以提升效率。此时，用于加速前端页面开发的Web框架(MVC)是关键。  
  
分布式服务架构  
当垂直应用越来越多，应用之间交互不可避免，将核心业务抽取出来，作为独立的服务，逐渐形成稳定的服务中心，使前端应用能更快速的响应多变的市场需求。此时，用于提高业务复用及整合的分布式服务框架(RPC)是关键。  
  
流动计算架构  
当服务越来越多，容量的评估，小服务资源的浪费等问题逐渐显现，此时需增加一个调度中心基于访问压力实时管理集群容量，提高集群利用率。此时，用于提高机器利用率的资源调度和治理中心(SOA)是关键。  
  
#### 2.Dubbo产生的需求  
  
![](https://github.com/YufeizhangRay/image/blob/master/Dubbo/%E9%9C%80%E6%B1%82.jpeg)  
  
在大规模服务化之前，应用可能只是通过 RMI 或 Hessian 等工具，简单的暴露和引用远程服务，通过配置服务的URL地址进行调用，通过 F5 等硬件进行负载均衡。  

当服务越来越多时，服务 URL 配置管理变得非常困难，F5 硬件负载均衡器的单点压力也越来越大。 此时需要一个服务注册中心，动态的注册和发现服务，使服务的位置透明。并通过在消费方获取服务提供方地址列表，实现软负载均衡和 Failover，降低对 F5 硬件负载均衡器的依赖，也能减少部分成本。  

当进一步发展，服务间依赖关系变得错踪复杂，甚至分不清哪个应用要在哪个应用之前启动，架构师都不能完整的描述应用的架构关系。 这时，需要自动画出应用间的依赖关系图，以帮助架构师理清理关系。  

接着，服务的调用量越来越大，服务的容量问题就暴露出来，这个服务需要多少机器支撑？什么时候该加机器？ 为了解决这些问题，第一步，要将服务现在每天的调用量，响应时间，都统计出来，作为容量规划的参考指标。其次，要可以动态调整权重，在线上，将某台机器的权重一直加大，并在加大的过程中记录响应时间的变化，直到响应时间到达阈值，记录此时的访问量，再以此访问量乘以机器数反推总容量。  

#### 3.Dubbo的架构  
  
![](https://github.com/YufeizhangRay/image/blob/master/Dubbo/%E6%9E%B6%E6%9E%84.jpeg)  
  
>Provider:	暴露服务的服务提供方  
Consumer:	调用远程服务的服务消费方  
Registry:	服务注册与发现的注册中心  
Monitor:	统计服务的调用次数和调用时间的监控中心  
Container:	服务运行容器  
  
在Dubbo中有自己内含的4种Container容器，SpringContainer、JettyContainer、Log4JContainer、LogbackContainer，默认为SpringContainer。于是Dubuo可以在不使用外部容器的情况下通过com.alibaba.dubbo.contrainer.Main.main(args)快速启动。若想使用默认的方式启动，则必须在classpath路径下存在META-INF/Spring/路径，并且存在相应的xml配置文件。Dubbo也支持同时启动多个container，例如我们可以在args参数中同时传入spring与log4j,Dubbo的源码会做一个循环处理，以启动每一个container。  
  
Dubbo支持4中注册中心：zookeeper、redis、multicast、simple，默认使用zookeeper。  
当服务的提供方和消费方都连接到zookeeper以后，可以在zookeeper的dubbo节点下找到所注册的所有服务节点，每一个服务节点下面分别又包含4个子节点，分别是：  
>consumers: 当前服务的消费节点  
configurators：配置  
routers：路由  
providers：当前服务的提供节点  
  
打开providers节点，可以看到类似如下的记录：  
>dubbo://192.168.188.138:20880%2Fcn.zyf.dubbo.IHello%3Fanyhost%3Dtrue%26application%3Dhello-world-app%26dubbo%3D2.5.6%26generic%3Dfalse%26interface%3Dcn.zyf.dubbo.IHello%26methods%3DsayHello%26pid%3D60700%26revision%3D1.0.0%26side%3Dprovider%26timestamp%3D1529498478644%26version%3D1.0.0  
  
Dubbo作为支持多协议的的RPC框架，一切都是以URL为基础，上面的记录其实就是以dubbo为协议的地址。  
  
Dubbo支持的协议：dubbo、RMI、hessian、webservice、http、thirft，默认为dubbo。  
  
除此之外，Dubbo也支持多注册中心服务，可以在配置文件中进行配置。  
  
#### 4.Dubbo的架构功能特性  
  
多版本支持  
设置不同版本的目的，就是要考虑到接口升级以后带来的兼容问题。在Dubbo中配置不同版本的接口，会在zookeeper地址中有多个协议URL的体现。  
>dubbo://192.168.188.138:20880%2Fcn.zyf.dubbo.IHello%3Fanyhost%3Dtrue%26application%3Dhello-world-app%26dubbo%3D2.5.6%26generic%3Dfalse%26interface%3Dcn.zyf.dubbo.IHello%26methods%3DsayHello%26pid%3D60700%26revision%3D1.0.0%26side%3Dprovider%26timestamp%3D1529498478644%26version%3D1.0.0  
  
>dubbo://192.168.188.138:20880%2Fcn.zyf.dubbo.IHello%3Fanyhost%3Dtrue%26application%3Dhello-world-app%26dubbo%3D2.5.6%26generic%3Dfalse%26interface%3Dcn.zyf.dubbo.IHello%26methods%3DsayHello%26pid%3D60700%26revision%3D1.0.0%26side%3Dprovider%26timestamp%3D1529498478644%26version%3D1.0.1  
  
主机绑定  
在发布一个Dubbo服务的时候，会生成一个dubbo://ip:port的协议地址，那么这个IP是根据什么生成的呢？可以在ServiceConfig.java代码中找到代码;可以发现，在生成绑定的主机的时候，会通过一层一层的判断，直到获取到合法的ip地址。  
  
集群容错  
什么是容错机制？ 容错机制指的是某种系统控制在一定范围内的一种允许或包容犯错情况发生的能力。举个简单例子，电脑上运行一个程序，有时候会出现无响应的情况，然后系统会弹出一个提示框让我们选择，是立即结束还是继续等待，然后根据我们的选择执行对应的操作，这就是“容错”。  
在分布式架构下，网络、硬件、应用都可能发生故障，由于各个服务之间可能存在依赖关系，如果一条链路中的其中一个节点出现故障，将会导致雪崩效应。为了减少某一个节点故障的影响范围，所以我们才需要去构建容错服务，来优雅的处理这种中断的响应结果。  
  
Dubbo提供了6种容错机制，分别如下：  
>1.	failsafe 失败安全，可以认为是把错误吞掉（记录日志）。
>2.	failover(默认)   重试其他服务器； retries（2）。
>3.	failfast 快速失败， 失败以后立马报错。
>4.	failback  失败后自动恢复。
>5.	forking forks. 设置并行数。
>6.	broadcast  广播，任意一台报错，则执行的方法报错。
  
服务降级  
降级的目的是为了保证核心服务可用。  
降级可以有几个层面的分类： 自动降级和人工降级； 按照功能可以分为：读服务降级和写服务降级；  
>1.	对一些非核心服务进行人工降级，在大促之前通过降级开关关闭哪些推荐内容、评价等对主流程没有影响的功能
>2.	故障降级，比如调用的远程服务挂了，网络故障、或者RPC服务返回异常。那么可以直接降级，降级的方案比如设置默认值、采用兜底数据（系统推荐的行为广告挂了，可以提前准备静态页面做返回）等等。
>3.	限流降级，在秒杀这种流量比较集中并且流量特别大的情况下，因为突发访问量特别大可能会导致系统支撑不了。这个时候可以采用限流来限制访问量。当达到阈值时，后续的请求被降级，比如进入排队页面，比如跳转到错误页（活动太火爆，稍后重试等）。
  
dubbo的降级方式：Mock
实现步骤
>1.	在client端创建一个TestMock类，实现对应IHello的接口（需要对哪个接口进行mock，就实现哪个），名称必须以Mock结尾。
>2.	在client端的xml配置文件中，在<dubbo:reference>中增加mock属性指向创建的TestMock。
>3.	模拟错误（设置timeout），模拟超时异常，运行测试代码即可访问到TestMock这个类。当服务端故障解除以后，调用过程将恢复正常。
 
配置优先级别
以timeout为例，显示了配置的查找顺序，其它retries, loadbalance等类似。
>1.	方法级优先，接口级次之，全局配置再次之。
>2.	如果级别一样，则消费方优先，提供方次之。  

其中，服务提供方配置，通过URL经由注册中心传递给消费方。
建议由服务提供方设置超时，因为一个方法需要执行多长时间，服务提供方更清楚，如果一个消费方同时引用多个服务，就不需要关心每个服务的超时设置。
  
#### 5.Dubbo的SPI   
  
Dubbo SPI和JAVA SPI 的使用和对比  
  
在Dubbo中，SPI是一个非常核心的机制，贯穿在几乎所有的流程中。
Dubbo的SPI是基于Java原生SPI机制思想的一个改进，所以，先从JAVA SPI机制开始了解什么是SPI以后再去学习Dubbo的SPI，就比较容易了。
  
关于JAVA 的SPI机制
SPI全称（service provider interface），是JDK内置的一种服务提供发现机制，目前市面上有很多框架都是用它来做服务的扩展发现，大家耳熟能详的如JDBC、日志框架都有用到；
简单来说，它是一种动态替换发现的机制。举个简单的例子，如果我们定义了一个规范，需要第三方厂商去实现，那么对于我们应用方来说，只需要集成对应厂商的插件，既可以完成对应规范的实现机制。 形成一种插拔式的扩展手段。 
  
SPI规范总结
实现SPI，就需要按照SPI本身定义的规范来进行配置，SPI规范如下
>1.	需要在classpath下创建一个目录，该目录命名必须是：META-INF/services
>2.	在该目录下创建一个properties文件，该文件需要满足以下几个条件
>>a)	文件名必须是扩展的接口的全路径名称  
>>b)	文件内部描述的是该扩展接口的所有实现类  
>>c)	文件的编码格式是UTF-8  
>3.	通过java.util.ServiceLoader的加载机制来发现
  
SPI的实际应用
SPI在很多地方有应用，大家可以看看最常用的java.sql.Driver驱动。JDK官方提供了java.sql.Driver这个驱动扩展点，但是你们并没有看到JDK中有对应的Driver实现。 
以连接Mysql为例，我们需要添加mysql-connector-java依赖。然后，你们可以在这个jar包中找到SPI的配置信息。如下图，所以java.sql.Driver由各个数据库厂商自行实现。这就是SPI的实际应用，在spring的包中也可以看到相应的痕迹。
![](https://github.com/YufeizhangRay/image/blob/master/Dubbo/SPI.jpeg)  
   
SPI的缺点
>1.JDK标准的SPI会一次性加载实例化扩展点的所有实现，什么意思呢？就是如果你在META-INF/service下的文件里面加了N个实现类，那么JDK启动的时候都会一次性全部加载。那么如果有的扩展点实现初始化很耗时或者如果有些实现类并没有用到，那么会很浪费资源。  
>2.如果扩展点加载失败，会导致调用方报错，而且这个错误很难定位到是这个原因。
  
Dubbo优化后的SPI实现
基于Dubbo提供的SPI规范实现自己的扩展。

Dubbo的SPI机制规范
大部分的思想都是和SPI是一样，只是下面两个地方有差异。
>1.需要在resource目录下配置META-INF/dubbo或者META-INF/dubbo/internal或者META-INF/services，并基于SPI接口去创建一个文件。  
>2.文件名称和接口名称保持一致，文件内容和SPI有差异，内容是KEY对应Value。  
  
通过Dubbo，我们可以实现自己的Protocol。
