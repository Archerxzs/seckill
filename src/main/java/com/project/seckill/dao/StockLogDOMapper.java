package com.project.seckill.dao;

import com.project.seckill.dataobject.StockLogDO;

public interface StockLogDOMapper {
    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table stock_log
     *
     * @mbg.generated Thu Nov 19 16:07:30 CST 2020
     */
    int deleteByPrimaryKey(String stockLogId);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table stock_log
     *
     * @mbg.generated Thu Nov 19 16:07:30 CST 2020
     */
    int insert(StockLogDO record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table stock_log
     *
     * @mbg.generated Thu Nov 19 16:07:30 CST 2020
     */
    int insertSelective(StockLogDO record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table stock_log
     *
     * @mbg.generated Thu Nov 19 16:07:30 CST 2020
     */
    StockLogDO selectByPrimaryKey(String stockLogId);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table stock_log
     *
     * @mbg.generated Thu Nov 19 16:07:30 CST 2020
     */
    int updateByPrimaryKeySelective(StockLogDO record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table stock_log
     *
     * @mbg.generated Thu Nov 19 16:07:30 CST 2020
     */
    int updateByPrimaryKey(StockLogDO record);
}