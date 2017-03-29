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
				List<SemaphoreWrapper> methodInfoList = handler.getMethodSemaphoreList();
				for(SemaphoreWrapper methodInfo : methodInfoList){
					try {
						recordAllTimes(methodInfo);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			try {
				TimeUnit.SECONDS.sleep(context.getRegistry().getMonitorInterval());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	};
	
	XSnakeProviderContext context;
	
	public MethodMonitorUpdator(XSnakeProviderContext context){
		this.context = context;
	}
	
	public void recordAllTimes(SemaphoreWrapper methodInfo) throws KeeperException, InterruptedException, IOException{
		int times = methodInfo.resetTimes();//调用次数，记录后清空重置为0
		Method method = methodInfo.getMethod();
		Class<?> interFace = methodInfo.getInterFace();
		Date now = new Date();
		String methodName = method.getName() + "("+Arrays.toString(method.getParameterTypes())+")";
		String allTimesPath = context.getRmiSupportHandler().getAllInvokeTimesPath();
		String methodPath = allTimesPath+"/"+interFace.getName()+"/"+methodName;
		RMISupportHandler handler = context.getRmiSupportHandler();
		handler.getZooKeeper().dir(allTimesPath);
		handler.getZooKeeper().dir(allTimesPath+"/"+interFace.getName());
		if(handler.getZooKeeper().exists(methodPath)){
			byte[] methodMonitorInfoData = handler.getZooKeeper().dirData(methodPath);
			MethodMonitorInfo methodMonitorInfo = (MethodMonitorInfo) MessageHandler.bytesToObject(methodMonitorInfoData);
			methodMonitorInfo.setMaxCallNum(methodInfo.getMaxCallNum());
			methodMonitorInfo.setMaxCallNumDate(methodInfo.getMaxCallNumDate());
			methodMonitorInfo.setAllTimes(methodMonitorInfo.getAllTimes()+times);
			MonitorInfo monitorInfo = new MonitorInfo();
			monitorInfo.setDatetime(now);
			monitorInfo.setTimes(times);
			methodMonitorInfo.getList().add(monitorInfo);
			handler.getZooKeeper().dir(methodPath,MessageHandler.objectToBytes(methodMonitorInfo));
		}else{
			handler.getZooKeeper().dir(methodPath,MessageHandler.objectToBytes(new MethodMonitorInfo()));
		}
	}

}
