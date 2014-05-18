package com.alibaba.test.jcommander;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.beust.jcommander.IStringConverter;

public class DateTimeConvert implements IStringConverter<Date>{

	@Override
	public Date convert(String arg0) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date d = null;
		try {
			d = format.parse(arg0);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return d;
	}

}
