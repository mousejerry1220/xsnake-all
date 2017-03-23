package org.xsnake.rpc.rest;

public class StringConverter implements IConverter<String>{

	@Override
	public String converter(String str) throws ConverterException {
		return str;
	}
}
