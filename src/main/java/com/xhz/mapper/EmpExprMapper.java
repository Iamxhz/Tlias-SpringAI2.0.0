package com.xhz.mapper;

import com.xhz.pojo.EmpExpr;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface EmpExprMapper {

    /**
     * 批量插入员工工作经历信息
     *
     * @return
     */
    public int insertBatch(List<EmpExpr> exprList);

    /**
     * 根据员工的ID批量删除工作经历信息
     *
     * @return
     */
    int deleteByEmpIds(List<Integer> empIds);
}