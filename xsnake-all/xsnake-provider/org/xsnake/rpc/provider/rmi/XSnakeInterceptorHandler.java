package org.xsnake.rpc.provider.rmi;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.xsnake.rpc.api.Remote;

public class XSnakeInterceptorHandler implements InvocationHandler {
	
	//全局的并发量
	Semaphore maxThread = null;
	
	//注册在ZK的节点名称，用于更新访问次数
	String nodeName;
	
	Remote remote;
	
	Map<String,SemaphoreWrapper> methodSemaphore = new HashMap<String,SemaphoreWrapper>();
	
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
				result = invoke(method, args);
			}catch(Exception e){
				throw e;
			}
			return result;
	    }  finally {
	    	//nodeName,interfaceClass,method.getName();
	    	maxThread.release();
	    }
	}

	private String methodKey(Method method){
		StringBuffer key = new StringBuffer();
		key.append(interfaceClass.getName())
		.append(".")
		.append(method.getName())
		.append("(")
		.append(Arrays.toString(method.getParameterTypes()))
		.append(")");
		return key.toString();
	} 
	
	/**
	 * 获取方法自身的信号量并控制
	 * @param method
	 * @param args
	 * @return
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws InterruptedException
	 */
	private Object invoke(Method method, Object[] args) throws IllegalAccessException, InvocationTargetException, InterruptedException {
		SemaphoreWrapper methodSemaphore = getMethodSemaphore(method);
		methodSemaphore.acquire();
		Object result;
		try{
			result = method.invoke(targetObject, args);
		}finally{
			methodSemaphore.release();
		}
		return result;
	}

	/**
	 * 通过方法获取到该方法的信号量
	 * @param method
	 * @return
	 */
	private SemaphoreWrapper getMethodSemaphore(Method method) {
		String methodKey = methodKey(method);
		SemaphoreWrapper semaphore = methodSemaphore.get(methodKey);
		if(semaphore == null){
			semaphore = new SemaphoreWrapper(interfaceClass,method,50);
			methodSemaphore.put(methodKey, semaphore);
		}
		return semaphore;
	}
	
	public List<SemaphoreWrapper> getMethodSemaphoreList(){
		return new ArrayList<SemaphoreWrapper>(methodSemaphore.values());
	}

}
