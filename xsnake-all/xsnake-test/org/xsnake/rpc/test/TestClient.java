package org.xsnake.rpc.test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

//@SpringBootApplication
public class TestClient {

	static ApplicationContext ctx;
	
//	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		ctx = new FileSystemXmlApplicationContext("classpath:application-context2.xml");
		final IMyService s = ctx.getBean(IMyService.class);
		
//		
//		Map<String,String> propertyMap = new HashMap<String,String>();
//		propertyMap.put("zooKeeper", "127.0.0.1:2181");
//		propertyMap.put("environment", "test");
//		XSnakeProxyFactory factory = new XSnakeProxyFactory(propertyMap);
//		final IMyService s = factory.getService(IMyService.class);
//		for(int j=0;j<100;j++){
//			for(int i=0;i<200;i++){
//				final int a= i;
//				new Thread(){
//					public void run() {
						System.out.println(s.todo("aaaaaa")+"=====");
//					};
//				}.start();
//			}
//			TimeUnit.SECONDS.sleep(1);
//		};
	}

	public static <T> T getBean(Class<T> cls) {
		return ctx.getBean(cls);
	}
	
}
