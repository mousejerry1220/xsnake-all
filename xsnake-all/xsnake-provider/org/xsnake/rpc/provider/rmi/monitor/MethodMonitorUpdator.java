package org.xsnake.rpc.provider.rmi.monitor;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.zookeeper.KeeperException;
import org.xsnake.rpc.common.MessageHandler;
import org.xsnake.rpc.provider.XSnakeProviderContext;
import org.xsnake.rpc.provider.rmi.RMISupportHandler;
import org.xsnake.rpc.provider.rmi.SemaphoreWrapper;
import org.xsnake.rpc.provider.rmi.XSnakeInterceptorHandler;

public class MethodMonitorUpdator extends Thread{ 

	
	public void run() {
		while(true){
			List<XSnakeInterceptorHandler> list = context.getRmiSupportHandler().getHandlerList();
			for(XSnakeInterceptorHandler handler : list){
				List<SemaphoreWrapper> methodList = handler.getMethodSemaphoreList();
				for(SemaphoreWrapper method : methodList){
					int times = method.resetTimes();
					try {
						recordAllTimes(method.getInterFace(),method.getMethod(),times);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			try {
				TimeUnit.MINUTES.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	};
	
	XSnakeProviderContext context;
	
	public MethodMonitorUpdator(XSnakeProviderContext context){
		this.context = context;
	}
	
	public void recordAllTimes(Class<?> interFace,Method method,int times) throws KeeperException, InterruptedException, IOException{
		Date now = new Date();
		String methodName = method.getName() + "("+Arrays.toString(method.getParameterTypes())+")";
		String allTimesPath = context.getRmiSupportHandler().getAllInvokeTimesPath();
		String methodPath = allTimesPath+"/"+interFace.getName()+"/"+methodName;
		RMISupportHandler handler = context.getRmiSupportHandler();
		handler.getZooKeeper().dir(allTimesPath);
		handler.getZooKeeper().dir(allTimesPath+"/"+interFace.getName());
		if(handler.getZooKeeper().exists(methodPath)){
			String methodMonitorInfoStringInfo = handler.getZooKeeper().dirData(context.getRmiSupportHandler().getInvokeTimesPath()+"/"+interFace.getName()+"/"+methodName);
			MethodMonitorInfo methodMonitorInfo = (MethodMonitorInfo) MessageHandler.stringToObject(methodMonitorInfoStringInfo);
			methodMonitorInfo.setAllTimes(methodMonitorInfo.getAllTimes()+times);
			MonitorInfo monitorInfo = new MonitorInfo();
			monitorInfo.setDatetime(now);
			monitorInfo.setTimes(times);
			methodMonitorInfo.getList().add(monitorInfo);
			handler.getZooKeeper().dir(methodPath,MessageHandler.objectToString(methodMonitorInfo));
		}else{
			handler.getZooKeeper().dir(methodPath,MessageHandler.objectToString(new MethodMonitorInfo()));
		}
	}

}
