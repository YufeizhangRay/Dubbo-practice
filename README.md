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
  
#### 5.部分源码  
  
Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class). getAdaptiveExtension();  
  
把上面这段代码分成两段，一段是getExtensionLoader、 另一段是getAdaptiveExtension。   
初步猜想一下:  
>第一段是通过一个Class参数去获得一个ExtensionLoader对象，有点类似一个工厂模式。  
第二段getAdaptiveExtension，去获得一个自适应的扩展点  
  
Extension源码的结构  
  
了解源码结构，建立一个全局认识。结构图如下
![](https://github.com/YufeizhangRay/image/blob/master/Dubbo/extention.jpeg)  
  
Protocol源码
以下是Protocol的源码，在这个源码中可以看到有两个注解，一个是在类级别上的@SPI(“dubbo”). 另一个是@Adaptive
@SPI 表示当前这个接口是一个扩展点，可以实现自己的扩展实现，默认的扩展点是DubboProtocol。
@Adaptive  表示一个自适应扩展点，在方法级别上，会动态生成一个适配器类

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
由上述源码可知，我们可以通过Protocol来发布暴露(export()方法)服务于引用消费(refer()方法)服务。  
  
getExtensionLoader
该方法需要一个Class类型的参数，该参数表示希望加载的扩展点类型，该参数必须是接口，且该接口必须被@SPI注解注释，否则拒绝处理。检查通过之后首先会检查ExtensionLoader缓存中是否已经存在该扩展对应的ExtensionLoader，如果有则直接返回，否则创建一个新的ExtensionLoader负责加载该扩展实现，同时将其缓存起来。可以看到对于每一个扩展，dubbo中只会有一个对应的ExtensionLoader实例

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
ExtensionLoader提供了一个私有的构造函数，并且在这里面对两个成员变量type/objectFactory进行赋值。
private ExtensionLoader(Class<?> type) {
    this.type = type;
    objectFactory = (type == ExtensionFactory.class ? null :
            ExtensionLoader.getExtensionLoader(ExtensionFactory.class).
                    getAdaptiveExtension());
}

getAdaptiveExtension
通过getExtensionLoader获得了对应的ExtensionLoader实例以后，再调用getAdaptiveExtension()方法来获得一个自适应扩展点。
ps：这个自适应扩展点实际上就是一个适配器。
这个方法里面主要做几个事情：
1.	从cacheAdaptiveInstance 这个内存缓存中获得一个对象实例
2.	如果实例为空，说明是第一次加载，则通过双重检查锁的方式去创建一个适配器扩展点
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

createAdaptiveExtension
这段代码里面有两个结构，一个是injectExtension.  另一个是getAdaptiveExtensionClass()
我们需要先去了解getAdaptiveExtensionClass这个方法做了什么？很显然，从后面的.newInstance来看，应该是获得一个类并且进行实例。

private T createAdaptiveExtension() {
    try {
        //可以实现扩展点的注入
        return injectExtension((T) getAdaptiveExtensionClass().newInstance());
    } catch (Exception e) {
        throw new IllegalStateException("Can not create adaptive extenstion " + type + ", cause: " + e.getMessage(), e);
    }
}

getAdaptiveExtensionClass
从类名来看，是获得一个适配器扩展点的类。
在这段代码中，做了两个事情
1.	getExtensionClasses() 加载所有路径下的扩展点
2.	createAdaptiveExtensionClass() 动态创建一个扩展点
cachedAdaptiveClass这里有个判断，用来判断当前Protocol这个扩展点是否存在一个自定义的适配器，如果有，则直接返回自定义适配器，否则，就动态创建，这个值是在getExtensionClasses中赋值的
private Class<?> getAdaptiveExtensionClass() {
    getExtensionClasses();
    //TODO  不一定？
    if (cachedAdaptiveClass != null) {
        return cachedAdaptiveClass;
    }
    return cachedAdaptiveClass = createAdaptiveExtensionClass();
}

createAdaptiveExtensionClass
动态生成适配器代码，以及动态编译
1.	createAdaptiveExtensionClassCode,  动态创建一个字节码文件。返回code这个字符串
2.	通过compiler.compile进行编译（默认情况下使用的是javassist）
3.	通过ClassLoader加载到jvm中
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
CODE的字节码内容
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
Protocol$Adaptive的主要功能 
1. 从url或扩展接口获取扩展接口实现类的名称； 
2.根据名称，获取实现类ExtensionLoader.getExtensionLoader(扩展接口类).getExtension(扩展接口实现类名称)，然后调用实现类的方法。
需要明白一点dubbo的内部传参基本上都是基于Url来实现的，也就是说Dubbo是基于URL驱动的技术
所以，适配器类的目的是在运行期获取扩展的真正实现来调用，解耦接口和实现，这样的话要不我们自己实现适配器类，要不dubbo帮我们生成，而这些都是通过Adpative来实现。
到目前为止，我们的AdaptiveExtension的主线走完了，可以简单整理一下他们的调用关系如下
![](https://github.com/YufeizhangRay/image/blob/master/Dubbo/%E8%B0%83%E7%94%A8%E7%BB%93%E6%9E%841.jpeg)  
  
我们再回过去梳理下代码，实际上在调用createAdaptiveExtensionClass之前，还做了一个操作。是执行getExtensionClasses方法，我们来看看这个方法做了什么事情
getExtensionClasses
getExtensionClasses这个方法，就是加载扩展点实现类了。这段代码本来应该先看的，但是担心先看这段代码会容易导致大家不好理解。我就把顺序交换了下
这段代码主要做如下几个事情
1.	从cachedClasses中获得一个结果，这个结果实际上就是所有的扩展点类，key对应name，value对应class
2.	通过双重检查锁进行判断
3.	调用loadExtensionClasses，去加载左右扩展点的实现
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

loadExtensionClasses
从不同目录去加载扩展点的实现，在最开始的时候讲到过的。META-INF/dubbo ；META-INF/internal ; META-INF/services
主要逻辑
1.	获得当前扩展点的注解，也就是Protocol.class这个类的注解，@SPI
2.	判断这个注解不为空，则再次获得@SPI中的value值
3.	如果value有值，也就是@SPI(“dubbo”)，则讲这个dubbo的值赋给cachedDefaultName。这就是为什么我们能够通过
ExtensionLoader.getExtensionLoader(Protocol.class).getDefaultExtension() ,能够获得DubboProtocol这个扩展点的原因
4.	最后，通过loadFile去加载指定路径下的所有扩展点。也就是META-INF/dubbo;META-INF/internal;META-INF/services

// 此方法已经getExtensionClasses方法同步过。
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

loadFile
解析指定路径下的文件，获取对应的扩展点，通过反射的方式进行实例化以后，put到extensionClasses这个Map集合中
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

                                        //判断是否有自定义适配类，如果有，则在前面讲过的获取适配类的时候，直接返回当前的自定义适配类，不需要再动态创建
// 还记得在前面讲过的getAdaptiveExtensionClass中有一个判断吗？是用来判断cachedAdaptiveClass是不是为空的。如果不为空，表示存在自定义扩展点。也就不会去动态生成字节码了。这个地方可以得到一个简单的结论；
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

阶段性小结
截止到目前，我们已经把基于Protocol的自适应扩展点看完了。也明白最终这句话应该返回的对象是什么了.
Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class). getAdaptiveExtension();
也就是，这段代码中，最终的protocol应该等于= Protocol$Adaptive 
![](https://github.com/YufeizhangRay/image/blob/master/Dubbo/%E8%B0%83%E7%94%A8%E7%BB%93%E6%9E%842.jpeg)  
  
injectExtension
简单来说，这个方法的作用，是为这个自适应扩展点进行依赖注入。类似于spring里面的依赖注入功能。为适配器类的setter方法插入其他扩展点或实现。
