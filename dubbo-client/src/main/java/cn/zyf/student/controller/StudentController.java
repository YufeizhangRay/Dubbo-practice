package cn.zyf.student.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.dubbo.config.annotation.Reference;

import cn.zyf.student.server.StudentServer;

//@Controller
//@ResponseBody
@RestController
@RequestMapping("controller")
public class StudentController {
	
	@Reference//将远程服务中的StudentServer注入
	private StudentServer server ;
	
	@RequestMapping("rpcServer")
	public String rpcServer() {
		String result = server.server("Ray") ;
		return result;
	}
}
