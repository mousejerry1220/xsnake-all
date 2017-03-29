package org.xsnake.rpc.consumer.rmi;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.rmi.UnmarshalException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.remoting.rmi.RmiProxyFactoryBean;
import org.xsnake.rpc.connector.ZooKeeperWrapper;

public class XSnakeProxyHandler implements InvocationHandler {
	
	Class<?> interfaceService = null;
	
	String environment = null;
	
	ZooKeeperWrapper zooKeeper = null;
	
	Map<String,Object> targetMap = new LinkedHashMap<String,Object>();
	
	List<String> targetNodeList;
	
	final String path;
	
	public XSnakeProxyHandler(String environment,ZooKeeperWrapper zooKeeper, Class<?> interfaceService) {
		this.interfaceService = interfaceService;
		this.environment = environment;
		this.zooKeeper = zooKeeper;
		path = "/XSNAKE/"+environment+"/SERVICES/" + interfaceService.getName();
		try {
			zooKeeper.dir(path);
		} catch (KeeperException e) {
			throw new BeanCreationException("没有可用的服务");
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new BeanCreationException(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			throw new BeanCreationException(e.getMessage());
		}
		
		try {
			initTarget();
		} catch (KeeperException e) {
			e.printStackTrace();
			throw new BeanCreationException("没有可用的服务");
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new BeanCreationException(e.getMessage());
		}
		
		try {
			zooKeeper.onChildrenChange(path, new Watcher() {
				@Override
				public void process(WatchedEvent event) {
					try {
						initTarget();
					} catch (KeeperException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			});
		} catch (KeeperException e) {
			e.printStackTrace();
			throw new BeanCreationException("没有可用的服务");
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new BeanCreationException(e.getMessage());
		}
		
	}

	/**
	 * 临时节点变动，重新装载RMI对象
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	synchronized private void initTarget() throws KeeperException, InterruptedException {
		Map<String,Object> newTargetMap = new LinkedHashMap<String,Object>();
		List<String> list = zooKeeper.getChildren(path);
		
		if(list==null || list.size() == 0){
			targetNodeList.clear();
			targetMap.clear();
			throw new BeanCreationException("没有可用的服务");
		}
		
		for(String node : list){
			byte[] rmiPathData = zooKeeper.dirData(path + "/"+node);
			if(rmiPathData == null){
				continue;
			}
			String rmiPath = new String(rmiPathData);
			if(targetMap.get(node) != null){
				newTargetMap.put(node, targetMap.get(node));
				continue;
			}
			
			RmiProxyFactoryBean c = new RmiProxyFactoryBean();
			c.setServiceInterface(interfaceService);
			c.setServiceUrl(rmiPath);
			c.afterPropertiesSet();
			Object target = c.getObject();
			newTargetMap.put(node, target);
		}
		targetMap.clear();
		targetMap = newTargetMap;
		targetNodeList = list;
	}

	public Object createProxy() {
		return Proxy.newProxyInstance(XSnakeProxyHandler.class.getClassLoader(),
				new Class[]{interfaceService}, this);
	}

	private Object invoke(Method method, Object[] args,boolean repeat) throws Throwable{
		//如果是重复调用暂停一段时间
		if(repeat){
			TimeUnit.MILLISECONDS.sleep(500);
		}
		
		int size = targetNodeList.size();
		if(size == 0){
			throw new IllegalAccessException("没有可用的服务");
		}
		String path = targetNodeList.get(RandomUtils.nextInt(size));
		Object target = targetMap.get(path);
		try{
			return method.invoke(target, args);
		}  catch(InvocationTargetException e){
			if(e.getTargetException() instanceof RemoteConnectFailureException &&  e.getTargetException().getCause() instanceof java.rmi.ConnectIOException){
				return invoke(method, args,true);
			}
			if(e.getTargetException() instanceof UnmarshalException || //执行中断开连接时代码执行至此
					(e.getTargetException() instanceof RemoteConnectFailureException )){	//非执行中断开连接时代码执行至此
				return invoke(method, args,true);
			}else if (e.getTargetException() instanceof InvocationTargetException){
				return invoke(method, args,true);
			}else if (e.getTargetException() instanceof UndeclaredThrowableException){
				UndeclaredThrowableException undeclaredThrowable = (UndeclaredThrowableException)e.getTargetException();
				if(undeclaredThrowable.getUndeclaredThrowable() instanceof InvocationTargetException){
					Throwable throwable = ((InvocationTargetException)undeclaredThrowable.getUndeclaredThrowable()).getTargetException();
					throw throwable;
				}
				throw undeclaredThrowable.getUndeclaredThrowable();
			}else {
				throw e.getTargetException();
			}
		}
	}
	
	public Object invoke(Object proxy, Method method, Object[] args)throws Throwable {
		return invoke(method, args,false);
	}

}
