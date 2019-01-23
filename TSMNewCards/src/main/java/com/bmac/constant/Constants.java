package com.bmac.constant;

/**
 * @Author: zqk
 * @Date: 2018/11/28 15:51
 * @Description: 常量接口
 */

public interface Constants {

    String EXCEPTION_APP_NAME = "安卓交易错误数和安卓交易超时次数"; //指标名字
    
    String TIMEOUT_APP_NAME = "安卓交易超时次数"; //指标名字

    //kafka_broker_list
    String KAFKA_BROKER_LIST = "metadata.broker.list";
    //要连接的kafka节点
    String KAFKA_META_BROKER_LIST = "kafka.metadata.broker.list";
    
    //交易错误次数消费者组id
    String EXCEPTION_GROUP_ID = "exception.groupid";
    
    //交易超时次数消费者组id
    String TIMEOUT_GROUP_ID = "timeout.groupid";

    //kafka的topics
    String KAFKA_TOPICS = "kafka.topics";

    //10分钟内，连续出现内部链接节点超时超过次数3次的窗口长度 10分钟
    int SPARK_DURATION_MINUTES = 1;

    //滑动间隔 10秒
    int SPARK_DURATION_SECONDS = 60;

    //10分钟内，连续出现内部链接节点超时超过次数 设置为3次，
    int TIME_OUT_NUM = 3;

    //10分钟内，连续累计业务异常报错超过次数， 设置为5次
    int TRADE_ERROR_NUM = 5;

}
