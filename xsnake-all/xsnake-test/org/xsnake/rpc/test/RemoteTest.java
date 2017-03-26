package org.xsnake.rpc.test;


import org.springframework.stereotype.Service;

@Service
public class RemoteTest implements IRemoteTest{
	
	public String sayHello(String name)  {
		return "v94 hello "+ name ;
	}

}
