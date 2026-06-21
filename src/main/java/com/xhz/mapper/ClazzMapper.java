package com.xhz.mapper;

import com.xhz.pojo.Clazz;
import com.xhz.pojo.ClazzQueryParam;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ClazzMapper {
    List<Clazz> getClazzsPage(ClazzQueryParam clazzQueryParam);

    void deleteById(Integer id);

    void save(Clazz clazz);

    Clazz getClazzById(Integer id);

    void updateById(Clazz clazz);

    List<Clazz> getClazzs();
}
