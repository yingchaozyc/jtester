package com.alibaba.test.jcommander; 

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public class JCommanderTest1 {
	@Parameter(names="-id", validateWith=IdValidate.class, description="ID")
	private int id ;
	
	@Parameter(names="-name", description="名称")
	private String name;
	
	@Parameter(names="-company", description="公司名称")
	private String comapny;

	@Parameter(names="-debug", description="debug模式", required=true)
	private boolean debug;
	
	// list传递参数的方式有点糟糕
	@Parameter(names="-address", description="地址列表")
	private List<String> address;
	
	@Parameter(names="-password", password=true, description="秘钥")
	private String password;
	
	// convert参数自定义转换，从string转换为你想要的那种类型
	@Parameter(names="-date", converter=DateTimeConvert.class)
	private Date date; 
	
	// 接受其他没有显式注明的参数
	@Parameter
	private List<String> otherParameter = new ArrayList<String>();
	
	public static void main(String[] args) {
		JCommanderTest1 test1 = new JCommanderTest1();
		new JCommander(test1, args);
		
		System.out.println("id:" + test1.getId());
		System.out.println("name:" + test1.getName());
		System.out.println("company:" + test1.getComapny());
		System.out.println("debug:" + test1.isDebug()); 
		System.out.println("list:" + test1.getAddress()); 
		System.out.println("password:" + test1.getPassword()); 
		System.out.println("date:" + test1.getDate().getTime()); 
		System.out.println("otherParameter:" + test1.getOtherParameter()); 
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getOtherParameter() {
		return otherParameter;
	}

	public void setOtherParameter(List<String> otherParameter) {
		this.otherParameter = otherParameter;
	}

	public String getComapny() {
		return comapny;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isDebug() {
		return debug;
	}

	public List<String> getAddress() {
		return address;
	}

	public void setAddress(List<String> address) {
		this.address = address;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public void setComapny(String comapny) {
		this.comapny = comapny;
	}  
}





