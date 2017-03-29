package org.xsnake.rpc.provider.rmi;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.remoting.rmi.RmiServiceExporter;
import org.xsnake.rpc.api.Remote;
import org.xsnake.rpc.common.ReflectionUtil;
import org.xsnake.rpc.connector.ZooKeeperConnector;
import org.xsnake.rpc.connector.ZooKeeperWrapper;
import org.xsnake.rpc.provider.XSnakeProviderContext;

public class RMISupportHandler {

	ZooKeeperWrapper zooKeeper = null;
	
	private int defaultPort = 16785;
	
	private String host = null;
	
	RMIServerSocketFactory server = new XSnakeServerSocketFactory();
	RMIClientSocketFactory client = new XSnakeClientSocketFactory();
	
	Semaphore maxThread;
	
	XSnakeProviderContext context;
	
	List<XSnakeInterceptorHandler> handlerList = new ArrayList<XSnakeInterceptorHandler>(); 
	
	public RMISupportHandler(XSnakeProviderContext context) throws BeanCreationException {
		
		this.context = context;
		
		maxThread = new Semaphore(context.getRegistry().getMaxThread());
		
		// 连接ZooKeeper
		if (StringUtils.isEmpty(context.getRegistry().getZooKeeper())) {
			throw new BeanCreationException("XSnake启动失败，配置参数registry.zooKeeper不能为空");
		}
		
		try {
			zooKeeper = new ZooKeeperConnector(context.getRegistry().getZooKeeper(), context.getRegistry().getTimeout());
		} catch (Exception e) {
			throw new BeanCreationException("XSnake启动失败，无法连接到ZooKeeper服务器。" + e.getMessage());
		}
		
		//设置RMI主机地址
		host = context.getRegistry().getRmiHost();
		if(host == null){
			host = getLocalHost();
		}
		System.setProperty("java.rmi.server.hostname", host);
		
		// 初始化XSNAKE主目录
		try {
			zooKeeper.dir("/XSNAKE");
			zooKeeper.dir(getRootPath());
			zooKeeper.dir(getServicePath());
		} catch (Exception e) {
			throw new BeanCreationException("XSnake启动失败，初始化数据失败。" + e.getMessage());
		}
		
		export(context);
	}

	protected String getLocalHost() {
		try {
			return InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			throw new BeanCreationException("创建对象失败，无法自动获取主机地址，请配置");
		}
		
	}
	
	/**
	 * 顺序获取可用的端口 
	 * @param port
	 * @return
	 */
	private int getPort(int port) {
		ServerSocket ss = null;
		try{
			 ss = new ServerSocket(port);
		}catch(Exception e){
			port = port + 1;
			return getPort(port);
		}finally{
			try {
				if(ss!=null){
					ss.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return port;
	}
	
	private void export(XSnakeProviderContext context) {
		boolean flag = true;
		ApplicationContext applicationContext = context.getApplicationContext();
		int port = getPort(defaultPort);
		String[] names = applicationContext.getBeanDefinitionNames();
		for (String name : names) {
			Object obj = applicationContext.getBean(name);
			Object target = ReflectionUtil.getTarget(obj);
			Class<?>[] interfaces = target.getClass().getInterfaces();
			for (Class<?> interFace : interfaces) {
				Remote remote = interFace.getAnnotation(Remote.class);
				if (remote != null) {
					try {
						RmiServiceExporter se = new RmiServiceExporter();
						String nodeName = UUID.randomUUID().toString();
						XSnakeInterceptorHandler handler = new XSnakeInterceptorHandler(interFace,target,nodeName,maxThread);
						se.setServiceName(nodeName);
						Object proxy = handler.createProxy();
						se.setService(proxy);
						se.setAlwaysCreateRegistry(flag);
						se.setRegistryPort(port);
						se.setServiceInterface(interFace);
						se.setClientSocketFactory(client);
						se.setServerSocketFactory(server);
						se.afterPropertiesSet();
						flag = false;
						String url = String.format("rmi://%s:%d/%s", host, port, nodeName);
						handlerList.add(handler);
						try {
							zooKeeper.dir(getServicePath()+"/"+interFace.getName());
							zooKeeper.tempDir(getServicePath()+"/"+interFace.getName()+"/"+nodeName, url);
							
							String nodeRoot = getRootPath() + "/NODES";
							zooKeeper.dir(nodeRoot);
							String nodes = nodeRoot+"/"+host+"_"+port;
							zooKeeper.dir(nodes);
							zooKeeper.tempDir(nodes+"/"+interFace.getName());
						}  catch (Exception e) {
							e.printStackTrace();
							throw new BeanCreationException(e.getMessage());
						}
					} catch (RemoteException e) {
						e.printStackTrace();
						throw new BeanCreationException("RMI remote access bean [" + interFace.getName() + "] creation failed !" + e.getMessage());
					}
					
				}
			}
		}
	}

	public ZooKeeperWrapper getZooKeeper() {
		return zooKeeper;
	}
	
	public String getRootPath(){
		return "/XSNAKE/" + context.getRegistry().getEnvironment();
	}

	public String getServicePath(){
		return "/XSNAKE/" + context.getRegistry().getEnvironment()+ "/SERVICES";
	}
	
	public String getAllInvokeTimesPath(){
		return "/XSNAKE/" + context.getRegistry().getEnvironment()+ "/INVOKE_TIMES_ALL";
	}

	public List<XSnakeInterceptorHandler> getHandlerList() {
		return handlerList;
	}
	
}
