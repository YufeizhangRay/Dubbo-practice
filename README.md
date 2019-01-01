# dubbo-practice  
  
## Dubbo作用、特性、源码分析 模块式开发实践  
  
### 特别注明，本文引用了部分Dubbo官网上面的资料，学习Dubbo最好的方式就是去官网。  

- [Dubbo产生背景](#dubbo-产生背景)  
- [Dubbo产生需求](#dubbo-产生需求)  
- [Dubbo的架构](#dubbo的架构)  
- [Dubbo的架构功能特性](#dubbo的架构功能特性)  
- [Dubbo的SPI](#dubbo的spi)  
- [Dubbo源码分析](#dubbo源码分析)  
 

  
### 1.Dubbo产生背景  
  
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
  
### 2.Dubbo产生需求  
  
![](https://github.com/YufeizhangRay/image/blob/master/Dubbo/%E9%9C%80%E6%B1%82.jpeg)  
  
在大规模服务化之前，应用可能只是通过 RMI 或 Hessian 等工具，简单的暴露和引用远程服务，通过配置服务的URL地址进行调用，通过 F5 等硬件进行负载均衡。  

当服务越来越多时，服务 URL 配置管理变得非常困难，F5 硬件负载均衡器的单点压力也越来越大。 此时需要一个服务注册中心，动态的注册和发现服务，使服务的位置透明。并通过在消费方获取服务提供方地址列表，实现软负载均衡和 Failover，降低对 F5 硬件负载均衡器的依赖，也能减少部分成本。  

当进一步发展，服务间依赖关系变得错踪复杂，甚至分不清哪个应用要在哪个应用之前启动，架构师都不能完整的描述应用的架构关系。 这时，需要自动画出应用间的依赖关系图，以帮助架构师理清理关系。  

接着，服务的调用量越来越大，服务的容量问题就暴露出来，这个服务需要多少机器支撑？什么时候该加机器？ 为了解决这些问题，第一步，要将服务现在每天的调用量，响应时间，都统计出来，作为容量规划的参考指标。其次，要可以动态调整权重，在线上，将某台机器的权重一直加大，并在加大的过程中记录响应时间的变化，直到响应时间到达阈值，记录此时的访问量，再以此访问量乘以机器数反推总容量。  

### 3.Dubbo的架构  
  
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
  
### 4.Dubbo的架构功能特性  
  
#### 多版本支持  
设置不同版本的目的，就是要考虑到接口升级以后带来的兼容问题。在Dubbo中配置不同版本的接口，会在zookeeper地址中有多个协议URL的体现。  
>dubbo://192.168.188.138:20880%2Fcn.zyf.dubbo.IHello%3Fanyhost%3Dtrue%26application%3Dhello-world-app%26dubbo%3D2.5.6%26generic%3Dfalse%26interface%3Dcn.zyf.dubbo.IHello%26methods%3DsayHello%26pid%3D60700%26revision%3D1.0.0%26side%3Dprovider%26timestamp%3D1529498478644%26version%3D1.0.0  
  
>dubbo://192.168.188.138:20880%2Fcn.zyf.dubbo.IHello%3Fanyhost%3Dtrue%26application%3Dhello-world-app%26dubbo%3D2.5.6%26generic%3Dfalse%26interface%3Dcn.zyf.dubbo.IHello%26methods%3DsayHello%26pid%3D60700%26revision%3D1.0.0%26side%3Dprovider%26timestamp%3D1529498478644%26version%3D1.0.1  
  
#### 主机绑定  
在发布一个Dubbo服务的时候，会生成一个dubbo://ip:port的协议地址，那么这个IP是根据什么生成的呢？可以在ServiceConfig.java代码中找到代码;可以发现，在生成绑定的主机的时候，会通过一层一层的判断，直到获取到合法的ip地址。  
```
1.NetUtils.isInvalidLocalHost(host); 从配置文件中获取host
2.host = InetAddress.getLocalHost().getHostAddress(); 直接查询本地地址
3.Socket socket = new Socket(); 连接一个socket，再通过socket获取本地地址
try {
    SocketAddress addr = new InetSocketAddress(registryURL.getHost(), registryURL.getPort());
    socket.connect(addr, 1000);
    host = socket.getLocalAddress().getHostAddress();
    break;
} finally {
    try {
        socket.close();
    } catch (Throwable e) {}
}
4.public static String getLocalHost(){  遍历本地网卡，返回合理的IP地址
  InetAddress address = getLocalAddress();
  return address == null ? LOCALHOST : address.getHostAddress();
}
  ```
#### 集群容错  
什么是容错机制？ 容错机制指的是某种系统控制在一定范围内的一种允许或包容犯错情况发生的能力。举个简单例子，电脑上运行一个程序，有时候会出现无响应的情况，然后系统会弹出一个提示框让我们选择，是立即结束还是继续等待，然后根据我们的选择执行对应的操作，这就是“容错”。  
在分布式架构下，网络、硬件、应用都可能发生故障，由于各个服务之间可能存在依赖关系，如果一条链路中的其中一个节点出现故障，将会导致雪崩效应。为了减少某一个节点故障的影响范围，所以我们才需要去构建容错服务，来优雅的处理这种中断的响应结果。  
  
Dubbo提供了6种容错机制，分别如下：  
>1.	failsafe 失败安全，可以认为是把错误吞掉（记录日志）。
>2.	failover(默认)   重试其他服务器； retries（2）。
>3.	failfast 快速失败， 失败以后立马报错。
>4.	failback  失败后自动恢复。
>5.	forking forks. 设置并行数。
>6.	broadcast  广播，任意一台报错，则执行的方法报错。
  
#### 服务降级  
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
 
#### 配置优先级别
以timeout为例，显示了配置的查找顺序，其它retries, loadbalance等类似。
>1.	方法级优先，接口级次之，全局配置再次之。
>2.	如果级别一样，则消费方优先，提供方次之。  

建议由服务提供方设置超时，因为一个方法需要执行多长时间，服务提供方更清楚，如果一个消费方同时引用多个服务，就不需要关心每个服务的超时设置。  

#### 负载均衡  
  
>Random LoadBalance  
随机，按权重设置随机概率。    
在一个截面上碰撞的概率高，但调用量越大分布越均匀，而且按概率使用权重后也比较均匀，有利于动态调整提供者权重  
  
>RoundRobin LoadBalance  
轮循，按公约后的权重设置轮循比率。  
存在慢的提供者累积请求问题，比如：第二台机器很慢，但没挂，当请求调到第二台时就卡在那，久而久之，所有请求都卡在调到第二台上。  
  
>LeastActive LoadBalance  
最少活跃调用数，相同活跃数的随机，活跃数指调用前后计数差。  
使慢的提供者收到更少请求，因为越慢的提供者的调用前后计数差会越大。  
  
>ConsistentHash LoadBalance  
一致性Hash，相同参数的请求总是发到同一提供者。  
当某一台提供者挂时，原本发往该提供者的请求，基于虚拟节点，平摊到其它提供者，不会引起剧烈变动。  
  
### 5.Dubbo的SPI   
  
Dubbo SPI和JAVA SPI 的使用和对比  
  
在Dubbo中，SPI是一个非常核心的机制，贯穿在几乎所有的流程中。
Dubbo的SPI是基于Java原生SPI机制思想的一个改进，所以，先从JAVA SPI机制开始了解什么是SPI以后再去学习Dubbo的SPI，就比较容易了。
  
关于JAVA 的SPI机制
SPI全称（service provider interface），是JDK内置的一种服务提供发现机制，目前市面上有很多框架都是用它来做服务的扩展发现，大家耳熟能详的如JDBC、日志框架都有用到；
简单来说，它是一种动态替换发现的机制。举个简单的例子，如果我们定义了一个规范，需要第三方厂商去实现，那么对于我们应用方来说，只需要集成对应厂商的插件，既可以完成对应规范的实现机制。形成一种插拔式的扩展手段。 
  
SPI规范总结
实现SPI，就需要按照SPI本身定义的规范来进行配置，SPI规范如下
>1.	需要在classpath下创建一个目录，该目录命名必须是：META-INF/services
>2.	在该目录下创建一个properties文件，该文件需要满足以下几个条件
>>a)	文件名必须是扩展的接口的全路径名称  
>>b)	文件内部描述的是该扩展接口的所有实现类  
>>c)	文件的编码格式是UTF-8  
>3.	通过java.util.ServiceLoader的加载机制来发现
  
SPI的实际应用
SPI在很多地方有应用，可以看看最常用的java.sql.Driver驱动。JDK官方提供了java.sql.Driver这个驱动扩展点，但是并没有看到JDK中有对应的Driver实现。 
以连接Mysql为例，我们需要添加mysql-connector-java依赖。可以在这个jar包中找到SPI的配置信息。如下图，所以java.sql.Driver由各个数据库厂商自行实现。这就是SPI的实际应用，在spring的包中也可以看到相应的痕迹。
![](https://github.com/YufeizhangRay/image/blob/master/Dubbo/SPI.jpeg)  
   
SPI的缺点
>1.JDK标准的SPI会一次性加载实例化扩展点的所有实现。如果你在META-INF/service下的文件里面加了N个实现类，那么JDK启动的时候都会一次性全部加载。如果有的扩展点实现初始化很耗时或者如果有些实现类并没有用到，那么会很浪费资源。  
>2.如果扩展点加载失败，会导致调用方报错，而且这个错误很难定位到是这个原因。
  
Dubbo优化后的SPI实现
基于Dubbo提供的SPI规范实现自己的扩展。

Dubbo的SPI机制规范
大部分的思想都是和SPI是一样，只是下面两个地方有差异。
>1.需要在resource目录下配置META-INF/dubbo或者META-INF/dubbo/internal或者META-INF/services，并基于SPI接口去创建一个文件。  
>2.文件名称和接口名称保持一致，文件内容和SPI有差异，内容是KEY对应Value。  
  
通过Dubbo的SPI机制，我们可以实现自己的Protocol。
  
### 5.Dubbo源码分析  
  
#### Extension源码的结构  
  
了解源码结构，建立一个全局认识。结构图如下
![](https://github.com/YufeizhangRay/image/blob/master/Dubbo/extention.jpeg)  
  
Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();  
  
把上面这段代码分成两段，一段是getExtensionLoader、 另一段是getAdaptiveExtension。   
初步猜想一下:  
>第一段是通过一个Class参数去获得一个ExtensionLoader对象，有点类似一个工厂模式。  
第二段getAdaptiveExtension，去获得一个自适应的扩展点    
  
#### Protocol源码
以下是Protocol的源码，在这个源码中可以看到有两个注解，一个是在类级别上的@SPI(“dubbo”)，另一个是@Adaptive。  
@SPI 表示当前这个接口是一个扩展点，可以实现自己的扩展实现，默认的扩展点是DubboProtocol。  
@Adaptive  表示一个自适应扩展点，在方法级别上，会动态生成一个适配器类。如果是在类级别上，表示直接加载自定义的自适应适配器。  
```
@SPI("dubbo")
public interface Protocol {
    
    /**
     * 获取缺省端口，当用户没有配置端口时使用。
     * 
     * @return 缺省端口
     */
    int getDefaultPort();

    /**
     * 暴露远程服务：<br>
     * 1. 协议在接收请求时，应记录请求来源方地址信息：RpcContext.getContext().setRemoteAddress();<br>
     * 2. export()必须是幂等的，也就是暴露同一个URL的Invoker两次，和暴露一次没有区别。<br>
     * 3. export()传入的Invoker由框架实现并传入，协议不需要关心。<br>
     * 
     * @param <T> 服务的类型
     * @param invoker 服务的执行体
     * @return exporter 暴露服务的引用，用于取消暴露
     * @throws RpcException 当暴露服务出错时抛出，比如端口已占用
     */
    @Adaptive
    <T> Exporter<T> export(Invoker<T> invoker) throws RpcException;

    /**
     * 引用远程服务：<br>
     * 1. 当用户调用refer()所返回的Invoker对象的invoke()方法时，协议需相应执行同URL远端export()传入的Invoker对象的invoke()方法。<br>
     * 2. refer()返回的Invoker由协议实现，协议通常需要在此Invoker中发送远程请求。<br>
     * 3. 当url中有设置check=false时，连接失败不能抛出异常，并内部自动恢复。<br>
     * 
     * @param <T> 服务的类型
     * @param type 服务的类型
     * @param url 远程服务的URL地址
     * @return invoker 服务的本地代理
     * @throws RpcException 当连接服务提供方失败时抛出
     */
    @Adaptive
    <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException;

    /**
     * 释放协议：<br>
     * 1. 取消该协议所有已经暴露和引用的服务。<br>
     * 2. 释放协议所占用的所有资源，比如连接和端口。<br>
     * 3. 协议在释放后，依然能暴露和引用新的服务。<br>
     */
    void destroy();
}
```
由上述源码可知，我们可以通过Protocol来发布暴露(export()方法)服务于引用消费(refer()方法)服务。  
  
getExtensionLoader  
该方法需要一个Class类型的参数，该参数表示希望加载的扩展点类型，该参数必须是接口，且该接口必须被@SPI注解注释，否则拒绝处理。检查通过之后首先会检查ExtensionLoader缓存中是否已经存在该扩展对应的ExtensionLoader，如果有则直接返回，否则创建一个新的ExtensionLoader负责加载该扩展实现，同时将其缓存起来。可以看到对于每一个扩展，dubbo中只会有一个对应的ExtensionLoader实例。    

```
@SuppressWarnings("unchecked")    
public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {    
    if (type == null)    
        throw new IllegalArgumentException("Extension type == null");    
    if(!type.isInterface()) {    
        throw new IllegalArgumentException("Extension type(" + type + ") is not interface!");    
    }    
    if(!withExtensionAnnotation(type)) {    
        throw new IllegalArgumentException("Extension type(" + type +     
                ") is not extension, because WITHOUT @" + SPI.class.getSimpleName() + " Annotation!");    
    }
 
    ExtensionLoader<T> loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
    if (loader == null) {
        EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader<T>(type));
        loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
    }
    return loader;
}
```

ExtensionLoader  
提供了一个私有的构造函数，并且在这里面对两个成员变量type/objectFactory进行赋值。
```
private ExtensionLoader(Class<?> type) {
    this.type = type;
    objectFactory = (type == ExtensionFactory.class ? null :
            ExtensionLoader.getExtensionLoader(ExtensionFactory.class).
                    getAdaptiveExtension());
}
```
getAdaptiveExtension  
通过getExtensionLoader获得了对应的ExtensionLoader实例以后，再调用getAdaptiveExtension()方法来获得一个自适应扩展点。
ps：这个自适应扩展点实际上就是一个适配器。
这个方法里面主要做几个事情：
>1.	从cacheAdaptiveInstance 这个内存缓存中获得一个对象实例
>2.	如果实例为空，说明是第一次加载，则通过双重检查锁的方式去创建一个适配器扩展点
```
public T getAdaptiveExtension() {
    Object instance = cachedAdaptiveInstance.get();
    if (instance == null) {
        if(createAdaptiveInstanceError == null) {
            synchronized (cachedAdaptiveInstance) {
                instance = cachedAdaptiveInstance.get();
                if (instance == null) {
                    try {
                        instance = createAdaptiveExtension();
                        cachedAdaptiveInstance.set(instance);
                    } catch (Throwable t) {
                        createAdaptiveInstanceError = t;
                        throw new IllegalStateException("fail to create adaptive instance: " + t.toString(), t);
                    }
                }
            }
        }
        else {
            throw new IllegalStateException("fail to create adaptive instance: " + createAdaptiveInstanceError.toString(), createAdaptiveInstanceError);
        }
    }
    return (T) instance;
}
```
createAdaptiveExtension  
这段代码里面有两个结构，一个是injectExtension，另一个是getAdaptiveExtensionClass()。  
我们需要先去了解getAdaptiveExtensionClass这个方法做了什么？很显然，从后面的.newInstance来看，应该是获得一个类并且进行实例。

```
private T createAdaptiveExtension() {
    try {
        //可以实现扩展点的注入
        return injectExtension((T) getAdaptiveExtensionClass().newInstance());
    } catch (Exception e) {
        throw new IllegalStateException("Can not create adaptive extenstion " + type + ", cause: " + e.getMessage(), e);
    }
}
```
getAdaptiveExtensionClass  
从类名来看，是获得一个适配器扩展点的类。
在这段代码中，做了两个事情。
>1.getExtensionClasses() 加载所有路径下的扩展点。  
>2.createAdaptiveExtensionClass() 动态创建一个扩展点。  
  
cachedAdaptiveClass这里有个判断，用来判断当前Protocol这个扩展点是否存在一个自定义的适配器，如果有，则直接返回自定义适配器，否则，就动态创建，这个值是在getExtensionClasses中赋值的。
```
private Class<?> getAdaptiveExtensionClass() {
    getExtensionClasses();
    if (cachedAdaptiveClass != null) {
        return cachedAdaptiveClass;
    }
    return cachedAdaptiveClass = createAdaptiveExtensionClass();
}
```
createAdaptiveExtensionClass  
动态生成适配器代码，以及动态编译。
>1.createAdaptiveExtensionClassCode，动态创建一个字节码文件。返回code这个字符串。  
>2.通过compiler.compile进行编译（默认情况下使用的是javassist）。  
>3.通过ClassLoader加载到jvm中。  
```
//创建一个适配器扩展点。（创建一个动态的字节码文件）
private Class<?> createAdaptiveExtensionClass() {
    //生成字节码代码
    String code = createAdaptiveExtensionClassCode();
    //获得类加载器
    ClassLoader classLoader = findClassLoader();
    com.alibaba.dubbo.common.compiler.Compiler compiler = ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.common.compiler.Compiler.class).getAdaptiveExtension();
    //动态编译字节码
    return compiler.compile(code, classLoader);
}
```
CODE的字节码内容
```
public class Protocol$Adaptive implements com.alibaba.dubbo.rpc.Protocol {
    public void destroy() {
        throw new UnsupportedOperationException("method public abstract void com.alibaba.dubbo.rpc.Protocol.destroy() of interface com.alibaba.dubbo.rpc.Protocol is not adaptive method!");
    }

    public int getDefaultPort() {
        throw new UnsupportedOperationException("method public abstract int com.alibaba.dubbo.rpc.Protocol.getDefaultPort() of interface com.alibaba.dubbo.rpc.Protocol is not adaptive method!");
    }

    public com.alibaba.dubbo.rpc.Invoker refer(java.lang.Class arg0, com.alibaba.dubbo.common.URL arg1) throws com.alibaba.dubbo.rpc.RpcException {
        if (arg1 == null) throw new IllegalArgumentException("url == null");
        com.alibaba.dubbo.common.URL url = arg1;
        String extName = (url.getProtocol() == null ? "dubbo" : url.getProtocol());
        if (extName == null)
            throw new IllegalStateException("Fail to get extension(com.alibaba.dubbo.rpc.Protocol) name from url(" + url.toString() + ") use keys([protocol])");
        com.alibaba.dubbo.rpc.Protocol extension = (com.alibaba.dubbo.rpc.Protocol) ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.rpc.Protocol.class).getExtension(extName);
        return extension.refer(arg0, arg1);
    }

    public com.alibaba.dubbo.rpc.Exporter export(com.alibaba.dubbo.rpc.Invoker arg0) throws com.alibaba.dubbo.rpc.RpcException {
        if (arg0 == null) throw new IllegalArgumentException("com.alibaba.dubbo.rpc.Invoker argument == null");
        if (arg0.getUrl() == null)
            throw new IllegalArgumentException("com.alibaba.dubbo.rpc.Invoker argument getUrl() == null");
        com.alibaba.dubbo.common.URL url = arg0.getUrl();
        String extName = (url.getProtocol() == null ? "dubbo" : url.getProtocol());
        if (extName == null)
            throw new IllegalStateException("Fail to get extension(com.alibaba.dubbo.rpc.Protocol) name from url(" + url.toString() + ") use keys([protocol])");
        com.alibaba.dubbo.rpc.Protocol extension = (com.alibaba.dubbo.rpc.Protocol) ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.rpc.Protocol.class).getExtension(extName);
        return extension.export(arg0);
    }
}
```
Protocol$Adaptive的主要功能 
>1.从URL或扩展接口获取扩展接口实现类的名称；  
>2.根据名称，获取实现类ExtensionLoader.getExtensionLoader(扩展接口类).getExtension(扩展接口实现类名称)，然后调用实现类的方法。  
  
需要明白一点dubbo的内部传参基本上都是基于URL来实现的，也就是说Dubbo是基于URL驱动的技术。  
所以，适配器类的目的是在运行期获取扩展的真正实现来调用，解耦接口和实现，这样的话要不我们自己实现适配器类，要不dubbo帮我们生成，而这些都是通过Adpative来实现。  
到目前为止，我们的AdaptiveExtension的主线走完了，可以简单整理一下他们的调用关系如下：  
![](https://github.com/YufeizhangRay/image/blob/master/Dubbo/%E8%B0%83%E7%94%A8%E7%BB%93%E6%9E%841.jpeg)  
  
我们再回过去梳理下代码，实际上在调用createAdaptiveExtensionClass之前，还做了一个操作。是执行getExtensionClasses方法，我们来看看这个方法做了什么事情。  
  
getExtensionClasses  
getExtensionClasses这个方法，就是加载扩展点实现类了。  
>1.从cachedClasses中获得一个结果，这个结果实际上就是所有的扩展点类，key对应name，value对应class  
>2.通过双重检查锁进行判断  
>3.调用loadExtensionClasses，去加载左右扩展点的实现  
```
//加载扩展点的实现类
private Map<String, Class<?>> getExtensionClasses() {
       
       Map<String, Class<?>> classes = cachedClasses.get();
       if (classes == null) {
           synchronized (cachedClasses) {
               classes = cachedClasses.get();
               if (classes == null) {
                   classes = loadExtensionClasses();
                   cachedClasses.set(classes);
               }
           }
       }
       return classes;
}
```
loadExtensionClasses  
从不同目录去加载扩展点的实现，在最开始的时候题到过的。META-INF/dubbo;META-INF/internal;META-INF/services;  
  
主要逻辑
>1.获得当前扩展点的注解，也就是Protocol.class这个类的注解，@SPI  
>2.判断这个注解不为空，则再次获得@SPI中的value值  
>3.如果value有值，也就是@SPI(“dubbo”)，则将这个dubbo的值赋给cachedDefaultName。这就是为什么我们能够通过
ExtensionLoader.getExtensionLoader(Protocol.class).getDefaultExtension() ,能够获得DubboProtocol这个扩展点的原因  
>4.最后，通过loadFile去加载指定路径下的所有扩展点。也就是META-INF/dubbo;META-INF/internal;META-INF/services  
```
private Map<String, Class<?>> loadExtensionClasses() {
    //type->Protocol.class
    //得到SPI的注解
    final SPI defaultAnnotation = type.getAnnotation(SPI.class);
    if(defaultAnnotation != null) { //如果不等于空.
        String value = defaultAnnotation.value();
        if(value != null && (value = value.trim()).length() > 0) {
            String[] names = NAME_SEPARATOR.split(value);
            if(names.length > 1) {
                throw new IllegalStateException("more than 1 default extension name on extension " + type.getName()
                        + ": " + Arrays.toString(names));
            }
            if(names.length == 1) cachedDefaultName = names[0];
        }
    }
    Map<String, Class<?>> extensionClasses = new HashMap<String, Class<?>>();
    loadFile(extensionClasses, DUBBO_INTERNAL_DIRECTORY);
    loadFile(extensionClasses, DUBBO_DIRECTORY);
    loadFile(extensionClasses, SERVICES_DIRECTORY);
    return extensionClasses;
}
```
loadFile  
解析指定路径下的文件，获取对应的扩展点，通过反射的方式进行实例化以后，put到extensionClasses这个Map集合中
```
private void loadFile(Map<String, Class<?>> extensionClasses, String dir) {
    String fileName = dir + type.getName();
    try {
        Enumeration<java.net.URL> urls;
        ClassLoader classLoader = findClassLoader();
        if (classLoader != null) {
            urls = classLoader.getResources(fileName);
        } else {
            urls = ClassLoader.getSystemResources(fileName);
        }
        if (urls != null) {
            while (urls.hasMoreElements()) {
                java.net.URL url = urls.nextElement();
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "utf-8"));
                    try {
                        String line = null;
                        while ((line = reader.readLine()) != null) {
                            final int ci = line.indexOf('#');
                            if (ci >= 0) line = line.substring(0, ci);
                            line = line.trim();
                            if (line.length() > 0) {
                                try {
                                    String name = null;
                                    int i = line.indexOf('=');
                                    if (i > 0) {//文件采用name=value方式，通过i进行分割
                                        name = line.substring(0, i).trim();
                                        line = line.substring(i + 1).trim();
                                    }
                                    if (line.length() > 0) {
                                        Class<?> clazz = Class.forName(line, true, classLoader);
                                        //加载对应的实现类，并且判断实现类必须是当前的加载的扩展点的实现
                                        if (! type.isAssignableFrom(clazz)) {
                                            throw new IllegalStateException("Error when load extension class(interface: " +
                                                    type + ", class line: " + clazz.getName() + "), class " 
                                                    + clazz.getName() + "is not subtype of interface.");
                                        }

                                        //判断是否有自定义适配类，如果有，则在前面获取适配类的时候，直接返回当前的自定义适配类，不需要再动态创建
// 在前面的getAdaptiveExtensionClass中有一个判断,用来判断cachedAdaptiveClass是不是为空。如果不为空，表示存在自定义扩展点。也就不会去动态生成字节码了。这个地方可以得到一个简单的结论；
// @Adaptive如果是加在类上， 表示当前类是一个自定义的自适应扩展点
//如果是加在方法级别上，表示需要动态创建一个自适应扩展点，也就是Protocol$Adaptive
                                        if (clazz.isAnnotationPresent(Adaptive.class)) {
                                            if(cachedAdaptiveClass == null) {
                                                cachedAdaptiveClass = clazz;
                                            } else if (! cachedAdaptiveClass.equals(clazz)) {
                                                throw new IllegalStateException("More than 1 adaptive class found: "
                                                        + cachedAdaptiveClass.getClass().getName()
                                                        + ", " + clazz.getClass().getName());
                                            }
                                        } else {
                                            try {
                                                //如果没有Adaptive注解，则判断当前类是否带有参数是type类型的构造函数，如果有，则认为是
                                                //wrapper类。这个wrapper实际上就是对扩展类进行装饰.
                                                //可以在dubbo-rpc-api/internal下找到Protocol文件，发现Protocol配置了3个装饰
                                                //分别是,filter/listener/mock. 所以Protocol这个实例来说，会增加对应的装饰器
                                                clazz.getConstructor(type);//
                                                //得到带有public DubboProtocol(Protocol protocol)的扩展点。进行包装
                                                Set<Class<?>> wrappers = cachedWrapperClasses;
                                                if (wrappers == null) {
                                                    cachedWrapperClasses = new ConcurrentHashSet<Class<?>>();
                                                    wrappers = cachedWrapperClasses;
                                                }
                                                wrappers.add(clazz);//包装类 ProtocolFilterWrapper(ProtocolListenerWrapper(Protocol))
                                            } catch (NoSuchMethodException e) {
                                                clazz.getConstructor();
                                                if (name == null || name.length() == 0) {
                                                    name = findAnnotationName(clazz);
                                                    if (name == null || name.length() == 0) {
                                                        if (clazz.getSimpleName().length() > type.getSimpleName().length()
                                                                && clazz.getSimpleName().endsWith(type.getSimpleName())) {
                                                            name = clazz.getSimpleName().substring(0, clazz.getSimpleName().length() - type.getSimpleName().length()).toLowerCase();
                                                        } else {
                                                            throw new IllegalStateException("No such extension name for the class " + clazz.getName() + " in the config " + url);
                                                        }
                                                    }
                                                }
                                                String[] names = NAME_SEPARATOR.split(name);
                                                if (names != null && names.length > 0) {
                                                    Activate activate = clazz.getAnnotation(Activate.class);
                                                    if (activate != null) {
                                                        cachedActivates.put(names[0], activate);
                                                    }
                                                    for (String n : names) {
                                                        if (! cachedNames.containsKey(clazz)) {
                                                            cachedNames.put(clazz, n);
                                                        }
                                                        Class<?> c = extensionClasses.get(n);
                                                        if (c == null) {
                                                            extensionClasses.put(n, clazz);
                                                        } else if (c != clazz) {
                                                            throw new IllegalStateException("Duplicate extension " + type.getName() + " name " + n + " on " + c.getName() + " and " + clazz.getName());
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } catch (Throwable t) {
                                    IllegalStateException e = new IllegalStateException("Failed to load extension class(interface: " + type + ", class line: " + line + ") in " + url + ", cause: " + t.getMessage(), t);
                                    exceptions.put(line, e);
                                }
                            }
                        } // end of while read lines
                    } finally {
                        reader.close();
                    }
                } catch (Throwable t) {
                    logger.error("Exception when load extension class(interface: " +
                                        type + ", class file: " + url + ") in " + url, t);
                }
            } // end of while urls
        }
    } catch (Throwable t) {
        logger.error("Exception when load extension class(interface: " +
                type + ", description file: " + fileName + ").", t);
    }
}
```
截止到目前，我们已经把基于Protocol的自适应扩展点看完了。也明白最终这句话应该返回的对象是什么了。  
Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class). getAdaptiveExtension();  
也就是，这段代码中，最终的protocol应该等于= Protocol$Adaptive 
#### 暴露服务时序
![](https://github.com/YufeizhangRay/image/blob/master/Dubbo/%E8%B0%83%E7%94%A8%E7%BB%93%E6%9E%842.jpeg)  
  
injectExtension  
简单来说，这个方法的作用，是为这个自适应扩展点进行依赖注入。类似于spring里面的依赖注入功能。为适配器类的setter方法插入其他扩展点或实现。  
![](https://github.com/YufeizhangRay/image/blob/master/Dubbo/inject.jpeg)  
  
这里可以看到，扩展点自动注入的一句就是根据 setter 方法对应的参数类 型和 property 名称从 ExtensionFactory 中查询，如果有返回扩展点实例， 那么就进行注入操作。到这里 getAdaptiveExtension 方法就分析完毕了。  

#### 服务发布  
    
ServiceBean在初始化的时候会调afterPropertiesSet方法，其中又调用了父类ServiceConfig的export方法。  
export->doExport->doExportUrls  
其中doExportUrls有如下代码：  
```
//通过 proxyFactory 来获取 Invoker 对象
Invoker<?> invoker = proxyFactory.getInvoker(ref, (Class)
interfaceClass,
registryURL.addParameterAndEncoded(Constants.EXPORT_KEY,
 
url.toFullString()));
//注册服务
Exporter<?> exporter = protocol.export(invoker);
//将 exporter 添加到 list 中
exporters.add(exporter);
```
在上面这段代码中可以看到 Dubbo 的比较核心的抽象:Invoker  
Invoker 是一个代理类，从 ProxyFactory 中生成。  
这个地方可以做一个小结  
>1. Invoker - 执行具体的远程调用  
>2. Protocol – 服务地址的发布和订阅  
>3. Exporter – 暴露服务或取消暴露  

protocol 这个地方，其实并不是直接调用 DubboProtocol 协议的 export，实际上这个 Protocol 得到的应该是一个 Protocol$Adaptive。一个自适应的适配器。这个时候，通过 protocol.export(invoker),实际上调用的应该是 Protocol$Adaptive 这个动态类的 export 方法。此方法含有如下代码：  
```
com.alibaba.dubbo.rpc.Protocol extension =
(com.alibaba.dubbo.rpc.Protocol)
ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.rpc.Protocol.class)
.getExtension(extName);
return extension.export(arg0);
```
可以获得一个具体的类，例如当 extName 为 registry 的时候，我们可以定位到 RegistryProtocolRegistryProtocol 好这个类中的 export 方法。  
```
public <T> Exporter<T> export(final Invoker<T> originInvoker)
throws RpcException {
final ExporterChangeableWrapper<T> exporter =
doLocalExport(originInvoker);//本地发布服务(启动 netty)
final Registry registry = getRegistry(originInvoker);//服务注册
......
```
doLocalExport  
```
exporter = new ExporterChangeableWrapper<T>((Exporter<T>)
protocol.export(invokerDelegete), originInvoker);
```
protocol在 injectExtension 方法中针对自适应扩展点已经进行了依赖注入。  
protocol 是一个自适应扩展点 Protocol$Adaptive，调用这个自适应扩展点中的 export 方法，这个时候传入的协议地址应该是dubbo://127.0.0.1/xxxx... 但是在 Protocol$Adaptive.export 方法中，ExtensionLoader.getExtension(Protocol.class).getExtension 并不是基于 DubboProtocol 协议去发布服务，因为这里并不是获得一个单纯的 DubboProtocol 扩展点，而是通过 Wrapper 对 Protocol 进行了装饰，装饰器分别为:ProtocolFilterWrapper/ ProtocolListenerWrapper;   
  
ProtocolFilterWrapper  
这个类非常重要，dubbo 机制里面日志记录、超时等等功能都是在这一部分实现的  
这个类有 3 个特点  
>1.它有一个参数为 Protocol protocol 的构造函数;  
>2.它实现了 Protocol 接口;  
>3.它使用责任链模式，对 export 和 refer 函数进行了封装  
  
现在我们能够定位到 DubboProtocol.export(invoker) 方法，从invoker中获取到url，再调用openServer(url)方法来暴露服务。底层最终通过 NettyTranport 创建基于 Netty 的 server 服务。  
![](https://github.com/YufeizhangRay/image/blob/master/Dubbo/%E6%9C%8D%E5%8A%A1%E6%9A%B4%E9%9C%B2.jpeg)  
  
#### 服务注册  
  
```
private Registry getRegistry(final Invoker<?> originInvoker){
URL registryUrl = originInvoker.getUrl(); //获得registry://192.168.11.156:2181 的协议地址
if(Constants.REGISTRY_PROTOCOL.equals(registryUrl.getProtocol())) {//得到 zookeeper 的协议地址
String protocol = registryUrl.getParameter(Constants.REGISTRY_KEY, Constants.DEFAULT_DIRECTORY);
//registryUrl 就会变成了 zookeeper://192.168.11.156
registryUrl =registryUrl.setProtocol(protocol).removeParameter(Constants.REGIST RY_KEY);
}
return registryFactory.getRegistry(registryUrl); 
}
```
RegistryFactory 这个类的定义是一个扩展点，所以这个自适应适配器应该是 RegistryFactory$Adaptive。  
我们拿到这个动态生成的自适应扩展点，看看这段代码里面的实现  
>1. 从 url 中拿到协议头信息，这个时候的协议头是 zookeeper://  
>2. 通过ExtensionLoader.getExtensionLoader(RegistryFactory.class).getExtension(“zookeeper”)去获得一个指定的扩展点，得到一个 ZookeeperRegistryFactory
```
 public class RegistryFactory$Adaptive implements com.alibaba.dubbo.registry.RegistryFactory {
 public com.alibaba.dubbo.registry.Registry getRegistry(com.alibaba.dubbo.common.URL arg0) {
 if (arg0 == null) throw new IllegalArgumentException("url == null");
 com.alibaba.dubbo.common.URL url = arg0;
 String extName = (url.getProtocol() == null ? "dubbo" : url.getProtocol());
 if (extName == null)
 throw new IllegalStateException("Fail to get extension(com.alibaba.dubbo.registry.RegistryFactory) " +
 "name from url(" + url.toString() + ") use keys([protocol])");
 com.alibaba.dubbo.registry.RegistryFactory extension = (com.alibaba.dubbo.registry.RegistryFactory)
 ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.registry.RegistryFactory.class).
 getExtension(extName); 
     return extension.getRegistry(arg0);
 }
 }
 ```
这个方法中并没有 getRegistry 方法，而是在父类 AbstractRegistryFactory  
>1. 从缓存 REGISTRIES 中，根据 key 获得对应的 Registry  
>2. 如果不存在，则创建Registry  
```
 public Registry getRegistry(URL url) {
 url = url.setPath(RegistryService.class.getName())
 .addParameter(Constants.INTERFACE_KEY,RegistryService.class.getName())
 .removeParameters(Constants.EXPORT_KEY,Constants.REFER_KEY);
 String key = url.toServiceString();// 锁定注册中心获取过程，保证注册中心单一实例
 LOCK.lock();
 try {
 Registry registry = REGISTRIES.get(key);
 if (registry != null) {
 return registry;
 }
    registry = createRegistry(url);
 if (registry == null) {
 throw new IllegalStateException("Can not createregistry " + url);
 }
 REGISTRIES.put(key, registry);
 return registry;
 } finally {
 // 释放锁
 LOCK.unlock();
 }
 }
 ```
createRegistry  
创建一个注册中心，这个是一个抽象方法，具体的实现在对应的子类实例中实现的，在 ZookeeperRegistryFactory 中
```
public Registry createRegistry(URL url) {
 return new ZookeeperRegistry(url, zookeeperTransporter);
 }
```
通过 zkClient，获得一个 zookeeper 的连接实例
```
public ZookeeperRegistry(URL url, ZookeeperTransporter zookeeperTransporter) {
super(url);
if (url.isAnyHost()) {
throw new IllegalStateException("registry address == null"); }
String group = url.getParameter(Constants.GROUP_KEY, DEFAULT_ROOT);
if (! group.startsWith(Constants.PATH_SEPARATOR)) { group = Constants.PATH_SEPARATOR + group;
}
this.root = group; //设置根节点
zkClient = zookeeperTransporter.connect(url);//建立连接
zkClient.addStateListener(new StateListener() { public void stateChanged(int state) {
if (state == RECONNECTED) { try {
recover();
} catch (Exception e) {
logger.error(e.getMessage(), e); }
}
 } });
}
```
代码分析到这里，我们对于 getRegistry 得出了一个结论，根据当前注册中心的配置信息，获得一个匹配的注册中心，也就是 ZookeeperRegistry 
  
registry.register(registedProviderUrl);  
继续往下分析，会调用 registry.register 去将 dubbo://的协议地址注册到 zookeeper上。  
这个方法会调用 FailbackRegistry 类中的 register。因为 ZookeeperRegistry 这个类中并没有 register 这个方法，但是他的父类 FailbackRegistry 中存在 register 方法，而这个类又重写了 AbstractRegistry 类中的 register 方法。所以我们可以直接定位大 FailbackRegistry 这个类中的 register 方法中。  
FailbackRegistry.register  
>1. FailbackRegistry，从名字上来看，是一个失败重试机制  
>2. 调用父类的register方法，讲当前url添加到缓存集合中  
>3. 调用 doRegister 方法，这个方法很明显，是一个抽象方法，会由ZookeeperRegistry 子类实现  
```
@Override
public void register(URL url) {
     super.register(url);
 failedRegistered.remove(url);
 failedUnregistered.remove(url);
 try {
 // 向服务器端发送注册请求
 doRegister(url);
 } catch (Exception e) {
 ......
 ```
ZookeeperRegistry.doRegister  
调用 zkclient.create 在 zookeeper 中创建一个节点。
```
 protected void doRegister(URL url) {
 try {
 zkClient.create(toUrlPath(url),url.getParameter(Constants.DYNAMIC_KEY, true));
 } catch (Throwable e) {
 throw new RpcException("Failed to register " + url + " to zookeeper " + getUrl() + ", cause: " + e.getMessage(), e);  
     }
 }
```
#### 消费端初始化   
  
消费端的代码解析是从下面这段代码开始的  
<dubbo:reference id="xxxService" interface="xxx.xxx.Service"/>
ReferenceBean(afterPropertiesSet) ->getObject() ->get()->init()->createProxy 最终会获得一个代理对象。  
createProxy第375行  
前面很多代码都是初始化的动作，需要仔细分析的代码代码从createProxy第375行开始  
```
List<URL> us = loadRegistries(false); //从注册中心上获得相应的协议url地址
if (us != null && us.size() > 0) {
       for (URL u : us) {
           URL monitorUrl = loadMonitor(u); 
           if (monitorUrl != null) {
               map.put(Constants.MONITOR_KEY, URL.encode(monitorUrl.toFullString()));
           }
           urls.add(u.addParameterAndEncoded(Constants.REFER_KEY, StringUtils.toQueryString(map)));
       }
}
if (urls == null || urls.size() == 0) {
       throw new IllegalStateException("No such any registry to reference " + interfaceName  + " on the consumer " + NetUtils.getLocalHost() + " use dubbo version " + Version.getVersion() + ", please config <dubbo:registry address=\"...\" /> to your spring config.");
   }
if (urls.size() == 1) {
    invoker = refprotocol.refer(interfaceClass, urls.get(0)); //获得invoker代理对象
} else {
    List<Invoker<?>> invokers = new ArrayList<Invoker<?>>();
    URL registryURL = null;
    for (URL url : urls) {
        invokers.add(refprotocol.refer(interfaceClass, url));
        if (Constants.REGISTRY_PROTOCOL.equals(url.getProtocol())) {
            registryURL = url; // 用了最后一个registry url
        }
    }
    if (registryURL != null) { // 有 注册中心协议的URL
        // 对有注册中心的Cluster 只用 AvailableCluster
        URL u = registryURL.addParameter(Constants.CLUSTER_KEY, AvailableCluster.NAME); 
        invoker = cluster.join(new StaticDirectory(u, invokers));
    }  else { // 不是 注册中心的URL
        invoker = cluster.join(new StaticDirectory(invokers));
    }
}
```
refprotocol.refer  
refprotocol这个对象，定义的代码如下，是一个自适应扩展点，得到的是Protocol$Adaptive。  
Protocol refprotocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();  
直接找到Protocol$Adaptive代码中的refer代码块如下  
```
    public com.alibaba.dubbo.rpc.Invoker refer(java.lang.Class arg0, com.alibaba.dubbo.common.URL arg1) throws com.alibaba.dubbo.rpc.RpcException {
        if (arg1 == null) throw new IllegalArgumentException("url == null");
        com.alibaba.dubbo.common.URL url = arg1;
        String extName = (url.getProtocol() == null ? "dubbo" : url.getProtocol());
        if (extName == null)
            throw new IllegalStateException("Fail to get extension(com.alibaba.dubbo.rpc.Protocol) name from url(" + url.toString() + ") use keys([protocol])");
        com.alibaba.dubbo.rpc.Protocol extension = (com.alibaba.dubbo.rpc.Protocol) ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.rpc.Protocol.class).getExtension(extName);
        return extension.refer(arg0, arg1);
    }
```
这段代码中，根据当前的协议url，得到一个指定的扩展点，传递进来的参数中，协议地址为registry://，所以，我们可以直接定位到RegistryProtocol.refer代码。 
  
RegistryProtocol.refer  
这个方法里面的代码，基本上都能看懂  
>1.根据根据url获得注册中心，这个registry是zookeeperRegistry  
>2.调用doRefer，按方法，传递了几个参数， 其中有一个culster参数，这个需要注意下
```
public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
       url = url.setProtocol(url.getParameter(Constants.REGISTRY_KEY, Constants.DEFAULT_REGISTRY)).removeParameter(Constants.REGISTRY_KEY);
       Registry registry = registryFactory.getRegistry(url);
       if (RegistryService.class.equals(type)) {
           return proxyFactory.getInvoker((T) registry, type, url);
       }
       // group="a,b" or group="*"
       Map<String, String> qs = StringUtils.parseQueryString(url.getParameterAndDecoded(Constants.REFER_KEY));
       String group = qs.get(Constants.GROUP_KEY);
       if (group != null && group.length() > 0 ) {
           if ( ( Constants.COMMA_SPLIT_PATTERN.split( group ) ).length > 1
                   || "*".equals( group ) ) {
               return doRefer( getMergeableCluster(), registry, type, url );
           }
       }
       return doRefer(cluster, registry, type, url);
   }
```
cluster   
doRefer方法中有一个参数是cluster,又是一个自动注入的扩展点。  
从下面的代码可以看出，这个不仅仅是一个扩展点，而且方法层面上，还有一个@Adaptive，表示会动态生成一个自适应适配器Cluster$Adaptive
```
@SPI(FailoverCluster.NAME)
public interface Cluster {

    /**
     * Merge the directory invokers to a virtual invoker.
     * 
     * @param <T>
     * @param directory
     * @return cluster invoker
     * @throws RpcException
     */
    @Adaptive
    <T> Invoker<T> join(Directory<T> directory) throws RpcException;

}
```
Cluster$Adaptive  
我们知道cluster这个对象的实例以后，继续看doRefer方法；  
注意：这里的Cluster$Adaptive也并不单纯，如果这个扩展点存在一个构造函数，并且构造函数就是扩展接口本身，那么这个扩展点就会这个wrapper装饰，而Cluster被装饰的是：MockClusterWrapper  
```
public class Cluster$Adaptive implements com.alibaba.dubbo.rpc.cluster.Cluster {
    public com.alibaba.dubbo.rpc.Invoker join(com.alibaba.dubbo.rpc.cluster.Directory arg0) throws com.alibaba.dubbo.rpc.RpcException {
        if (arg0 == null)
            throw new IllegalArgumentException("com.alibaba.dubbo.rpc.cluster.Directory argument == null");
        if (arg0.getUrl() == null)
            throw new IllegalArgumentException("com.alibaba.dubbo.rpc.cluster.Directory argument getUrl() == null");
        com.alibaba.dubbo.common.URL url = arg0.getUrl();
        String extName = url.getParameter("cluster", "failover");
        if (extName == null)
            throw new IllegalStateException("Fail to get extension(com.alibaba.dubbo.rpc.cluster.Cluster) name from url(" + url.toString() + ") use keys([cluster])");
        com.alibaba.dubbo.rpc.cluster.Cluster extension = (com.alibaba.dubbo.rpc.cluster.Cluster) ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.rpc.cluster.Cluster.class).getExtension(extName);
        return extension.join(arg0);
    }
}
```
RegistryProtocol.doRefer  
>1.将consumer://协议地址注册到注册中心  
>2.订阅zookeeper地址的变化  
>3.调用cluster.join()方法  
```
private <T> Invoker<T> doRefer(Cluster cluster, Registry registry, Class<T> type, URL url) {
    RegistryDirectory<T> directory = new RegistryDirectory<T>(type, url);
    directory.setRegistry(registry);
    directory.setProtocol(protocol);
    URL subscribeUrl = new URL(Constants.CONSUMER_PROTOCOL, NetUtils.getLocalHost(), 0, type.getName(), directory.getUrl().getParameters());
    if (! Constants.ANY_VALUE.equals(url.getServiceInterface())
            && url.getParameter(Constants.REGISTER_KEY, true)) {
        registry.register(subscribeUrl.addParameters(Constants.CATEGORY_KEY, Constants.CONSUMERS_CATEGORY,
                Constants.CHECK_KEY, String.valueOf(false)));
    }
    directory.subscribe(subscribeUrl.addParameter(Constants.CATEGORY_KEY, 
            Constants.PROVIDERS_CATEGORY 
            + "," + Constants.CONFIGURATORS_CATEGORY 
            + "," + Constants.ROUTERS_CATEGORY));
    return cluster.join(directory);
}
```
cluster.join  
由Cluster$Adaptive这个类中的join方法的分析，可知cluster.join会调用MockClusterWrapper.join方法，然后再调用FailoverCluster.join方法。
  
MockClusterWrapper.join  
这个意思很明显，是mock容错机制，如果出现异常情况，会调用MockClusterInvoker，否则，调用FailoverClusterInvoker.
```
public class MockClusterWrapper implements Cluster {

   private Cluster cluster;

   public MockClusterWrapper(Cluster cluster) {
      this.cluster = cluster;
   }

   public <T> Invoker<T> join(Directory<T> directory) throws RpcException {
      return new MockClusterInvoker<T>(directory,
            this.cluster.join(directory));
   }
}
```
所以refprotocol.ref这个方法，会返回一个MockClusterInvoker(FailoverClusterInvoker)。  
  
proxyFactory.getProxy(invoker);  
再回到ReferenceConfig这个类，在createProxy方法的最后一行，调用proxyFactory.getProxy(invoker). 把前面生成的invoker对象作为参数，再通过proxyFactory工厂去获得一个代理对象。  
ProxyFactory，会生成一个动态的自适应适配器。ProxyFactory$Adaptive，然后调用这个适配器中的getProxy方法  
```
public java.lang.Object getProxy(com.alibaba.dubbo.rpc.Invoker arg0) throws com.alibaba.dubbo.rpc.RpcException {
        if (arg0 == null) throw new IllegalArgumentException("com.alibaba.dubbo.rpc.Invoker argument == null");
        if (arg0.getUrl() == null) throw new IllegalArgumentException("com.alibaba.dubbo.rpc.Invoker argument getUrl() == null");com.alibaba.dubbo.common.URL url = arg0.getUrl();
        String extName = url.getParameter("proxy", "javassist");
        if(extName == null) throw new IllegalStateException("Fail to get extension(com.alibaba.dubbo.rpc.ProxyFactory) name from url(" + url.toString() + ") use keys([proxy])");
        com.alibaba.dubbo.rpc.ProxyFactory extension = (com.alibaba.dubbo.rpc.ProxyFactory)ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.rpc.ProxyFactory.class).getExtension(extName);
        return extension.getProxy(arg0);
    }
```
通过javassist实现的一个动态代理。  
  
JavassistProxyFactory.getProxy  
通过javasssist动态字节码生成动态代理类，
```
public <T> T getProxy(Invoker<T> invoker, Class<?>[] interfaces) {
    return (T) Proxy.getProxy(interfaces).newInstance(new InvokerInvocationHandler(invoker));
}
```
proxy.getProxy(interfaces)  
在Proxy.getProxy这个类的如下代码中添加断点，在debug下可以看到动态字节码如下
```
public java.lang.String sayHello(java.lang.String arg0){
  Object[] args = new Object[1]; 
  args[0] = ($w)$1; 
  Object ret = handler.invoke(this, methods[0], args); 
return (java.lang.String)ret;
}
```
handler.invoke(this, methods[0], args)就是在JavassistProxyFactory.getProxy中传递的new InvokerInvocationHandler(invoker)。  

#### 建立和服务端的连接  
  
前面我们通过代码分析到了，消费端的初始化过程，但是似乎没有看到客户端和服务端建立NIO连接。实际上，建立连接的过程在消费端初始化的时候就建立好的。RegistryProtocol.doRefer方法内的directory.suscribe方法中。
  
directory.subscribe  
调用链为： RegistryDirectory.subscribe ->FailbackRegistry. subscribe->AbstractRegistry.subscribe>zookeeperRegistry.doSubscribe
```
public void subscribe(URL url) {
    setConsumerUrl(url);
    registry.subscribe(url, this);
}
```
FailbackRegistry. subscribe  
调用FailbackRegistry.subscribe 进行订阅，这里有一个特殊处理，如果订阅失败，则会添加到定时任务中进行重试
```
@Override
public void subscribe(URL url, NotifyListener listener) {
    super.subscribe(url, listener);
    removeFailedSubscribed(url, listener);
    try {
        // 向服务器端发送订阅请求
        doSubscribe(url, listener);
    ......
```
zookeeperRegistry.doSubscribe  
调用zookeeperRegistry执行真正的订阅操作，这里面主要做两个操作
>1.对providers/routers/configurator三个节点进行创建和监听  
>2.调用notify(url,listener,urls) 将已经可用的列表进行通知  

AbstractRegistry.notify  
```
protected void notify(URL url, NotifyListener listener, List<URL> urls) {
    if (url == null) {
        throw new IllegalArgumentException("notify url == null");
    }
    if (listener == null) {
        throw new IllegalArgumentException("notify listener == null");
    }
    if ((urls == null || urls.size() == 0) 
            && ! Constants.ANY_VALUE.equals(url.getServiceInterface())) {
        logger.warn("Ignore empty notify urls for subscribe url " + url);
        return;
    }
    if (logger.isInfoEnabled()) {
        logger.info("Notify urls for subscribe url " + url + ", urls: " + urls);
    }
    Map<String, List<URL>> result = new HashMap<String, List<URL>>();
    for (URL u : urls) {
        if (UrlUtils.isMatch(url, u)) {
           String category = u.getParameter(Constants.CATEGORY_KEY, Constants.DEFAULT_CATEGORY);
           List<URL> categoryList = result.get(category);
           if (categoryList == null) {
              categoryList = new ArrayList<URL>();
              result.put(category, categoryList);
           }
           categoryList.add(u);
        }
    }
    if (result.size() == 0) {
        return;
    }
    Map<String, List<URL>> categoryNotified = notified.get(url);
    if (categoryNotified == null) {
        notified.putIfAbsent(url, new ConcurrentHashMap<String, List<URL>>());
        categoryNotified = notified.get(url);
    }
    for (Map.Entry<String, List<URL>> entry : result.entrySet()) {
        String category = entry.getKey();
        List<URL> categoryList = entry.getValue();
        categoryNotified.put(category, categoryList);
        saveProperties(url);
        listener.notify(categoryList);
    }
}
```
#### 引用服务时序图
![](https://github.com/YufeizhangRay/image/blob/master/Dubbo/%E6%B6%88%E8%B4%B9%E5%88%9D%E5%A7%8B%E5%8C%96.jpeg)  
  
#### 消费端调用过程 
![](https://github.com/YufeizhangRay/image/blob/master/Dubbo/%E6%B6%88%E8%B4%B9%E7%AB%AF%E8%B0%83%E7%94%A8%E8%BF%87%E7%A8%8B.jpeg)  
  
#### 服务端接收消息处理过程  
  
NettyHandler.messageReceived  
接收消息的时候，通过NettyHandler.messageReceived作为入口。  
```
@Override
public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
    NettyChannel channel = NettyChannel.getOrAddChannel(ctx.getChannel(), url, handler);
    try {
        handler.received(channel, e.getMessage());
    } finally {
        NettyChannel.removeChannelIfDisconnected(ctx.getChannel());
    }
}
```
handler.received  
在服务发布的时候，组装了一系列的handler
```
HeaderExchanger.bind
public ExchangeServer bind(URL url, ExchangeHandler handler) throws RemotingException {
    return new HeaderExchangeServer(Transporters.bind(url, new DecodeHandler(new HeaderExchangeHandler(handler))));
}
```
NettyServer  
接着又在Nettyserver中，wrap了多个handler  
```
public NettyServer(URL url, ChannelHandler handler) throws RemotingException {
    super(url, ChannelHandlers.wrap(handler, ExecutorUtil.setThreadName(url, SERVER_THREAD_POOL_NAME)));
}
protected ChannelHandler wrapInternal(ChannelHandler handler, URL url) {
    return new MultiMessageHandler(new HeartbeatHandler(ExtensionLoader.getExtensionLoader(Dispatcher.class)
            .getAdaptiveExtension().dispatch(handler, url)));
}
```
所以服务端的handler处理链为  
MultiMessageHandler(HeartbeatHandler(AllChannelHandler(DecodeHandler)))  
MultiMessageHandler: 复合消息处理  
HeartbeatHandler：心跳消息处理，接收心跳并发送心跳响应  
AllChannelHandler：业务线程转化处理器，把接收到的消息封装成ChannelEventRunnable可执行任务给线程池处理  
DecodeHandler:业务解码处理器  
  
HeaderExchangeHandler.received  
交互层请求响应处理，有三种处理方式  
>1.handlerRequest，双向请求  
>2.handler.received 单向请求  
>3.handleResponse 响应消息  
```
public void received(Channel channel, Object message) throws RemotingException {
    channel.setAttribute(KEY_READ_TIMESTAMP, System.currentTimeMillis());
    ExchangeChannel exchangeChannel = HeaderExchangeChannel.getOrAddChannel(channel);
    try {
        if (message instanceof Request) {
            // handle request.
            Request request = (Request) message;
            if (request.isEvent()) {
                handlerEvent(channel, request);
            } else {
                if (request.isTwoWay()) {
                    Response response = handleRequest(exchangeChannel, request);
                    channel.send(response);
                } else {
                    handler.received(exchangeChannel, request.getData());
                }
            }
        } else if (message instanceof Response) {
            handleResponse(channel, (Response) message);
        } else if (message instanceof String) {
            if (isClientSide(channel)) {
                Exception e = new Exception("Dubbo client can not supported string message: " + message + " in channel: " + channel + ", url: " + channel.getUrl());
                logger.error(e.getMessage(), e);
            } else {
                String echo = handler.telnet(channel, (String) message);
                if (echo != null && echo.length() > 0) {
                    channel.send(echo);
                }
            }
        } else {
            handler.received(exchangeChannel, message);
        }
    } finally {
        HeaderExchangeChannel.removeChannelIfDisconnected(channel);
    }
}
```
handleRequest  
处理请求并返回response  
```
Response handleRequest(ExchangeChannel channel, Request req) throws RemotingException {
    Response res = new Response(req.getId(), req.getVersion());
    if (req.isBroken()) {
        Object data = req.getData();
        String msg;
        if (data == null) msg = null;
        else if (data instanceof Throwable) msg = StringUtils.toString((Throwable) data);
        else msg = data.toString();
        res.setErrorMessage("Fail to decode request due to: " + msg);
        res.setStatus(Response.BAD_REQUEST);

        return res;
    }
    // find handler by message class.
    Object msg = req.getData();
    try {
        // handle data.
        Object result = handler.reply(channel, msg);
        res.setStatus(Response.OK);
        res.setResult(result);
    } catch (Throwable e) {
        res.setStatus(Response.SERVICE_ERROR);
        res.setErrorMessage(StringUtils.toString(e));
    }
    return res;
}
```
ExchangeHandlerAdaptive.replay(DubboProtocol)   
调用DubboProtocol中定义的ExchangeHandlerAdaptive.replay方法处理消息   
```
private ExchangeHandler requestHandler = new ExchangeHandlerAdapter() {
    
    public Object reply(ExchangeChannel channel, Object message) throws RemotingException {
     invoker.invoke(inv);
}
```
在RegistryDirectory中发布本地方法的时候通过InvokerDelegete对原本的invoker做了一层包装，而原本的invoker是一个JavassistProxyFactory生成的动态代理吧。所以此处的invoker应该是  
Filter(Listener(InvokerDelegete(AbstractProxyInvoker (Wrapper.invokeMethod)))    
RegistryDirectory生成invoker的代码如下  
```
private <T> ExporterChangeableWrapper<T>  doLocalExport(final Invoker<T> originInvoker){
    String key = getCacheKey(originInvoker);
    ExporterChangeableWrapper<T> exporter = (ExporterChangeableWrapper<T>) bounds.get(key);
    if (exporter == null) {
        synchronized (bounds) {
            exporter = (ExporterChangeableWrapper<T>) bounds.get(key);
            if (exporter == null) {
                final Invoker<?> invokerDelegete = new InvokerDelegete<T>(originInvoker, getProviderUrl(originInvoker));
                exporter = new ExporterChangeableWrapper<T>((Exporter<T>)protocol.export(invokerDelegete), originInvoker);
                bounds.put(key, exporter);
            }
        }
    }
    return (ExporterChangeableWrapper<T>) exporter;
}
```
Directory  
集群目录服务Directory，代表多个Invoker，可以看成List<Invoker>,它的值可能是动态变化的比如注册中心推送变更。    
集群选择调用服务时通过目录服务找到所有服务。    
>StaticDirectory: 静态目录服务，它的所有Invoker通过构造函数传入，服务消费方引用服务的时候，服务对多注册中心的引用，将Invokers集合直接传入   StaticDirectory构造器，再由Cluster伪装成一个Invoker；StaticDirectory的list方法直接返回所有invoker集合  
>RegistryDirectory: 注册目录服务，它的Invoker集合是从注册中心获取的，它实现了NotifyListener接口实现了回调接口notify(List<Url>)  
  
Directory目录服务的更新过程  
RegistryProtocol.doRefer方法，也就是消费端在初始化的时候，这里涉及到了RegistryDirectory这个类。然后执行cluster.join(directory)方法。  
cluster.join其实就是将Directory中的多个Invoker伪装成一个Invoker, 对上层透明，包含集群的容错机制
```
private <T> Invoker<T> doRefer(Cluster cluster, Registry registry, Class<T> type, URL url) {
    RegistryDirectory<T> directory = new RegistryDirectory<T>(type, url);//对多个invoker进行组装
    directory.setRegistry(registry); //ZookeeperRegistry
    directory.setProtocol(protocol); //protocol=Protocol$Adaptive
    //url=consumer://192.168.111....
    URL subscribeUrl = new URL(Constants.CONSUMER_PROTOCOL, NetUtils.getLocalHost(), 0, type.getName(), directory.getUrl().getParameters());
    //会把consumer://192...  注册到注册中心
    if (! Constants.ANY_VALUE.equals(url.getServiceInterface())
            && url.getParameter(Constants.REGISTER_KEY, true)) {
        //zkClient.create()
        registry.register(subscribeUrl.addParameters(Constants.CATEGORY_KEY, Constants.CONSUMERS_CATEGORY,
                Constants.CHECK_KEY, String.valueOf(false)));
    }
    directory.subscribe(subscribeUrl.addParameter(Constants.CATEGORY_KEY, 
            Constants.PROVIDERS_CATEGORY 
            + "," + Constants.CONFIGURATORS_CATEGORY 
            + "," + Constants.ROUTERS_CATEGORY));
    //Cluster$Adaptive
    return cluster.join(directory);
}
```
directory.subscribe  
订阅节点的变化  
>1.当zookeeper上指定节点发生变化以后，会通知到RegistryDirectory的notify方法  
>2.将url转化为invoker对象  
  
调用过程中invokers的使用  
再调用过程中，AbstractClusterInvoker.invoke方法中 
```
public Result invoke(final Invocation invocation) throws RpcException {

    checkWhetherDestroyed();

    LoadBalance loadbalance;
    
    List<Invoker<T>> invokers = list(invocation); 
    if (invokers != null && invokers.size() > 0) {
        loadbalance = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(invokers.get(0).getUrl()
                .getMethodParameter(invocation.getMethodName(),Constants.LOADBALANCE_KEY, Constants.DEFAULT_LOADBALANCE));
    } else {
        loadbalance = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(Constants.DEFAULT_LOADBALANCE);
    }
    RpcUtils.attachInvocationIdIfAsync(getUrl(), invocation);
    return doInvoke(invocation, invokers, loadbalance);
}
```
list方法   
从directory中获得invokers  
```
protected  List<Invoker<T>> list(Invocation invocation) throws RpcException {
   List<Invoker<T>> invokers = directory.list(invocation);
   return invokers;
}
```
负载均衡LoadBalance  
LoadBalance负载均衡，负责从多个Invokers中选出具体的一个Invoker用于本次调用，调用过程中包含了负载均衡的算法。  
负载均衡代码访问入口  
在AbstractClusterInvoker.invoke中代码如下，通过名称获得指定的扩展点。    
```
public Result invoke(final Invocation invocation) throws RpcException {

    checkWhetherDestroyed();

    LoadBalance loadbalance;
    
    List<Invoker<T>> invokers = list(invocation);
    if (invokers != null && invokers.size() > 0) {
        loadbalance = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(invokers.get(0).getUrl()
                .getMethodParameter(invocation.getMethodName(),Constants.LOADBALANCE_KEY, Constants.DEFAULT_LOADBALANCE));
    } else {
        loadbalance = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(Constants.DEFAULT_LOADBALANCE);
    }
    RpcUtils.attachInvocationIdIfAsync(getUrl(), invocation);
    return doInvoke(invocation, invokers, loadbalance);
}
```
AbstractClusterInvoker.doselect  
调用LoadBalance.select方法，讲invokers按照指定算法进行负载  
```
private Invoker<T> doselect(LoadBalance loadbalance, Invocation invocation, List<Invoker<T>> invokers, List<Invoker<T>> selected) throws RpcException {
    if (invokers == null || invokers.size() == 0)
        return null;
    if (invokers.size() == 1)
        return invokers.get(0);
    // 如果只有两个invoker，退化成轮循
    if (invokers.size() == 2 && selected != null && selected.size() > 0) {
        return selected.get(0) == invokers.get(0) ? invokers.get(1) : invokers.get(0);
    }
    Invoker<T> invoker = loadbalance.select(invokers, getUrl(), invocation);
    
    //如果 selected中包含（优先判断） 或者 不可用&&availablecheck=true 则重试.
    if( (selected != null && selected.contains(invoker))
            ||(!invoker.isAvailable() && getUrl()!=null && availablecheck)){
        try{
            Invoker<T> rinvoker = reselect(loadbalance, invocation, invokers, selected, availablecheck);
            if(rinvoker != null){
                invoker =  rinvoker;
            }else{
                //看下第一次选的位置，如果不是最后，选+1位置.
                int index = invokers.indexOf(invoker);
                try{
                    //最后在避免碰撞
                    invoker = index <invokers.size()-1?invokers.get(index+1) :invoker;
                }catch (Exception e) {
                    logger.warn(e.getMessage()+" may because invokers list dynamic change, ignore.",e);
                }
            }
        }catch (Throwable t){
            logger.error("clustor relselect fail reason is :"+t.getMessage() +" if can not slove ,you can set cluster.availablecheck=false in url",t);
        }
    }
    return invoker;
} 
```
默认情况下，LoadBalance使用的是Random算法，但是这个随机和我们理解上的随机还是不一样的,因为他还有个概念叫weight(权重)。
假设有四个集群节点A,B,C,D,对应的权重分别是1,2,3,4,那么请求到A节点的概率就为1/(1+2+3+4) = 10%.B,C,D节点依次类推为20%,30%,40%.
```
protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
    int length = invokers.size(); // 总个数
    int totalWeight = 0; // 总权重
    boolean sameWeight = true; // 权重是否都一样
    for (int i = 0; i < length; i++) {
        int weight = getWeight(invokers.get(i), invocation);
        totalWeight += weight; // 累计总权重
        if (sameWeight && i > 0
                && weight != getWeight(invokers.get(i - 1), invocation)) {
            sameWeight = false; // 计算所有权重是否一样
        }
    }
    if (totalWeight > 0 && ! sameWeight) {
        // 如果权重不相同且权重大于0则按总权重数随机
        int offset = random.nextInt(totalWeight);
        // 并确定随机值落在哪个片断上
        for (int i = 0; i < length; i++) {
            offset -= getWeight(invokers.get(i), invocation);
            if (offset < 0) {
                return invokers.get(i);
            }
        }
    }
    // 如果权重相同或权重为0则均等随机
    return invokers.get(random.nextInt(length));
}
```
#### Dubbo整体设计  
![](https://github.com/YufeizhangRay/image/blob/master/Dubbo/%E6%95%B4%E4%BD%93%E8%AE%BE%E8%AE%A1.jpeg)  
  
#### 调用链  
![](https://github.com/YufeizhangRay/image/blob/master/Dubbo/%E8%B0%83%E7%94%A8%E9%93%BE.jpeg)
