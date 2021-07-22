package com.project.seckill.service;

import com.project.seckill.error.BusinessException;
import com.project.seckill.service.model.UserModel;

/**
 * @author Casterx on 2020/10/2.
 */

public interface UserService {

    //通过用户ID获取用户对象的方法I
    UserModel getUserById(Integer id);


    //通过缓存获取用户对象
    UserModel getUserByIdInCache(Integer id);


    void register(UserModel userModel) throws BusinessException;

    /**
     *
     * @param telphone 用户注册手机
     * @param encrptPassword 用户加密后的密码
     * @throws BusinessException
     */
    UserModel validateLogin(String telphone, String encrptPassword) throws BusinessException;
}
