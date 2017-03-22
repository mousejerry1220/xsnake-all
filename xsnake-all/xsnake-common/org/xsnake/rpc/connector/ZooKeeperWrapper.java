package org.xsnake.rpc.connector;

import java.io.IOException;
import java.net.ConnectException;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

public class ZooKeeperWrapper {

	protected ZooKeeper _zooKeeper;
	
	public void mkTempDir(String node) throws KeeperException, InterruptedException, IOException {
		mkDir(node, null, CreateMode.EPHEMERAL_SEQUENTIAL);
	}

	public void mkDir(String node) throws KeeperException, InterruptedException, IOException {
		mkDir(node, null, CreateMode.PERSISTENT);
	}

	public void mkDir(String path,String value,CreateMode createMode)throws KeeperException, InterruptedException, IOException{
		byte[] data = value != null ? value.getBytes() : null;
		if(!_zooKeeper.getState().isConnected()){
			throw new ConnectException(" zookeeper connection loss !");
		}
		if(_zooKeeper.exists(path, null)==null){
			_zooKeeper.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, createMode);
		}else{
			_zooKeeper.setData(path, data, -1);
		}
	}
	
	public void close() throws InterruptedException{
		_zooKeeper.close();
	}
	
}
