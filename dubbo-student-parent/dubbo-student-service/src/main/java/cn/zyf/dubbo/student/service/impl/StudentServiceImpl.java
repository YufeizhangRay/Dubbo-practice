package cn.zyf.dubbo.student.service.impl;

import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.dubbo.config.annotation.Service;

import cn.zyf.dubbo.student.pojo.Student;
import cn.zyf.student.mapper.StudentMapper;
import cn.zyf.student.service.StudentService;

@Service
public class StudentServiceImpl implements StudentService{

	@Autowired
	private StudentMapper studentMapper;
	
	@Override
	public void addStudent(Student student) {
		studentMapper.addStudent(student);
	}

	@Override
	public Student queryStudentByStuNo(int stuNo) {
		return studentMapper.queryStudentByStuNo(stuNo);
	}

}
