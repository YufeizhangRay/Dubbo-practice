package cn.zyf.student.mapper;

import cn.zyf.dubbo.student.pojo.Student;

public interface StudentMapper {
	public Student queryStudentByStuNo(int stuNo);
	public void addStudent(Student student);
}
