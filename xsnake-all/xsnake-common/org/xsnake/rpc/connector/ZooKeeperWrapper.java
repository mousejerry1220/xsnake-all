package org.xsnake.rpc.connector;

import java.io.IOException;
import java.net.ConnectException;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

public class ZooKeeperWrapper {

	protected ZooKeeper _zooKeeper;
	
	/**
	 * 创建临时目录
	 * @param node
	 * @throws KeeperException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void tempDir(String node) throws KeeperException, InterruptedException, IOException {
		dir(node, null, CreateMode.EPHEMERAL);
	}
	
	/**
	 * 创建临时目录及数据
	 * @param node
	 * @param data
	 * @throws KeeperException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void tempDir(String node,String data) throws KeeperException, InterruptedException, IOException {
		dir(node, data, CreateMode.EPHEMERAL);
	}

	/**
	 * 创建目录
	 * @param node
	 * @throws KeeperException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void dir(String node) throws KeeperException, InterruptedException, IOException {
		dir(node, null, CreateMode.PERSISTENT);
	}

	/**
	 * 创建目录及数据
	 * @param node
	 * @param data
	 * @throws KeeperException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void dir(String node,String data) throws KeeperException, InterruptedException, IOException {
		dir(node, data, CreateMode.PERSISTENT);
	}
	
	/**
	 * 创建目录及数据，如果目录存在，则更新数据
	 * @param path
	 * @param value
	 * @param createMode
	 * @throws KeeperException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void dir(String path,String value,CreateMode createMode)throws KeeperException, InterruptedException, IOException{
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
	
	/**
	 * 获取目录数据
	 * @param path
	 * @return
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	public String dirData(String path) throws KeeperException, InterruptedException{
		byte[] data = _zooKeeper.getData(path, null, null);
		if(data == null){
			return null;
		}
		return new String(data);
	}
	
	
	/**
	 * 判断是否存在
	 * @param path
	 * @return
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	public boolean exists(String path) throws KeeperException, InterruptedException{
		return _zooKeeper.exists(path, null)!=null;
	}
	
	public void delete(String path) throws InterruptedException, KeeperException{
		_zooKeeper.delete(path, -1);
	}
	
	public void close() throws InterruptedException{
		_zooKeeper.close();
	}
	
}
