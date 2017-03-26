package org.xsnake.rpc.consumer.rmq;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.xsnake.rpc.rmq.RequestObject;

public class XSnakeProxyHandler implements InvocationHandler {
	
	Class<?> interfaceService = null;
	
	String environment = null;
	
	public XSnakeProxyHandler(String environment,Class<?> interfaceService){
		this.interfaceService = interfaceService;
		this.environment = environment;
	}

	public Object createProxy() {
		return Proxy.newProxyInstance(XSnakeProxyHandler.class.getClassLoader(),
				new Class[]{interfaceService}, this);
	}

	public Object invoke(Object proxy, Method method, Object[] args)throws Throwable {
		RequestObject request = new RequestObject();
		request.setInterFace(interfaceService);
		request.setMethod(method.getName());
		request.setParameterTypes(method.getParameterTypes());
		request.setArgs(args);
		return null;
	}

}
