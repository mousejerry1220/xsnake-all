package org.xsnake.rpc.common;

import java.io.Serializable;

public class ResponseObject implements Serializable{

	public static final String SUCCESS = "success";
	
	public static final String ERROR = "error";
	
	private static final long serialVersionUID = 1L;

	private String status;
	
	private String message;
	
	private Object resultObject;

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Object getResultObject() {
		return resultObject;
	}

	public void setResultObject(Object resultObject) {
		this.resultObject = resultObject;
	}
	
}
