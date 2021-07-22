package com.project.seckill.service.impl;

import com.project.seckill.dao.OrderDOMapper;
import com.project.seckill.dao.SequenceDOMapper;
import com.project.seckill.dao.StockLogDOMapper;
import com.project.seckill.dataobject.OrderDO;
import com.project.seckill.dataobject.SequenceDO;
import com.project.seckill.dataobject.StockLogDO;
import com.project.seckill.error.BusinessException;
import com.project.seckill.error.EmBusinessError;
import com.project.seckill.service.ItemService;
import com.project.seckill.service.OrderService;
import com.project.seckill.service.UserService;
import com.project.seckill.service.model.ItemModel;
import com.project.seckill.service.model.OrderModel;
import com.project.seckill.service.model.UserModel;
import org.apache.ibatis.transaction.Transaction;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author Casterx on 2020/10/8.
 */
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderDOMapper orderDOMapper;

    @Autowired
    private ItemService itemService;

    @Autowired
    private UserService userService;

    @Autowired
    private SequenceDOMapper sequenceDOMapper;

    @Autowired
    private StockLogDOMapper stockLogDOMapper;


    @Override
    @Transactional
    public OrderModel createOrder(Integer userId, Integer itemId, Integer promoId, Integer amount,String stockLogId) throws BusinessException {
        //1.校验下单状态，下单的商品是否存在，用户是否合法，购买数量是否正确
        ItemModel itemModel=itemService.getItemByIdInCache(itemId);
        if(itemModel==null){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"商品信息不存在");
        }

//        UserModel userModel=userService.getUserByIdInCache(userId);
//        if(userModel==null){
//            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"用户信息不存在");
//        }
        if(amount<=0||amount>99){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"数量信息不正确");
        }
        //校验活动信息
//        if(promoId!=null){
//            //（1）校验对应活动是否存在这个适用商品
//            if(promoId.intValue()!=itemModel.getPromoModel().getId()){
//                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"活动信息不正确");
//            }else if(itemModel.getPromoModel().getStatus().intValue()!=2){//（2）校验活动是否正在进行中
//                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"活动还未开始");
//            }
//        }

        System.out.println("OrderServiceImpl：开始减缓存内的库存了");
        //2.落单减库存（或者支付减库存） 扣减缓存内的库存
        boolean result=itemService.decreaseStock(itemId,amount);
        if(!result){
            throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH);
        }

        //3.订单入库
        OrderModel orderModel=new OrderModel();
        orderModel.setUserId(userId);
        orderModel.setItemId(itemId);
        orderModel.setAmount(amount);
        if(promoId!=null){
            orderModel.setItemPrice(itemModel.getPromoModel().getPromoItemPrice());
        }else{
            orderModel.setItemPrice(itemModel.getPrice());
        }
        orderModel.setPromoId(promoId);
        orderModel.setOrderPrice(orderModel.getItemPrice().multiply(new BigDecimal(amount)));

        //生产交易流水号，订单号
        String no=generateOrderNo();
        orderModel.setId(no);
        OrderDO orderDO=convertFromOrderModel(orderModel);
        System.out.println("OrderServiceImpl：数据库内添加订单成功");
        orderDOMapper.insertSelective(orderDO);

        //加上商品的销量
        itemService.increaseSales(itemId,amount);


        //设置库存流水状态为成功
        StockLogDO stockLogDO=stockLogDOMapper.selectByPrimaryKey(stockLogId);
        if(stockLogDO==null){
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR);
        }

        stockLogDO.setStatus(2);
        stockLogDOMapper.updateByPrimaryKeySelective(stockLogDO);





        //4.返回前端
        return orderModel;
    }

    //开启一个新的事务并提交，不管上述的事务如何
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private String generateOrderNo(){
        //订单号有16位
        StringBuilder stringBuilder=new StringBuilder();

        //前8位为时间信息，年月日
        LocalDateTime now= LocalDateTime.now();
        String nowDate=now.format(DateTimeFormatter.ISO_DATE).replace("-","");
        stringBuilder.append(nowDate);

        //中间6位为自增序列
        //获取当前sequence
        int sequence=0;
        SequenceDO sequenceDO=sequenceDOMapper.getSequenceByName("order_info");
        sequence=sequenceDO.getCurrentValue();
        sequenceDO.setCurrentValue(sequenceDO.getCurrentValue()+sequenceDO.getStep());
        sequenceDOMapper.updateByPrimaryKeySelective(sequenceDO);
        String sequenceStr=String.valueOf(sequence);
        for(int i=0;i<6-sequenceStr.length();i++){
            stringBuilder.append(0);
        }
        stringBuilder.append(sequenceStr);

        //最后2位为分库分表位00-99 暂时写死
        stringBuilder.append("00");
        return stringBuilder.toString();
    }

    private OrderDO convertFromOrderModel(OrderModel orderModel){
        if(orderModel==null){
            return null;
        }
        OrderDO orderDO=new OrderDO();
        BeanUtils.copyProperties(orderModel,orderDO);
        orderDO.setItemPrice(orderModel.getItemPrice().doubleValue());
        orderDO.setOrderPrice(orderModel.getOrderPrice().doubleValue());
        return orderDO;
    }

}
