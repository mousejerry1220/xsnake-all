package org.xsnake.rpc.test;

import org.xsnake.rpc.api.Remote;

@Remote
public interface IMyService {
	String todo(String name);
}
