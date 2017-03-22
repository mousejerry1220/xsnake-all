package org.xsnake.rpc.provider;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MessageQueueWarpper {

	int size = 1000;
	
	BlockingQueue<MessageBody> queue = new LinkedBlockingQueue<MessageBody>(size);
	
	public void put(MessageBody message){
		try {
			queue.put(message);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public MessageBody take(){
		try {
			return queue.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}
	
}
