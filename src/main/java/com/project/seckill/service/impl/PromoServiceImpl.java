package com.project.seckill.service.impl;

import com.project.seckill.dao.PromoDOMapper;
import com.project.seckill.dataobject.PromoDO;
import com.project.seckill.error.BusinessException;
import com.project.seckill.error.EmBusinessError;
import com.project.seckill.service.ItemService;
import com.project.seckill.service.PromoService;
import com.project.seckill.service.UserService;
import com.project.seckill.service.model.ItemModel;
import com.project.seckill.service.model.PromoModel;
import com.project.seckill.service.model.UserModel;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Casterx on 2020/10/8.
 */
@Service
public class PromoServiceImpl implements PromoService {

    @Autowired
    private PromoDOMapper promoDOMapper;


    @Autowired
    private ItemService itemService;

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate redisTemplate;


    @Override
    public void publishPromo(Integer promoId) {
        //通过活动id获取活动
        PromoDO promoDO=promoDOMapper.selectByPrimaryKey(promoId);
        if(promoDO.getItemId()==null|promoDO.getItemId().intValue()==0){
            return ;
        }
        ItemModel itemModel=itemService.getItemById(promoDO.getItemId());
        System.out.println("itemModel="+itemModel);
        //此时默认库存不会发生变化
        //将库存同步到redis内
        redisTemplate.opsForValue().set("promo_item_stock_"+itemModel.getId(),itemModel.getStock());

        //将大闸限制数字放到redis内
        redisTemplate.opsForValue().set("promo_door_count_"+promoId,itemModel.getStock().intValue()*5);

    }

    @Override
    public PromoModel getPromoByItemId(Integer itemId) {
        //获取对应商品的秒杀活动信息
        PromoDO promoDO=promoDOMapper.selectByItemId(itemId);

        //dataobject->model
        PromoModel promoModel=converFromDataObject(promoDO);
        if(promoModel==null){
            return null;
        }

        //判断当前是时间是否秒杀活动即将开始或正在进行
        if(promoModel.getStartDate().isAfterNow()){
            promoModel.setStatus(1);
        }else if(promoModel.getEndDate().isBeforeNow()){
            promoModel.setStatus(3);
        }else{
            promoModel.setStatus(2);
        }
        return promoModel;
    }

    private PromoModel converFromDataObject(PromoDO promoDO){
        if(promoDO==null){
            return null;
        }
        PromoModel promoModel=new PromoModel();
        BeanUtils.copyProperties(promoDO,promoModel);
        promoModel.setPromoItemPrice(new BigDecimal(promoDO.getPromoItemPrice()));
        promoModel.setStartDate(new DateTime(promoDO.getStartDate()));
        promoModel.setEndDate(new DateTime(promoDO.getEndDate()));

        return promoModel;
    }

    @Override
    public String generateSecondKillToken(Integer promoId,Integer itemId,Integer userId) {

        //判断是否库存已售罄，若对应的售罄key存在，则直接返回下单失败
        if(redisTemplate.hasKey("promo_item_stock_invalid_"+itemId)){
            return null;
        }

        PromoDO promoDO=promoDOMapper.selectByPrimaryKey(promoId);

        //dataobject->model
        PromoModel promoModel=converFromDataObject(promoDO);
        if(promoModel==null){
            return null;
        }

        //判断当前是时间是否秒杀活动即将开始或正在进行
        if(promoModel.getStartDate().isAfterNow()){
            promoModel.setStatus(1);
        }else if(promoModel.getEndDate().isBeforeNow()){
            promoModel.setStatus(3);
        }else{
            promoModel.setStatus(2);
        }

        //判断活动是否正在进行
        if(promoModel.getStatus().intValue()!=2){
            return null;
        }

        //判断item信息是否存在
        ItemModel itemModel=itemService.getItemByIdInCache(itemId);
        if(itemModel==null){
            return null;
        }

        //判断user信息是否存在
        UserModel userModel=userService.getUserByIdInCache(userId);
        if(userModel==null){
            return null;
        }

        //获取秒杀大闸的count数量
        long result=redisTemplate.opsForValue().increment("promo_door_count_"+promoId,-1);
        if(result<0){
            return null;
        }


        //生成token并且存入redis内并给一个5分钟生存期
        String token= UUID.randomUUID().toString().replace("-","");
        redisTemplate.opsForValue().set("promo_token_"+promoId+"_userid_"+userId+"_itemid_"+itemId,token);
        redisTemplate.expire("promo_token_"+promoId+"_userid_"+userId+"_itemid_"+itemId,5, TimeUnit.MINUTES);


        return token;

    }
}
