package org.xsnake.rpc.test;


import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class RemoteTest implements IRemoteTest{
	
	public String sayHello(Map<String,String> dataMap)  {
		return "v94 hello "+ dataMap.get("name") +"===="+ dataMap.get("aaa");
	}

}
