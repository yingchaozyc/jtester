package com.alibaba.test.jcommander;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class IdValidate implements IParameterValidator{

	@Override
	public void validate(String name, String value) throws ParameterException {
		int id = Integer.parseInt(value);
		if(id > 100){
			throw new IllegalArgumentException("id is too big!");
		}
	}

}
