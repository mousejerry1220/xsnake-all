package org.xsnake.rpc.test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.xsnake.rpc.consumer.rmi.XSnakeProxyFactory;

@SpringBootApplication
public class TestClient {

	static ApplicationContext ctx;
	
//	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
//		ctx = new FileSystemXmlApplicationContext("classpath:application-context2.xml");
//		final IMyService s = ctx.getBean(IMyService.class);
		
//		
		Map<String,String> propertyMap = new HashMap<String,String>();
		propertyMap.put("zooKeeper", "127.0.0.1:2181");
		propertyMap.put("environment", "SIT");
		XSnakeProxyFactory factory = new XSnakeProxyFactory(propertyMap);
		final IMyService s = factory.getService(IMyService.class);
		for(int j=0;j<100;j++){
//			for(int i=0;i<200;i++){
//				final int a= i;
//				new Thread(){
//					public void run() {
			try{
				System.out.println(s.todo("aaaaaa")+"=====");
			}catch(Exception e){}
//					};
//				}.start();
//			}
			TimeUnit.SECONDS.sleep(2);
		};
		
//		IRemoteTest remoteTest = factory.getService(IRemoteTest.class);
//		System.out.println(remoteTest.sayHello(new TestParam()));
	}

	public static <T> T getBean(Class<T> cls) {
		return ctx.getBean(cls);
	}
	
}
