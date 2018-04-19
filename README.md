# Aredis

#### **Aredis的目标：**
实现一个可部署在移动平台、支持自定义类型、对于数据库的读写均可以放在主线程去执行，保证数据的一致性<span style="background-color:rgb(250, 250, 250);"><span style="color:rgb(89, 89, 89);">的Key-Value数据库</span></span>。

#### **Aredis的特性：**
* 实时内存共享，支持持久化

* 使用Native Heap 存放数据，不占用任何 Java Heap

* 支持key超时设置、支持数据最大存放量设置(如10m、20m，超过最大限制使用lru策略进行淘汰)

* value支持java除void外所有8种基本类型、支持list列表、set集合、Serializable

* value支持自定义对象，同时支持配置自定义对象存取策略（fastjson、protostuff、自定义）


#### Aredis的模式：
1.**RDB模式**
Redis DataBase模式，简而言之，就是在不同的时间点，将Aredis存储的数据生成快照并存储到磁盘等介质上。
客户端每次调用接口存放数据时并不进行真正的数据写操作，而是等到了一定的条件才进行写操作。

**目前支持的触发数据同步的条件：**
* N次set操作 （如set数据50次、100次等）

* N秒后（如本次set操作距离上次时间间隔超过100秒、200秒等）

* 同步sync （客户端 主动调用sync接口 同步数据）

同redis一样 数据同步的任务执行在 另一个进程中（fork/vfork）

**RDB模式的优点：**
1. 对于客户端来说，每次数据变更实时生效，无需任何提交操作即可实现数据的共享（相比于preference和db）
2. 客户端线程每次提交数据，无任何IO操作
3. <span style="background-color:rgb(250, 250, 250);"><span style="color:rgb(89, 89, 89);">产生的数据同步request是在另一个进程去调度执行，数据同步的过程中aredis仍然可以对客户端提供服务，提高了aredis对于客户端的吞吐率。</span></span>

<span style="background-color:rgb(250, 250, 250);"><span style="color:rgb(89, 89, 89);">_**RDB模式的缺点：**_</span></span>
<span style="background-color:rgb(250, 250, 250);"><span style="color:rgb(89, 89, 89);">如果客户端不主动调用sync同步数据库，可能会出现丢数据的情况。比如设置每10次set操作同步一次RDB，当客户</span></span>
<span style="background-color:rgb(250, 250, 250);"><span style="color:rgb(89, 89, 89);">端set数据第11次时，客户端因为异常崩溃或主动退出，那么第11次的操作可能丢失。</span></span>

<span style="background-color:rgb(250, 250, 250);"><span style="color:rgb(89, 89, 89);">RDB模式的相关协议及实现细节详见 下面的实现篇。</span></span>

2.**AOF模式**
Append Only File模式，将redis执行过的所有写指令记录下来追加到文件中，在下次redis重新启动时，只要把这些写指令从前到后再重复执行一遍，就可以实现数据恢复了。
与redis不同的是，redis使用每x秒去同步一次未被同步的aof的策略，而针对于移动端的特性，Aredis使用了客户端每次调用即同步当次aof的策略（对于客户端来说，IO操作仍是异步，在当前调用线程中设置超时等待IO结果）。
因为采用了追加方式，如果不做任何处理的话，AOF文件会变得越来越大，同redis一样，Aredis提供了rewrite模式去压缩aof日志，客户端在aof模式下可以调用sync接口去压缩重写aof日志。

**AOF模式的优点：**
1. <span style="background-color:rgb(250, 250, 250);"><span style="color:rgb(89, 89, 89);">对于客户端来说，每次数据变更实时生效，无需任何提交操作即可实现数据的共享（相比于preference和db）</span></span>
2. 不会丢数据

**AOF模式的缺点：**
1. 吞吐率不如RDB模式
2. 长时间不压缩aof日志文件的话会导致aof文件越来越大，初始化读取效率降低。


#### Aredis的使用：

      创建一个aredis实例
```java
ARedisCache cache = ARedisCache.create(this, "test", new AredisDefaultConfig() {
    @Override
    public AredisType getType() {
        return AredisType.RDB;
    }
});
```
通过实例存取key value
```java
cache.set("key", 1747, System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
System.out.println("get key:" + cache.get("key"));
```

<span style="color:#D4380D;">**Aredis的配置选项：**</span>
通过AredisConfig对象指定，其中的接口返回值含义如下：
```java
    /**
     * return 使用rdb 或aof 模式,默认是rdb模式
     */
    AredisType getType();

    /**
     *  return rdb模式下每隔 多少次set/delete 操作后 写一次镜像 默认为50次
     *  如果返回<=0 则不会自动触发持久化操作
     */
    int getRdbSaveCount();

    /**
     * return rdb模式下每隔 多少毫秒 写一次镜像 默认为10分钟
     *  如果返回<=0 则不会自动触发持久化操作
     */
    long getRdbSaveInterval();

    /**
     * @return aof模式下 是否使用异步io ,默认false
     */
    boolean useAofStrictMode();

    /**
     * @return 异步io的最大等待超时的毫秒数时间 默认为1000ms
     */
    int getMaxIoWaitTime();

    /**
     * @return 是否持久化数据  默认true
     */
    boolean persist();
```

<span style="color:#D4380D;">**还有一些全局的固定配置：**</span>
* 单个key-value的最大 大小为4194304~=4m 
* 每个aredis节点 内存中最多存放20m的数据，当存放一个新key-value超过了这个限制后，会触发lru策略，删除掉最久没被使用过的那个key-value，重复这个过程直到占用空间 小于20m
* 每个aredis节点 最多存放90000个 key-value


<span style="color:#D4380D;">**aredis 如何控制自定义类型数据的持久化策略**</span>
首先bean对象需要实现一个 markable interface：Apersist
```java
public class Team implements APersist {

    public float slary;

    public boolean playoff;

    public String name;

    public int cham;

    public List<Player> players;
    
}
```
实现接口后，在存取Team对象之前，向Aredis容器为Team注册一个唯一的id
```
APojoManager.regist(Team.class,10);
```
注册后，可以向aredis节点进行存取Team的对象，默认正反序列化的策略是使用fastjson。
如果想自定义bean的正反序列化（以protostuff举例），可以实现ApojoStrategy接口
```java
public class TeamAPojoStrategy implements APojoStrategy<Team> {

    @Override
    public byte[] toBytes(Team object) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Schema<Team> schema = RuntimeSchema.getSchema(Team.class);
        LinkedBuffer linkedBuffer = LinkedBuffer.allocate(256);
        try {
            ProtobufIOUtil.writeDelimitedTo(outputStream,object,schema,linkedBuffer);
            byte[] bytes = outputStream.toByteArray();
            return bytes;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    @Override
    public Team toObject(byte[] bytes, Class<Team> cls) {
        Schema<Team> schema = RuntimeSchema.getSchema(Team.class);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        Team team = new Team();
        try {
            ProtobufIOUtil.mergeDelimitedFrom(inputStream,team,schema);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return team;
    }
}
```
```java
        APojoManager.regist(Team.class, 10, new TeamAPojoStrategy());
```

#### Aredis的实现：
**1.aredis的存储协议**
rdb:
![DingTalk20180130202206.png | center | 414x363](https://gw.alipayobjects.com/zos/skylark/7c28552c-d97c-41a8-8c9c-107b1f775e06/2018/png/d2f2c0f1-7fc6-43da-b596-a7f8badca77d.png "")

aof:
![DingTalk20180130202306.png | center | 324x437](https://gw.alipayobjects.com/zos/skylark/29ff6661-96bf-4777-a6c9-33ba837185d5/2018/png/901326d3-bd78-410c-bf0b-30cee9e35f86.png "")

**2.aredis的结构**

![DingTalk20180131181739.png | center | 618x466](https://gw.alipayobjects.com/zos/skylark/c7a349f5-f1f3-47e4-97a5-f31d2e5bedea/2018/png/abf24c21-70ff-431f-9ab9-6d0b95651089.png "")
**3.native层的数据结构**
   native存储key-value使用 stl的标准库
```
hash_map<string, lru *> *dicts
```
string为 aredis节点名称，lru为实际存储键值对的容器，也使用hash\_map实现。
**4.native层的异步写的实现 **
使用了vfork()替换了原版redis的fork()，在资源相对有限的移动端设备上vfork()的代价更小。
因为vfork()使用的是同一份内存空间，
所以带来了写镜像时数据时如果继续操作内存字典带来的数据不同步问题(如perference可能产生的concurrentmodify问题)，目前解决方案是在准备写镜像时，线程安全的设置一个标记flag，在这      个标记下所有客户端对aredis的set/delete操作都写到一个副本中，等vfork写结束再合并内存的主字典与这个副本。这保证      了在写操作时aredis依然可以对客户端提供服务，同时一个客户端的提交是对所有客户端可见的。


#### Aredis的数据：
| <span style="background-color:#E4F7D2;">写简单数据（2w个uuid）</span> | <span style="background-color:#E4F7D2;">同步耗时   (50个key/commit</span>) | <span style="background-color:#E4F7D2;">异步耗时（50个key/apply）</span> | <span style="background-color:#E4F7D2;">内存</span> |
| :--- | :--- | :--- | :--- |
| aredis-rdb | 4100ms | 160ms | native:4m heap:0m |
| aredis-aof | 500ms   --每个key/commit | 4200ms  --每个key/commit | native:4m heap:0m |
| preference | 7000ms | 崩溃 oom | native:0m heap:3.7m |
| sqlite | 1870ms | x | x |
| <span style="background-color:#E4F7D2;">检索2w个简单数据</span> | <span style="background-color:#E4F7D2;">耗时</span> |  |  |
| aredis | 130ms |  |  |
| preference | 20ms |  |  |
| sqlite (select where ) | 280ms |  |  |
| <span style="background-color:#E4F7D2;">初始化2w个简单数据</span> | <span style="background-color:#E4F7D2;">耗时</span> |  |  |
| aredis-rdb | 34ms |  |  |
| aredis-aof | 38ms |  |  |
| preference | 58ms |  |  |
| sqlite | x |  |  |
#### 复杂数据
| <span style="background-color:#E4F7D2;">写复杂数据(2w个list<bean>)</span> | <span style="background-color:#E4F7D2;">同步耗时   (50个key/commit)</span> | <span style="background-color:#E4F7D2;">异步耗时（50个key/apply）</span> |
| :--- | :--- | :--- |
| aredis-rdb | 6800ms | 700ms |
| aredis-aof | 1500ms  --每个key/commit | 5400ms  --每个key/commit |
| <span style="background-color:#E4F7D2;">检索复杂数据(2w个list<bean>)</span> | <span style="background-color:#E4F7D2;">耗时</span> |  |
| aredis | 700ms |  |
| <span style="background-color:#E4F7D2;">初始化复杂数据(2w个list<bean>)</span> | <span style="background-color:#E4F7D2;">耗时</span> |  |
| aredis-rdb | 50ms |  |
| aredis-aof | 55ms |  |


