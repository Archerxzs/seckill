package com.project.seckill.error;

/**
 * @author Casterx on 2020/10/2.
 */
public interface CommonError {

    public int getErrCode();
    public String getErrMsg();

    public CommonError setErrMsg(String errMsg);

}
