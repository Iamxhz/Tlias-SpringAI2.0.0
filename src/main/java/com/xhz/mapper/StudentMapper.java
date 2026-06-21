package com.xhz.mapper;

import com.xhz.pojo.Student;
import com.xhz.pojo.StudentQueryParam;
import org.apache.ibatis.annotations.Mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StudentMapper {
    List<Student> getStuPage(StudentQueryParam queryParam);

    void deleteByIds(List<Integer> ids);

    void save(Student student);

    Student getStuById(Integer id);

    /** 根据学号查询学员 */
    Student getStuByNo(String no);

    /** 根据姓名模糊查询学员列表 */
    List<Student> getStuByName(String name);

    void updateById(Student student);

    Integer countByClazzId(Integer id);

    /** 直接增加违纪扣分（原子更新，避免并发覆盖） */
    void addViolationScore(@Param("id") Integer id, @Param("score") Integer score);
}
