package org.xsnake.rpc.test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.xsnake.rpc.consumer.rmi.XSnakeProxyFactory;

public class TestClient {

//	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
//		ApplicationContext ctx = new FileSystemXmlApplicationContext("classpath:application-context2.xml");
//		IMyService s = 
		Map<String,String> propertyMap = new HashMap<String,String>();
		propertyMap.put("zooKeeper", "127.0.0.1:2181");
		propertyMap.put("environment", "test");
		XSnakeProxyFactory factory = new XSnakeProxyFactory(propertyMap);
		final IMyService s = factory.getService(IMyService.class);
		for(int i=0;i<200;i++){
			new Thread(){
				public void run() {
					System.out.println(s.todo("aaaaaa"));
				};
			}.start();
			//TimeUnit.SECONDS.sleep(1);
		};
	}
}
