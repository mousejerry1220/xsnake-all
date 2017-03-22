package org.xsnake.rpc.test;

import java.util.Map;

import org.xsnake.rpc.api.Remote;
import org.xsnake.rpc.api.Rest;
@Remote
public interface IRemoteTest {

	@Rest(value="/test/{name}/{aaa}")
	String sayHello(Map<String,String> dataMap);
}
