package org.xsnake.rpc.test;

import org.springframework.stereotype.Service;

@Service
public class MyServiceImpl implements IMyService{

	@Override
	public String todo(String name) {
		return "3===========" + name;
	}

}
