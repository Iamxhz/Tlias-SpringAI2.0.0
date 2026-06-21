package com.xhz.service;

import com.xhz.pojo.Clazz;
import com.xhz.pojo.ClazzQueryParam;
import com.xhz.pojo.PageResult;

import java.util.List;

public interface ClazzService {
    PageResult getClazzsPage(ClazzQueryParam clazzQueryParam);

    void deleteById(Integer id);

    void save(Clazz clazz);

    Clazz getClazzById(Integer id);

    void updateById(Clazz clazz);

    List<Clazz> getClazzs();
}
