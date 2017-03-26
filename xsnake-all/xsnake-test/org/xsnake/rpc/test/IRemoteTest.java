package org.xsnake.rpc.test;

import org.xsnake.rpc.api.Remote;
import org.xsnake.rpc.api.Rest;
@Remote
public interface IRemoteTest {

	@Rest(value="/test/{name}/{aaa}")
	String sayHello(String name);
}
