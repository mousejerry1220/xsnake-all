package org.xsnake.rpc.rmq;

import java.lang.reflect.Method;

public class InvokeObject {

	private Object target;

	private Method method;

	public InvokeObject(Object target, Method method) {
		this.target = target;
		this.method = method;
	}

	public Object getTarget() {
		return target;
	}

	public Method getMethod() {
		return method;
	}

}