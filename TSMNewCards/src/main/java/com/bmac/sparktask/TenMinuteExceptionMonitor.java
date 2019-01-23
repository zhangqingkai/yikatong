package com.bmac.sparktask;

import com.alibaba.fastjson.JSONObject;
import com.bmac.conf.ConfigurationManager;
import com.bmac.constant.Constants;
import com.bmac.dao.impl.MyTestStreamingListener;
import com.bmac.entity.SoAndroidError;
import com.bmac.entity.SoAndroidTime;
import com.bmac.entity.SoEarlyWarning;
import com.bmac.utils.DingTalkUtils;
import com.bmac.utils.MyBatisUtils;
import com.bmac.utils.RedisUtils;

import kafka.serializer.StringDecoder;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.storage.StorageLevel;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaPairInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka.KafkaUtils;

import scala.Serializable;
import scala.Tuple2;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @Author: zqk
 * @Date: 2018/12/01 10:59
 * @Description: 交易错误数交易超时次数
 */

public class TenMinuteExceptionMonitor implements Serializable{

    private static final long serialVersionUID = 8016728007281902043L;
    private String group = ConfigurationManager.getProperty(Constants.EXCEPTION_GROUP_ID);
    private  int rate;//频率 10分钟(标准条件)
    private  int exceptionTimes;//10分钟内5次异常(标准条件)
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date errorTime;
    int warnningQuota1,warnningDataId1 = 0;
    int warnningQuota2,warnningDataId2 = 0;
    int flagStatus1 = 2; //系统状态解除报警标识及操作状态标识(Exception)
    int flagStatus2 = 2; //(TimeOut)
    int flagOperStatus1; //人工解除预警标识(Exception)
    int flagOperStatus2; //人工解除预警标识(TimeOut)
    private static int errorTimes;//10分钟内3次超时(标准条件)
    public void tenMinuteExceptionMonitor() {
        SparkConf conf = new SparkConf();
        conf.setAppName(Constants.EXCEPTION_APP_NAME);
//        conf.setMaster("local[2]");
        			
        JavaStreamingContext jsc = new JavaStreamingContext(conf, Durations.seconds(10));

        //时间监听
        final MyTestStreamingListener myTestStreamingListener = new MyTestStreamingListener();
        jsc.addStreamingListener(myTestStreamingListener);
        //构建请求kafka的参数
        Map<String, String> kafkaParams = new HashMap<String, String>();
        kafkaParams.put(Constants.KAFKA_BROKER_LIST, ConfigurationManager.getProperty(Constants.KAFKA_META_BROKER_LIST));
        kafkaParams.put("group.id", group);
        kafkaParams.put("enable.auto.commit", "true"); //使用kafka自动提交模式，由应用程序自己接管offset

        // 构建topics
        String kafkaTopics = ConfigurationManager.getProperty(Constants.KAFKA_TOPICS);
        String[] kafkaTopicSplited = kafkaTopics.split(",");
        
        // 存储topics
        Set<String> topics = new HashSet<String>();
        for (String topic : kafkaTopicSplited) {
            topics.add(topic);
        }

        //以Direct的方式读取
        JavaPairInputDStream<String, String> realTimeDStream = KafkaUtils.createDirectStream(
        		jsc,
        		String.class,
                String.class,
                StringDecoder.class,
                StringDecoder.class,
                kafkaParams,
                topics
                );
        
        rate = RedisUtils.getTakeRateException();
        //设置窗口函数
        JavaPairDStream<String, String> realTimeDstreamWindow = realTimeDStream.window(
                Durations.minutes(Long.valueOf(rate)),Durations.seconds(Constants.SPARK_DURATION_SECONDS));
       
        //需要先按时间对数据进行过滤(防止OGG宕机,然后推过来历史数据)
//        JavaPairDStream<String, String> realTimeDstreamWindowFilterd = realTimeDstreamWindow.filter(new Function<Tuple2<String,String>, Boolean>() {
//			private static final long serialVersionUID = 1L;
//			public Boolean call(Tuple2<String, String> tuple2) throws Exception {
//		        try {
//		        	long time = myTestStreamingListener.getWatchStartTime().getTime();
//			        long timeStar = time - (rate * 60 * 1000);
//
//					JSONObject jsonObject = JSONObject.parseObject(tuple2._2);
//					String errorTime = jsonObject.getString("ErrorTime");
//					long errorTime1 = simpleDateFormat.parse(errorTime.substring(0, 19)).getTime();
//					return errorTime1 >= timeStar;
//		        }catch(Exception e) {
//		        	return false;
//		        }
//			}
//		});
        //Exception
        //生成(response_code, 所有字段的数据)
        JavaPairDStream<String, String> lines = realTimeDstreamWindow.flatMapToPair(new PairFlatMapFunction<Tuple2<String,String>, String, String>() {
			private static final long serialVersionUID = 1L;
			public Iterator<Tuple2<String, String>> call(Tuple2<String, String> tuple2) throws Exception {
				try {
					JSONObject jsonObject = JSONObject.parseObject(tuple2._2);
	                String responseCode = jsonObject.getString("ResponseCode"); 
	                return Arrays.asList(new Tuple2<String, String>(responseCode, tuple2._2)).iterator();
				}catch(Exception e) {
					return null;
				}
			}
		});
        
        //缓存起来，两个指标要用到
        JavaPairDStream<String, String> persist = lines.persist(StorageLevel.MEMORY_AND_DISK());
        /**
         * 交易错误数
         */
        //过滤数据
        JavaPairDStream<String, String> responseCodeNotInTarget = persist.filter(new Function<Tuple2<String, String>, Boolean>() {
            private static final long serialVersionUID = 3952246369368747636L;

			public Boolean call(Tuple2<String, String> tuple2) throws Exception {
                String responseCode = tuple2._1;
                return !responseCode.equals("0198") && !responseCode.equals("0199") &&
                        !responseCode.equals("0298") && !responseCode.equals("0299") &&
                        !responseCode.equals("4998") && !responseCode.equals("4999") &&
                        !responseCode.equals("0181") && !responseCode.equals("0182") &&
                        !responseCode.equals("0183") && !responseCode.equals("0185") &&
                        !responseCode.equals("0186") && !responseCode.equals("0188");
            }
        });

        //将过滤的数据缓存起来，一个是so_early_warnning要用到，一个是so_android_error要用
        JavaPairDStream<String, String> persist1 = responseCodeNotInTarget.persist(StorageLevel.MEMORY_AND_DISK());

        //将数据生成("ExceptionNum", 1)的数据，进行聚合
        JavaPairDStream<String, Integer> errorNumTuple = persist1.mapToPair(new PairFunction<Tuple2<String, String>, String, Integer>() {
            private static final long serialVersionUID = -6288055682090558568L;

			public Tuple2<String, Integer> call(Tuple2<String, String> tuple2) throws Exception {

                return new Tuple2<String, Integer>("ExceptionNum", 1);
            }
        });

        //聚合
        JavaPairDStream<String, Integer> errorNumTupleSumd =  errorNumTuple.reduceByKey(new Function2<Integer, Integer, Integer>() {
            private static final long serialVersionUID = -7207158770759333378L;

			public Integer call(Integer v1, Integer v2) throws Exception {

                return v1 + v2;
            }
        });

        //将数据存储
        errorNumTupleSumd.foreachRDD(new VoidFunction<JavaPairRDD<String, Integer>>() {
            private static final long serialVersionUID = 4209766177770847319L;
			public void call(JavaPairRDD<String, Integer> rdd) throws Exception {
               try {
            	   List<Tuple2<String, Integer>> collect = rdd.collect();
                   errorTime = new Date(System.currentTimeMillis());
   				Long createTimeLong = errorTime.getTime();
   				String createTimeString = simpleDateFormat.format(errorTime).substring(0, 11);
   				//从reids中获取指标生效时间、失效时间、预警时间段(开始、结束)
   				String exceptionNumStartTime = RedisUtils.getExceptionNumStartTime();
   				String exceptionNumEndTime = RedisUtils.getExceptionNumEndTime();
   				String exceptionNumTakeTime = RedisUtils.getExceptionNumTakeTime();
   				String exceptionNumInvalidTime = RedisUtils.getExceptionNumInvalidTime();
   				Long exceptionNumStartTimeLong = simpleDateFormat.parse(exceptionNumStartTime).getTime();
   				Long exceptionNumEndTimeLong = simpleDateFormat.parse(exceptionNumEndTime).getTime();
   				Long exceptionNumTakeTimeLong = simpleDateFormat.parse(createTimeString + exceptionNumTakeTime).getTime();
   				Long exceptionNumInvalidTimeLong = simpleDateFormat.parse(createTimeString + exceptionNumInvalidTime).getTime();
                int errorNum = 0; //条件 5次
                if(createTimeLong >= exceptionNumStartTimeLong && createTimeLong <= exceptionNumEndTimeLong
                   		&& createTimeLong >= exceptionNumTakeTimeLong && createTimeLong <= exceptionNumInvalidTimeLong) {
                   	for (Tuple2<String, Integer> tuple2 : collect) {
                       	// 从redis中取值(标准值比较值 错误数)
                           exceptionTimes = RedisUtils.getwarningPresentExceptionNum();
                           errorNum = tuple2._2;
                           SoEarlyWarning soEarlyWarning = new SoEarlyWarning();
                           
                           if (errorNum >= Long.valueOf(exceptionTimes)) {
                               //soEarlyWarning
                               warnningQuota1 = errorNum;
                               soEarlyWarning.setQuotaCode("YJ005");
                               soEarlyWarning.setQuotaPresent(errorNum);
                               soEarlyWarning.setWarningPresent(exceptionTimes);
       						   soEarlyWarning.setStatus(0); //报警
       						   flagStatus1 = 0;
                               soEarlyWarning.setType("34");//android指标
                               soEarlyWarning.setCreateTime(errorTime);
                               soEarlyWarning.setCreateUser("ExceptionMonitor");
       						   soEarlyWarning.setOperStatus(0); //预警未解除
                               soEarlyWarning.setUpdateTime(errorTime);
                               soEarlyWarning.setUpdateUser("ExceptionMonitor");
                               SqlSessionFactory sqlSessionFactory = MyBatisUtils.getSqlSessionFactory();
                               SqlSession sqlSession = sqlSessionFactory.openSession();
                               sqlSession.insert("insertToSoEarlyWarning", soEarlyWarning);
                               try {
                                   sqlSession.commit();
                               } catch (Exception e) {
                                   e.printStackTrace();
                               }finally {
                                   sqlSession.close();
                               }

                               warnningDataId1 = soEarlyWarning.getId();
                           }else if(errorNum > 0 && errorNum < Long.valueOf(exceptionTimes)){
                               //soEarlyWarning
                               warnningQuota1 = errorNum;
                               soEarlyWarning.setQuotaCode("YJ005");
                               soEarlyWarning.setQuotaPresent(errorNum);
                               soEarlyWarning.setWarningPresent(exceptionTimes);
                               if (flagStatus1 == 0) {
                                   soEarlyWarning.setStatus(1); //解除
                               } else {
                                   soEarlyWarning.setStatus(2); //正常
                               }
                               soEarlyWarning.setType("34");//android指标
                               soEarlyWarning.setCreateTime(errorTime);
                               soEarlyWarning.setCreateUser("ExceptionMonitor");
                               if (flagStatus1 == 0) {
                                   soEarlyWarning.setOperStatus(1); //系统自动解除预警
                                   flagStatus1 = 1;
                               } else {
                                   soEarlyWarning.setOperStatus(3); //正常
                                   flagStatus1 = 2;
                               }
                               soEarlyWarning.setUpdateTime(errorTime);
                               soEarlyWarning.setUpdateUser("ExceptionMonitor");
                               SqlSessionFactory sqlSessionFactory = MyBatisUtils.getSqlSessionFactory();
                               SqlSession sqlSession = sqlSessionFactory.openSession();
                               sqlSession.insert("insertToSoEarlyWarning", soEarlyWarning);
                               try {
                                   sqlSession.commit();
                               } catch (Exception e) {
                                   e.printStackTrace();
                               }finally {
                                   sqlSession.close();
                               }
                               warnningDataId1 = soEarlyWarning.getId();
                           }
                           //从redis中拿取操作状态值，如果是人工解除预警，将预警下的status
                           flagOperStatus1 = RedisUtils.getExceptionNumOperStatus();
                           //数据插入数据库后，按条件进行预警
                           if (flagOperStatus1 == 2) { //是从redis里拿的数据
                               //donothing 不发预警了
                           } else {
                               if (flagStatus1 == 0 && errorNum >= Long.valueOf(exceptionTimes)) { //预警
                                   String message = "{'msgtype':'text','text':{'content':"
                                           + "'服务器:安卓TSM平台"
                                           + "\\r\\n发生了:交易错误数报警"
                                           + "\\r\\n故障级别:报警/P3"
                                           + "\\r\\n故障状态:PROBLEM"
                                           + "\\r\\n内容为:当前" + rate + "分钟内,交易错误次数为" + errorNum + "次;"
                                           + "当前报警指标值为" + exceptionTimes + "次;" + "创建预警时间为" + simpleDateFormat.format(errorTime)
                                           + "\\r\\n事件ID:" + warnningDataId1 +"'}}";
                                   DingTalkUtils.earlyWarning(message);
                               } else if (flagStatus1 == 1 && errorNum > 0 && errorNum < Long.valueOf(exceptionTimes)) { //系统自动解除预警
                                   String message = "{'msgtype':'text','text':{'content':"
                                           + "'服务器:安卓TSM平台"
                                           + "\\r\\n发生了:交易错误数报警自动解除" 
                                           + "\\r\\n故障级别:解除/P3"
                                           + "\\r\\n故障状态:OK"
                                           + "\\r\\n内容为:当前" + rate + "分钟内,交易错误次数为" + errorNum + "次;"
                                           + "当前报警指标值为" + exceptionTimes + "次;" + "解除预警时间为" + simpleDateFormat.format(errorTime)
                                           + "\\r\\n事件ID:" + warnningDataId1 +"'}}";
                                   DingTalkUtils.earlyWarning(message);
                               }
                           }
                       }
                   }
               }catch(Exception e) {
            	   e.printStackTrace();
               }
            }
        });

        //将明细数据走so_android_time库
        persist1.foreachRDD(new VoidFunction<JavaPairRDD<String, String>>() {
            private static final long serialVersionUID = 2572855772563032098L;

			public void call(JavaPairRDD<String, String> rdd) throws Exception {
                try {
                	List<Tuple2<String, String>> collect = rdd.collect();
    				Long createTimeLong = errorTime.getTime();
    				String createTimeString = simpleDateFormat.format(errorTime).substring(0, 11);
    				//从reids中获取指标生效时间、失效时间、预警时间段(开始、结束)
    				String exceptionNumStartTime = RedisUtils.getExceptionNumStartTime();
    				String exceptionNumEndTime = RedisUtils.getExceptionNumEndTime();
    				String exceptionNumTakeTime = RedisUtils.getExceptionNumTakeTime();
    				String exceptionNumInvalidTime = RedisUtils.getExceptionNumInvalidTime();
    				Long exceptionNumStartTimeLong = simpleDateFormat.parse(exceptionNumStartTime).getTime();
    				Long exceptionNumEndTimeLong = simpleDateFormat.parse(exceptionNumEndTime).getTime();
    				Long exceptionNumTakeTimeLong = simpleDateFormat.parse(createTimeString + exceptionNumTakeTime).getTime();
    				Long exceptionNumInvalidTimeLong = simpleDateFormat.parse(createTimeString + exceptionNumInvalidTime).getTime();
    				//判断指标预警时间段
    				if(createTimeLong >= exceptionNumStartTimeLong && createTimeLong <= exceptionNumEndTimeLong
    						&& createTimeLong >= exceptionNumTakeTimeLong && createTimeLong <= exceptionNumInvalidTimeLong) {
    					if(collect.size() > 0) {
    						SqlSessionFactory sqlSessionFactory = MyBatisUtils.getSqlSessionFactory();
    	                    SqlSession sqlSession1 = sqlSessionFactory.openSession();
    						for (Tuple2<String, String> tuple2 : collect) {
        						JSONObject jsonObject = JSONObject.parseObject(tuple2._2);
        	                    Date createTime = new Date(System.currentTimeMillis());
        	                    //正常情况下 用jsonObject.get()得到它的明细数据，给它封装到对象中
        	                    SoAndroidError soAndroidError = new SoAndroidError();
        	                    // 正常情况
        	                    soAndroidError.setSellNo(jsonObject.getString("SellNo"));
        	                    soAndroidError.setCardCode(jsonObject.getString("CardCode"));
        	                    soAndroidError.setUserMobile(jsonObject.getString("UserMobile"));
        	                    soAndroidError.setDeviceID(jsonObject.getString("DeviceId"));
        	                    soAndroidError.setResponseCode(tuple2._1);
        	                    soAndroidError.setMessage(jsonObject.getString("Message"));

        	                    soAndroidError.setErrorTime(simpleDateFormat.parse((jsonObject.getString("ErrorTime")).substring(0, 19)));
        	                    soAndroidError.setQuotaPresent(warnningQuota1);
        	                    soAndroidError.setCreateTime(createTime);
        	                    
        	            		long time = myTestStreamingListener.getWatchStartTime().getTime();
        	            		long timeStar = time - (rate * 60 * 1000);
        	                    soAndroidError.setStartTime(new Timestamp(timeStar));
        	                    soAndroidError.setEndTime(new Timestamp(time));
        	                    soAndroidError.setWariningID(warnningDataId1);

        	                    sqlSession1.insert("insertToSoAndroidError",soAndroidError);
        	                }
        	                try {
        	                    sqlSession1.commit();
        	                } catch (Exception e) {
        	                    e.printStackTrace();
        	                }finally {
        	                    sqlSession1.close();
        	                }
    					}
    				}
                }catch(Exception e) {
                	e.printStackTrace();
                }
            }
        });
        
        //TimeOut
        //报警数据先进行过滤，剩下的都是有问题连接异常的数据
        JavaPairDStream<String, String> filtered = persist.filter(new Function<Tuple2<String, String>, Boolean>() {
            private static final long serialVersionUID = -3302292144646922821L;

			public Boolean call(Tuple2<String, String> tuple2) throws Exception {
                String s = tuple2._1;
                return s.equals("0198") || s.equals("0199") || s.equals("0298")
                        || s.equals("0299") || s.equals("4998") || s.equals("4999");
            }
        });

        //将过滤后的数据缓存起来，后面两次用到
        JavaPairDStream<String, String> persist2 = filtered.persist(StorageLevel.MEMORY_AND_DISK());

        //接下来分两步走
        //第一步走so_early_warning 库
        //剩下的所有数据都是连接超时的数据了，生成("timeOut", 1)，进行聚合
        JavaPairDStream<String, Integer> timeOutTuple = persist2.mapToPair(new PairFunction<Tuple2<String, String>, String, Integer>() {
            private static final long serialVersionUID = -4162335378298019871L;

			public Tuple2<String, Integer> call(Tuple2<String, String> tuple2) throws Exception {

                return new Tuple2<String, Integer>("timeOut", 1);
            }
        });

        //进行聚合
        JavaPairDStream<String, Integer> timeOutSumed = timeOutTuple.reduceByKey(new Function2<Integer, Integer, Integer>() {
            private static final long serialVersionUID = -5824057116803137099L;

            public Integer call(Integer v1, Integer v2) throws Exception {
                return v1 + v2;
            }
        });

        //思路：将正常的数据生成(响应码， 所有字段)类型， 先根据响应码进行过滤，剩下的数据
        //都是有问题的数据，将剩下的数据分两步走，一部分生成("timeOut", 1)类型，调用reduceByKey
        //再调用window算子，走so_early_warning库，另一部分取所有字段即明细数据，走so_android_time库
        timeOutSumed.foreachRDD(new VoidFunction<JavaPairRDD<String, Integer>>() {
            private static final long serialVersionUID = 8515083313165243140L;

            public void call(JavaPairRDD<String, Integer> rdd) throws Exception {
                try {
                	List<Tuple2<String, Integer>> collect = rdd.collect();
                    int timeOutNum = 0;
                    //报警时间
                    errorTime = new Date(System.currentTimeMillis());
    				Long createTimeLong = errorTime.getTime();
    				String createTimeString = simpleDateFormat.format(errorTime).substring(0, 11);
    				//从reids中获取指标生效时间、失效时间、预警时间段(开始、结束)
    				String timeOutNumStartTime = RedisUtils.getTimeOutStartTime();
    				String timeOutNumEndTime = RedisUtils.getTimeOutEndTime();
    				String timeOutNumTakeTime = RedisUtils.getTimeOutTakeTime();
    				String timeOutNumInvalidTime = RedisUtils.getTimeOutInvalidTime();
    				Long timeOutNumStartTimeLong = simpleDateFormat.parse(timeOutNumStartTime).getTime();
    				Long timeOutNumEndTimeLong = simpleDateFormat.parse(timeOutNumEndTime).getTime();
    				Long timeOutNumTakeTimeLong = simpleDateFormat.parse(createTimeString + timeOutNumTakeTime).getTime();
    				Long timeOutNumInvalidTimeLong = simpleDateFormat.parse(createTimeString + timeOutNumInvalidTime).getTime();
    				//判断指标预警时间
    				if(createTimeLong >= timeOutNumStartTimeLong && createTimeLong <= timeOutNumEndTimeLong
    						&& createTimeLong >= timeOutNumTakeTimeLong && createTimeLong <= timeOutNumInvalidTimeLong) {
    	                for (Tuple2<String, Integer> tuple2 : collect) {
    	                     timeOutNum = tuple2._2;
    	                     SoEarlyWarning soEarlyWarning = new SoEarlyWarning();
    	                     //从redis中拿取值(标准值比较值)
    	                     errorTimes = RedisUtils.getwarningPresentTimeOutNum(); //3次
    	                     if (timeOutNum >= Long.valueOf(errorTimes)) {
    	                        //soEarlyWarning
    	                        warnningQuota2 = timeOutNum;
    	                        
    	                        soEarlyWarning.setQuotaCode("YJ002");
    	                        soEarlyWarning.setQuotaPresent(timeOutNum);
    	                        soEarlyWarning.setWarningPresent(errorTimes);
    							soEarlyWarning.setStatus(0);
    							flagStatus2 = 0;
    							
    	                        soEarlyWarning.setType("34");//android指标
    	                        soEarlyWarning.setCreateTime(errorTime);
    	                        soEarlyWarning.setCreateUser("TimeOutNumsMonitor");
    	                        soEarlyWarning.setUpdateTime(errorTime);
    	                        soEarlyWarning.setUpdateUser("TimeOutNumsMonitor");
    							soEarlyWarning.setOperStatus(0); //预警
    							SqlSessionFactory sqlSessionFactory = MyBatisUtils.getSqlSessionFactory();
    	                        SqlSession sqlSession = sqlSessionFactory.openSession();
    	                        sqlSession.insert("insertToSoEarlyWarning", soEarlyWarning);
    	                        try {
    	                            sqlSession.commit();
    	                        } catch (Exception e) {
    	                        	sqlSession.rollback();
    	                            e.printStackTrace();
    	                        }finally {
    	                            sqlSession.close();
    	                        }

    	                        warnningDataId2 = soEarlyWarning.getId();
    	                     }else if(timeOutNum > 0 && timeOutNum < Long.valueOf(errorTimes)){
    	                        //soEarlyWarning
    	                        warnningQuota2 = timeOutNum;
    	                        
    	                        soEarlyWarning.setQuotaCode("YJ002");
    	                        soEarlyWarning.setQuotaPresent(timeOutNum);
    	                        soEarlyWarning.setWarningPresent(errorTimes);
    	                        if (flagStatus2 == 0) {
    	                            soEarlyWarning.setStatus(1); //系统自动解除
    	                        } else {
    	                            soEarlyWarning.setStatus(2);
    	                        }
    	                        soEarlyWarning.setType("34");//android指标
    	                        soEarlyWarning.setCreateTime(errorTime);
    	                        soEarlyWarning.setCreateUser("TimeOutNumsMonitor");
    	                        soEarlyWarning.setUpdateTime(errorTime);
    	                        soEarlyWarning.setUpdateUser("TimeOutNumsMonitor");
    	                        if (flagStatus2 == 0){
    	                            soEarlyWarning.setOperStatus(1); //系统自动解除
    	                            flagStatus2 = 1;
    	                        }else {
    	                            soEarlyWarning.setOperStatus(3); //正常
    	                            flagStatus2 = 2;
    	                        }
    	                        SqlSessionFactory sqlSessionFactory = MyBatisUtils.getSqlSessionFactory();
    	                        SqlSession sqlSession = sqlSessionFactory.openSession();
    	                        sqlSession.insert("insertToSoEarlyWarning", soEarlyWarning);
    	                        try {
    	                            sqlSession.commit();
    	                        } catch (Exception e) {
    	                        	sqlSession.rollback();
    	                            e.printStackTrace();
    	                        }finally {
    	                            sqlSession.close();
    	                        }
    	                        warnningDataId2 = soEarlyWarning.getId();
    	                    }
    	                    //从redis中拿取操作状态值
    	                    flagOperStatus2 = RedisUtils.getTimeOutNumOperStatus();
    	                    //数据插入数据库后，按条件进行预警
    	                    if (flagOperStatus2 == 2) { //是从redis里拿的数据
    	                        //donothing 不发预警了
    	                    } else if(flagOperStatus2 == 0) {
    	                        if (flagStatus2 == 0 && timeOutNum >= Long.valueOf(errorTimes)) { //预警
    	                            String message = "{'msgtype':'text','text':{'content':"
    	                                    + "'服务器:安卓TSM平台"
    	                                    + "\\r\\n发生了:交易超时次数报警" 
    	                                    + "\\r\\n故障级别:报警/P2"
    	                                    + "\\r\\n故障状态:PROBLEM"
    	                                    + "\\r\\n内容为:当前" + rate + "分钟内,交易错误次数为" + timeOutNum + "次;"
    	                                    + "当前报警指标值为" + errorTimes + "次;" + "创建报警时间为" + simpleDateFormat.format(errorTime)
    	                                    + "\\r\\n事件ID:" + warnningDataId2 +"'}}";
    	                            DingTalkUtils.earlyWarning(message);
    	                        } else if (flagStatus2 == 1 && timeOutNum > 0 && timeOutNum < Long.valueOf(errorTimes)) { //系统自动解除预警
    	                            String message = "{'msgtype':'text','text':{'content':"
    	                                    + "'服务器:安卓TSM平台"
    	                                    + "\\r\\n发生了:交易超时次数报警自动解除" 
    	                                    + "\\r\\n故障级别:解除/P2"
    	                                    + "\\r\\n故障状态:OK"
    	                                    + "\\r\\n内容为:当前" + rate + "分钟内,交易错误次数为" + timeOutNum + "次;"
    	                                    + "当前报警指标值为" + errorTimes + "次;" + "预警解除时间为" + simpleDateFormat.format(errorTime)
    	                                    + "\\r\\n\\r\\n事件ID:" + warnningDataId2 +"'}}";
    	                            DingTalkUtils.earlyWarning(message);
    	                        }
    	                    }
    	                }
    				}
                }catch(Exception e) {
                	e.printStackTrace();
                }
            }
        });

        //第二步 明细数据走so_android_time库
        persist2.foreachRDD(new VoidFunction<JavaPairRDD<String, String>>() {
            private static final long serialVersionUID = -1912870696655562553L;

            public void call(JavaPairRDD<String, String> rdd) throws Exception {
                try {
                	List<Tuple2<String, String>> list = rdd.collect();
                	errorTime = new Date(System.currentTimeMillis());
    				Long createTimeLong = errorTime.getTime();
    				String createTimeString = simpleDateFormat.format(errorTime).substring(0, 11);
    				//从reids中获取指标生效时间、失效时间、预警时间段(开始、结束)
    				String timeOutNumStartTime = RedisUtils.getTimeOutStartTime();
    				String timeOutNumEndTime = RedisUtils.getTimeOutEndTime();
    				String timeOutNumTakeTime = RedisUtils.getTimeOutTakeTime();
    				String timeOutNumInvalidTime = RedisUtils.getTimeOutInvalidTime();
    				Long timeOutNumStartTimeLong = simpleDateFormat.parse(timeOutNumStartTime).getTime();
    				Long timeOutNumEndTimeLong = simpleDateFormat.parse(timeOutNumEndTime).getTime();
    				Long timeOutNumTakeTimeLong = simpleDateFormat.parse(createTimeString + timeOutNumTakeTime).getTime();
    				Long timeOutNumInvalidTimeLong = simpleDateFormat.parse(createTimeString + timeOutNumInvalidTime).getTime();
    				//判断指标生效时间
    				if(createTimeLong >= timeOutNumStartTimeLong && createTimeLong <= timeOutNumEndTimeLong
    						&& createTimeLong >= timeOutNumTakeTimeLong && createTimeLong <= timeOutNumInvalidTimeLong) {
    	                if(list.size() > 0) {
    	                	SqlSessionFactory sqlSessionFactory = MyBatisUtils.getSqlSessionFactory();
    	                	SqlSession sqlSession1 = sqlSessionFactory.openSession();
    	                	for (Tuple2<String, String> tuple2 : list) {
    		                    JSONObject jsonObject = JSONObject.parseObject(tuple2._2);
    		                    Date createTime = new Date(System.currentTimeMillis());
    		                    //正常情况下 用jsonObject.get()得到它的明细数据，给它封装到对象中
    		                    SoAndroidTime soAndroidTime = new SoAndroidTime();
    		                    // 正常情况
    		                    soAndroidTime.setSellNo(jsonObject.getString("SellNo"));
    		                    soAndroidTime.setCardCode(jsonObject.getString("CardCode"));
    		                    soAndroidTime.setUserMobile(jsonObject.getString("UserMobile"));
    		                    soAndroidTime.setDeviceID(jsonObject.getString("DeviceId"));
    		                    soAndroidTime.setResponseCode(tuple2._1);
    		                    soAndroidTime.setMessage(jsonObject.getString("Message"));
    		                    
    		                    soAndroidTime.setErrorTime(simpleDateFormat.parse((jsonObject.getString("ErrorTime")).substring(0, 19)));
    		                    soAndroidTime.setQuotaPresent(warnningQuota2);
    		                    soAndroidTime.setCreateTime(createTime);
    		                    // 监听者启动的时间往前推10分钟，是数据开始的时间
    		    		        long time = myTestStreamingListener.getWatchStartTime().getTime();
    		    		        long timeStar = time - (rate * 60 * 1000);
    		                    soAndroidTime.setStartTime(new Timestamp(timeStar));
    		                    soAndroidTime.setEndTime(new Timestamp(time));
    		                    soAndroidTime.setWariningID(warnningDataId2);
    		                    
    		                    sqlSession1.insert("insertToSoAndroidTime",soAndroidTime);
    		                }
    		                try {
    		                    sqlSession1.commit();
    		                } catch (Exception e) {
    		                    e.printStackTrace();
    		                }finally {
    		                    sqlSession1.close();
    		                }
    		                RedisUtils.closeJedis();
    	                }
    				}
                }catch(Exception e) {
                	e.printStackTrace();
                }
            }
        });
        
        //使用完释放缓存
        persist.foreachRDD(new VoidFunction<JavaPairRDD<String,String>>() {

			private static final long serialVersionUID = 3730252459143368671L;

			public void call(JavaPairRDD<String, String> rdd) throws Exception {
				rdd.unpersist();
			}
		});
        persist1.foreachRDD(new VoidFunction<JavaPairRDD<String,String>>() {

			private static final long serialVersionUID = 3730252459143368671L;

			public void call(JavaPairRDD<String, String> rdd) throws Exception {
				rdd.unpersist();
			}
		});
        persist2.foreachRDD(new VoidFunction<JavaPairRDD<String,String>>() {

			private static final long serialVersionUID = 3730252459143368671L;

			public void call(JavaPairRDD<String, String> rdd) throws Exception {
				rdd.unpersist();
			}
		});

        jsc.start();
        try {
			jsc.awaitTermination();
		} catch (InterruptedException e) {
		}
        jsc.stop();
    }
}
