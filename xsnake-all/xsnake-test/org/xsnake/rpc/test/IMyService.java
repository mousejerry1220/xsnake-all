package org.xsnake.rpc.test;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.xsnake.rpc.api.Remote;

@Remote
public interface IMyService {
	String todo(@PathVariable @RequestParam(name="name") String name);
}
