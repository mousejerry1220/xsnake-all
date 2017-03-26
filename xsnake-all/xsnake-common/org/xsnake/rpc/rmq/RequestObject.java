package org.xsnake.rpc.rmq;

import java.io.Serializable;

public class RequestObject implements Serializable {

	private static final long serialVersionUID = 1L;

	Class<?> interFace;
	
	String method;
	
	Class<?>[] parameterTypes;
	
	Object[] args;
	
	String environment;

	public Class<?> getInterFace() {
		return interFace;
	}

	public void setInterFace(Class<?> interFace) {
		this.interFace = interFace;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public Class<?>[] getParameterTypes() {
		return parameterTypes;
	}

	public void setParameterTypes(Class<?>[] parameterTypes) {
		this.parameterTypes = parameterTypes;
	}

	public Object[] getArgs() {
		return args;
	}

	public void setArgs(Object[] args) {
		this.args = args;
	}
	
	public String getEnvironment() {
		return environment;
	}

	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	public static String createKey(String environment,Class<?> interFace, String method,Class<?>[] parameterTypes) {
		String invokeKey = "xsnake://" + environment + "/" + method + "(";
		for (Class<?> parameter : parameterTypes) {
			invokeKey = invokeKey + parameter.getName() + ",";
		}
		if (parameterTypes.length > 0) {
			invokeKey = invokeKey.substring(0, invokeKey.length() - 1);
		}
		invokeKey = invokeKey + ")@" + interFace.getName();
		return invokeKey;
	}

	@Override
	public String toString() {
		return createKey(environment,interFace,method,parameterTypes);
	}
}