package com.project.seckill.controller;

import com.project.seckill.error.BusinessException;
import com.project.seckill.response.CommonReturnType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

/**
 * @author Casterx on 2020/10/2.
 */
public class BaseController {

    public static final String CONTENT_TYPE_FORMED="application/x-www-form-urlencoded";

    @RequestMapping(value="/favicon.ico",method={RequestMethod.GET})
    @ResponseBody
    public CommonReturnType favicon()  {
        return null;
    }
//    @ExceptionHandler(Exception.class)
//    @ResponseStatus(HttpStatus.OK)
//    @ResponseBody
//    public Object handlerException(HttpServletRequest request, Exception ex){
//        Map<String,Object> responseData=new HashMap<>();
//        if(ex instanceof BusinessException){
//            BusinessException businessException=(BusinessException)ex;
//            responseData.put("errCode",businessException.getErrCode());
//            responseData.put("errMsg",businessException.getErrMsg());
//        }else{
//            responseData.put("errCode", EmBusinessError.UNKNOWN_ERROR.getErrCode());
//            responseData.put("errMsg", EmBusinessError.UNKNOWN_ERROR.getErrMsg());
//        }
//        return CommonReturnType.create(responseData,"fail");
//
//    }
}
