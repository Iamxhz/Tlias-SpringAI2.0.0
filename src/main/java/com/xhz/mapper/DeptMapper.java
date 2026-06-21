package com.xhz.mapper;

import com.xhz.pojo.Dept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DeptMapper {


    @Results({@Result(column = "create_time", property = "createTime"),
            @Result(column = "update_time", property = "updateTime")})
    @Select("select id, name, create_time, update_time from dept")
    public List<Dept> findAll();

    /**
     * 根据id删除部门
     */
    @Select("delete from dept where id = #{id}")
    public void deleteById(Integer id);

    @Select("insert into dept(name, create_time, update_time) values(#{name}, #{createTime}, #{updateTime})")
    void insert(Dept dept);

    @Select("select id, name, create_time, update_time from dept where id = #{id}")
    Dept getById(Integer id);

    @Select("update dept set name = #{name}, update_time = #{updateTime} where id = #{id}")
    void update(Dept dept);
}
