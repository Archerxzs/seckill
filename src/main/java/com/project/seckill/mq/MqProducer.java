package com.project.seckill.mq;

import com.alibaba.fastjson.JSON;
import com.project.seckill.dao.StockLogDOMapper;
import com.project.seckill.dataobject.StockLogDO;
import com.project.seckill.error.BusinessException;
import com.project.seckill.service.OrderService;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Casterx on 2020/11/9.
 */
@Component
public class MqProducer {


    private DefaultMQProducer producer;

    //为了防止事务提交后失败，导致缓存更改不过来
    private TransactionMQProducer transactionMQProducer;


    @Autowired
    private OrderService orderService;


    @Value("${mq.nameserver.addr}")
    private String nameAddr;


    @Value("${mq.topicname}")
    private String topicName;

    @Autowired
    private StockLogDOMapper stockLogDOMapper;
    

    @PostConstruct
    public void init() throws MQClientException {
        //做mq producer的初始化
        producer=new DefaultMQProducer("producer_group");
        producer.setNamesrvAddr(nameAddr);
        producer.start();

        transactionMQProducer=new TransactionMQProducer("transaction_producer_group");
        transactionMQProducer.setNamesrvAddr(nameAddr);
        transactionMQProducer.start();

        transactionMQProducer.setTransactionListener(new TransactionListener() {
            @Override
            public LocalTransactionState executeLocalTransaction(Message message, Object o) {
                System.out.println("MyProducer：进入到监听器内了");
                //真正要做的事，创建订单
                Integer userId= (Integer) ((Map)o).get("userId");
                Integer itemId= (Integer) ((Map)o).get("itemId");
                Integer promoId= (Integer) ((Map)o).get("promoId");
                Integer amount= (Integer) ((Map)o).get("amount");
                String stockLogId= (String) ((Map)o).get("stockLogId");
                try {
                    //创建订单
                    System.out.println("MyProducer：开始创建订单了");
                    orderService.createOrder(userId,itemId,promoId,amount,stockLogId);
                    //当程序走到这步，突然断联了，那么就不知道该消息的状态是怎样了
                } catch (BusinessException e) {
                    e.printStackTrace();
                    //设置对应的stockLog为回滚状态
                    StockLogDO stockLogDO = stockLogDOMapper.selectByPrimaryKey(stockLogId);
                    stockLogDO.setStatus(3);
                    stockLogDOMapper.updateByPrimaryKeySelective(stockLogDO);
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                    //return出去的时候 出问题了怎么办
                }
                //发送消息到MQ，让消费者去减库存
                return LocalTransactionState.COMMIT_MESSAGE;
                //return出去的时候 出问题了怎么办
            }

            //假设上面的方法返回一个UNKONOWN或是长时间无反应的话，那么下面方法就会回调
            @Override
            public LocalTransactionState checkLocalTransaction(MessageExt messageExt) {
                //根据是否扣减库存成功，来判断要返回COMMIT，ROLLBACK还是继续UNKONOWN
                String jsonString=new String(messageExt.getBody());
                Map<String,Object> map= JSON.parseObject(jsonString,Map.class);
                Integer itemId=(Integer)map.get("itemId");
                Integer amount=(Integer)map.get("amount");
                String stockLogId=(String)map.get("stockLogId");
                StockLogDO stockLogDO = stockLogDOMapper.selectByPrimaryKey(stockLogId);
                if(stockLogDO==null){
                    return LocalTransactionState.UNKNOW;
                }
                if(stockLogDO.getStatus().intValue()==2){
                    return LocalTransactionState.COMMIT_MESSAGE;
                }else if(stockLogDO.getStatus().intValue()==1){
                    return LocalTransactionState.UNKNOW;
                }else{
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }
            }
        });
    }

    //事务性同步库存扣减消息
    //只要数据库事务提交了，对应的消息必定可以发送成功；数据库回滚，消息必定不发送；数据库状态未知，消息要处于pending，等待发送/回滚
    public boolean transactionAsyncReduceStock(Integer userId,Integer itemId,Integer promoId,Integer amount,String stockLogId){
        System.out.println("MyProducer：进入到transactionAsyncReduceStock方法中");
        Map<String,Object> bodyMap=new HashMap<>();
        bodyMap.put("itemId",itemId);
        bodyMap.put("amount",amount);
        bodyMap.put("stockLogId",stockLogId);

        Map<String,Object> argsMap=new HashMap<>();
        argsMap.put("userId",userId);
        argsMap.put("itemId",itemId);
        argsMap.put("promoId",promoId);
        argsMap.put("amount",amount);
        argsMap.put("stockLogId",stockLogId);


        Message message=new Message(topicName,"increase",
                JSON.toJSON(bodyMap).toString().getBytes(Charset.forName("UTF-8")));
        TransactionSendResult sendResult=null;
        try {
            //二阶段提交：
            //发送事务性消息，发送出去后，消息处于prepared状态，该状态下，消息不会被消费者看见
            //在prepared状态下，消息会被保存在MQ中，然后本地会去执行上面回调方法executeLocalTransaction()方法
            //argsMap会被executeLocalTransaction()中的Object o所接受到
            System.out.println("MyProducer：准备发送事务性消息了");
            sendResult=transactionMQProducer.sendMessageInTransaction(message,argsMap);//todo

        } catch (MQClientException e) {
            e.printStackTrace();
            return false;
        }
        if(sendResult.getLocalTransactionState()==LocalTransactionState.ROLLBACK_MESSAGE){
            System.out.println("MyProducer：transactionAsyncReduceStock回滚了");
            return false;
        }else if(sendResult.getLocalTransactionState()==LocalTransactionState.COMMIT_MESSAGE){
            System.out.println("MyProducer：transactionAsyncReduceStock提交了");
            return true;
        }else{
            System.out.println("MyProducer：transactionAsyncReduceStock UNKNOWN了");
            return false;
        }
    }


    //同步库存扣减消息
    //这个方法非事务性
    public boolean asyncReduceStock(Integer itemId,Integer amount) {
        Map<String,Object> bodyMap=new HashMap<>();
        bodyMap.put("itemId",itemId);
        bodyMap.put("amount",amount);
        Message message=new Message(topicName,"increase",
                JSON.toJSON(bodyMap).toString().getBytes(Charset.forName("UTF-8")));

        try {
            //不管怎样，直接发送消息，发出去后消费端就可以得到消费的通知去消费
            producer.send(message);
        } catch (MQClientException e) {
            e.printStackTrace();
            return false;
        } catch (RemotingException e) {
            e.printStackTrace();
            return false;
        } catch (MQBrokerException e) {
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}
