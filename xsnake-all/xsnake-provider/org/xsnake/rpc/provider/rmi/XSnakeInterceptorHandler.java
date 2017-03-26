package org.xsnake.rpc.provider.rmi;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.xsnake.rpc.api.Remote;

public class XSnakeInterceptorHandler implements InvocationHandler {
	
	//全局的并发量
	Semaphore maxThread = null;
	
	//注册在ZK的节点名称，用于更新访问次数
	String nodeName;
	
	Remote remote;
	
	public XSnakeInterceptorHandler(Class<?> interfaceClass,Object targetObject,String nodeName,Semaphore maxThread){
		this.interfaceClass = interfaceClass;
		this.targetObject = targetObject;
		this.maxThread = maxThread;
		this.nodeName = nodeName;
		remote = interfaceClass.getAnnotation(Remote.class);
	}
	
	Object targetObject;
	
	Class<?> interfaceClass;
	
	public Object createProxy() {
		return Proxy.newProxyInstance(targetObject.getClass().getClassLoader(), 
				new Class[]{interfaceClass}, this);
	}

	public Object invoke(Object proxy, Method method, Object[] args)throws Throwable {
		maxThread.acquire();
		System.out.println(maxThread.availablePermits());
	    try {
	    	Object result = null;
			try{
				Method interfaceMethod=null; //给方法默认的信号量，初始化时候读取远程的配置，每个方法一个map存它所有的信号量对象，通过信号量的availablePermits方法，来提示监控，修改远程参数，
				try{
					interfaceMethod = interfaceClass.getMethod(method.getName(), method.getParameterTypes());
				}catch(Exception e){
					
				}
				result = method.invoke(targetObject, args);
				TimeUnit.SECONDS.sleep(2);
			}catch(Exception e){
				throw e;
			}
			return result;
	    }  finally {
	    	//nodeName,interfaceClass,method.getName();
	    	maxThread.release();
	    }
	}

}
