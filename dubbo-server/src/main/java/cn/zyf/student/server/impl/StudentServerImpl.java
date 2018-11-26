package cn.zyf.student.server.impl;

import com.alibaba.dubbo.config.annotation.Service;

import cn.zyf.student.server.StudentServer;

@Service//阿里巴巴提供的service注解
public class StudentServerImpl implements StudentServer{

	@Override
	public String server(String name) {
		return "server:"+name;
	}

}
