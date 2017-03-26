package org.xsnake.rpc.provider;

import java.io.IOException;

import org.apache.zookeeper.KeeperException;
import org.springframework.beans.BeanUtils;
import org.xsnake.rpc.util.GZipUtil;

import com.google.gson.Gson;

public class RemoteRegistryConfig extends RegistryConfig{

	public static final String REMOTE_REGISTRY_CONFIG_PATH = "/XSNAKE/CONFIG";
	
	private static final long serialVersionUID = 1L;

	private boolean ready = false;
	
	Gson gson = new Gson();

	public boolean isReady() {
		return ready;
	}
	
	public void loadConfig(XSnakeProviderContext context) throws KeeperException, InterruptedException, IOException{
		String path = getRemoteConfigPath(context);
		if(!context.getZooKeeper().exists(path)){
			return ;
		}
		String config = context.getZooKeeper().dirData(path);
		if(config != null){
			config = GZipUtil.gunzip(config);
			RegistryConfig remote = gson.fromJson(config, RegistryConfig.class);
			BeanUtils.copyProperties(remote, this);
			ready = true;
		}
	}
	
	public void uploadConfig(XSnakeProviderContext context) throws KeeperException, InterruptedException, IOException{
		String path = getRemoteConfigPath(context);
		String config = gson.toJson(this);
		context.getZooKeeper().dir(path, GZipUtil.gzip(config));
	}
	
	private String getRemoteConfigPath(XSnakeProviderContext context){
		return "/" + context.getRegistry().getEnvironment()  +REMOTE_REGISTRY_CONFIG_PATH + "/" + context.getLocalAddress();
	}
}
