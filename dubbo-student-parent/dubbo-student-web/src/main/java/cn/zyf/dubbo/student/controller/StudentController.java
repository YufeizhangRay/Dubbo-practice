package cn.zyf.dubbo.student.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.dubbo.config.annotation.Reference;

import cn.zyf.dubbo.student.pojo.Student;
import cn.zyf.student.service.StudentService;

@RestController
//@Controller
@RequestMapping("controller")
public class StudentController {

	@Reference
	private StudentService studentService;
	
	@RequestMapping("queryStudentByNo")
	public ModelAndView queryStudentByNo(@RequestParam(value="stuNo",required=true)int stuNo){
		ModelAndView modelAndView = new ModelAndView("success");
		Student student = studentService.queryStudentByStuNo(stuNo);
		modelAndView.addObject("student",student);
		System.out.println(student.getStuNo()+","+student.getStuName()+","+student.getStuAge());
		return modelAndView;
	}
	
	@RequestMapping("addStudent")
	public String addStudent(@RequestParam(value="stuNo",required=true)int stuNo,
			@RequestParam(value="stuName",required=true)String stuName,
			@RequestParam(value="stuAge",required=true)int stuAge) {
		Student student = new Student(stuNo,stuName,stuAge);
		studentService.addStudent(student);
		return "success";
	}
	
}
