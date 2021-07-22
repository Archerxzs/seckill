package com.project.seckill.service;

/**
 * 封装本地缓存操作类
 * @author Casterx on 2020/11/6.
 */
public interface CacheService {

    //存方法
    void setCommonCache(String key,Object value);

    Object getFromCommonCache(String key);
}
