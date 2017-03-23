package org.xsnake.rpc.rest;

public class LongConverter implements IConverter<Long>{

	@Override
	public Long converter(String str) throws ConverterException {
		try{
			return Long.parseLong(str);
		}catch(Exception e){
			throw new ConverterException(e.getMessage());
		}
	}
}
