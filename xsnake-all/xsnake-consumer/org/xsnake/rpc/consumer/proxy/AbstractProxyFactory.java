package org.xsnake.rpc.consumer.proxy;

public abstract class AbstractProxyFactory {
	
	abstract public <T> T getService(Class<T> interfaceService);
	
}
