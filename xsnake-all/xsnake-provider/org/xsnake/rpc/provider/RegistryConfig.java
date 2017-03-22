package org.xsnake.rpc.provider;

public class RegistryConfig {

	/**
	 * 连接到ZooKeeper的超时时间，超出该时间限制则启动失败。单位:秒
	 */
	int timeout = 5;
	/**
	 * 集群负载能力，根据服务器的强弱能力配置
	 * 假设有3台服务器，
	 * A服务器硬件配置，每秒处理10个，
	 * B服务器硬件配置，每秒处理20个，
	 * C服务器硬件配置，每秒处理50个，
	 * 那么：
	 * A服务器可以配置为1
	 * B服务器可以配置为2
	 * C服务器可以配置为5
	 * 该值取值范围1~5
	 */
	int loadCapacity = 1;
	/**
	 * 服务器最大并发处理量，超出该并发数后，会进入排队状态，该值与loadCapacity成正比关系
	 * 该数值决定了整个服务的最大并发能力
	 * 如三台集群服务
	 * A配置100
	 * B配置200
	 * C配置500
	 * 那么:
	 * 理论上最大并发为1000，假设每个服务平均执行0.5秒,那么1000/0.5秒=2000TPS。
	 * 嗯是的，理论上！
	 */
	int maxThread = 100;
	
	/**
	 * 如果threadManager = true时生效
	 * 最小并发数大小，系统会根据实际使用情况动态分配并发处理数，
	 * 超过一段时间稳定在一个水平时会取这个平均值得百分之120，多余的线程被回收，但是不会小于该值。
	 * 反之会扩增线程，但是不会大于maxThread
	 */
	int minThread = 5;//暂未实现
	
	/**
	 * 如果threadManager = true时生效
	 * 初始化是的最高并发数大小
	 */
	int initThread = 5;//暂未实现
	
	/**
	 * 系统根据使用情况动态管理线程数
	 */
	boolean threadManager = false;//暂未实现
	
	/**
	 * 应用名称，消息队列会以应用作为参数，区分队列。命名推荐包名形式
	 */
	String application;
	
	/**
	 * 介绍应用，用于服务治理时查看
	 */
	String descript;
	
	/**
	 * 连接到ZooKeeper的地址
	 */
	String zooKeeper;
	
	/**
	 * RPC需要用到的MQ地址
	 */
	String messageQueue;

	/**
	 * java调用方式
	 */
	boolean invokeMode = true;
	
	/**
	 * rest调用方式的服务端
	 */
	boolean restMode = false;
	
	/**
	 * rest客户端，主要提供转发服务
	 */
	boolean restClient = false;
	/**
	 * spring boot 支持 tomcat ，jetty 默认tomcat
	 */
	String restContainer = "tomcat"; 
	
	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public int getLoadCapacity() {
		return loadCapacity;
	}

	public void setLoadCapacity(int loadCapacity) {
		this.loadCapacity = loadCapacity;
	}

	public int getMaxThread() {
		return maxThread;
	}

	public void setMaxThread(int maxThread) {
		this.maxThread = maxThread;
	}

	public String getApplication() {
		return application;
	}

	public void setApplication(String application) {
		this.application = application;
	}

	public String getDescript() {
		return descript;
	}

	public void setDescript(String descript) {
		this.descript = descript;
	}

	public String getZooKeeper() {
		return zooKeeper;
	}

	public void setZooKeeper(String zooKeeper) {
		this.zooKeeper = zooKeeper;
	}

	public String getMessageQueue() {
		return messageQueue;
	}

	public void setMessageQueue(String messageQueue) {
		this.messageQueue = messageQueue;
	}

	public boolean isInvokeMode() {
		return invokeMode;
	}

	public void setInvokeMode(boolean invokeMode) {
		this.invokeMode = invokeMode;
	}

	public boolean isRestMode() {
		return restMode;
	}

	public void setRestMode(boolean restMode) {
		this.restMode = restMode;
	}

	public boolean isRestClient() {
		return restClient;
	}

	public void setRestClient(boolean restClient) {
		this.restClient = restClient;
	}

	public int getMinThread() {
		return minThread;
	}

	public void setMinThread(int minThread) {
		this.minThread = minThread;
	}

	public int getInitThread() {
		return initThread;
	}

	public void setInitThread(int initThread) {
		this.initThread = initThread;
	}

	public boolean isThreadManager() {
		return threadManager;
	}

	public void setThreadManager(boolean threadManager) {
		this.threadManager = threadManager;
	}

	public String getRestContainer() {
		return restContainer;
	}

	public void setRestContainer(String restContainer) {
		this.restContainer = restContainer;
	}
	
}
