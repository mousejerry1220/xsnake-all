package org.xsnake.rpc.rest;

public interface IConverter<T> {

	T converter(String str) throws ConverterException;
	
}
