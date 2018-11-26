package cn.zyf.student.service;

import cn.zyf.dubbo.student.pojo.Student;

public interface StudentService {
	void addStudent(Student student);
	Student queryStudentByStuNo(int stuNo);
}
