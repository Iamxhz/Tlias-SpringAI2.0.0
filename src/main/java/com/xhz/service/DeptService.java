package com.xhz.service;


import com.xhz.pojo.Dept;

import java.util.List;

public interface DeptService {
    public List<Dept> findAll();

    public void deleteById(Integer id);

    void save(Dept dept);

    Dept getById(Integer id);

    void update(Dept dept);
}
