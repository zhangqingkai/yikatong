//package com.bmac.sparktask;
//
//import com.alibaba.fastjson.JSONObject;
//import com.bmac.conf.ConfigurationManager;
//import com.bmac.constant.Constants;
//import com.bmac.dao.impl.MyTestStreamingListener;
//import com.bmac.entity.SoAndroidTime;
//import com.bmac.entity.SoEarlyWarning;
//import com.bmac.utils.DingTalkUtils;
//import com.bmac.utils.MyBatisUtils;
//import com.bmac.utils.RedisUtils;
//
//import kafka.serializer.StringDecoder;
//
//import org.apache.ibatis.session.SqlSession;
//import org.apache.spark.SparkConf;
//import org.apache.spark.api.java.JavaPairRDD;
//import org.apache.spark.api.java.function.*;
//import org.apache.spark.storage.StorageLevel;
//import org.apache.spark.streaming.Durations;
//import org.apache.spark.streaming.api.java.JavaPairDStream;
//import org.apache.spark.streaming.api.java.JavaPairInputDStream;
//import org.apache.spark.streaming.api.java.JavaStreamingContext;
//import org.apache.spark.streaming.kafka.KafkaUtils;
//
//import scala.Tuple2;
//import java.io.Serializable;
//import java.sql.Date;
//import java.sql.Timestamp;
//import java.text.SimpleDateFormat;
//import java.util.*;
//
///**
// * @Author: zqk
// * @Date: 2018/11/28 15:57
// * @Description: 交易超时次数
// */
//
//public class TenMinuteTimeOutThreeTimesMonitor implements Serializable{
//
//    private static final long serialVersionUID = 2694860879942103447L;
//    private String group = ConfigurationManager.getProperty(Constants.TIMEOUT_GROUP_ID);
//	private static  int rate;//频率 10分钟
//    private static  int errorTimes;//10分钟内3次超时(标准条件)
//
//    final com.bmac.dao.impl.MyTestStreamingListener myTestStreamingListener = new MyTestStreamingListener();
//    // 分别是so_early_warning里的id要和so_android_time的warnning_id关联的id。
//    int warnningDataId,warnningQuota = 0;
//    int flagStatus = 2; //解除标识
//    int flagOperStatus; //人工解除预警标识
//
//    public void tenMinuteTimeOutThreeTimesMonitor(){
//
//        SparkConf conf = new SparkConf()
//                .setAppName(Constants.TIMEOUT_APP_NAME)
//                .setMaster("local[2]");
//
//
//        JavaStreamingContext jsc = new JavaStreamingContext(conf, Durations.seconds(10));
//
//        //时间监听
//        jsc.addStreamingListener(myTestStreamingListener);
//        //构建请求kafka的参数
//        Map<String, String> kafkaParams = new HashMap<String, String>();
//        kafkaParams.put(Constants.KAFKA_BROKER_LIST, ConfigurationManager.getProperty(Constants.KAFKA_META_BROKER_LIST));
//        kafkaParams.put("group.id", group); //设置消费者组id
//        kafkaParams.put("enable.auto.commit", "true"); //使用kafka自动提交模式，由应用程序自己接管offset
//
//        System.out.println(kafkaParams);
//        // 构建topics
//        String kafkaTopics = ConfigurationManager.getProperty(Constants.KAFKA_TOPICS);
//        String[] kafkaTopicSplited = kafkaTopics.split(",");
//
//        // 存储topics
//        Set<String> topics = new HashSet<String>();
//        for (String topic : kafkaTopicSplited) {
//            topics.add(topic);
//        }
//
//        //以Direct的方式读取
//        JavaPairInputDStream<String, String> realTimeDStream = KafkaUtils.createDirectStream(
//        		jsc,
//        		String.class,
//                String.class,
//                StringDecoder.class,
//                StringDecoder.class,
//                kafkaParams,
//                topics
//                );
//
//        rate = RedisUtils.getTakeRateTimeOut(); //10分钟
//        
//        //设置窗口函数
//        JavaPairDStream<String, String> realTimeDstreamWindow = realTimeDStream.window(
//                Durations.minutes(Long.valueOf(rate)),Durations.seconds(Constants.SPARK_DURATION_SECONDS));
//        
//        //需要先按时间对数据进行过滤(防止OGG宕机,然后推过来历史数据)
//        JavaPairDStream<String, String> realTimeDstreamWindowFilterd = realTimeDstreamWindow.filter(new Function<Tuple2<String,String>, Boolean>() {
//			private static final long serialVersionUID = 1L;
//			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//			public Boolean call(Tuple2<String, String> tuple2) throws Exception {
//		        // 监听者启动的时间往前推rate分钟，是数据开始的时间
//		        try {
//		        	long time = myTestStreamingListener.getWatchStartTime().getTime();
//			        long timeStar = time - (10 * 60 * 1000);
//					JSONObject jsonObject = JSONObject.parseObject(tuple2._2);
//					String errorTime = jsonObject.getString("ErrorTime");
//					long errorTime1 = format.parse(errorTime.substring(0, 19)).getTime();
//					return errorTime1 >= timeStar;
//		        } catch(Exception e) {
//		        	return false;
//		        }
//			}
//		});
//                
//        //生成(response_code, 所有字段的数据)
//        JavaPairDStream<String, String> lines = realTimeDstreamWindowFilterd.flatMapToPair(new PairFlatMapFunction<Tuple2<String,String>, String, String>() {
//			private static final long serialVersionUID = 1L;
//			@SuppressWarnings("unchecked")
//			public Iterator<Tuple2<String, String>> call(Tuple2<String, String> tuple2) throws Exception {
//				try {
//					JSONObject jsonObject = JSONObject.parseObject(tuple2._2);
//	                String responseCode = jsonObject.getString("ResponseCode"); 
//	                return Arrays.asList(new Tuple2<String, String>(responseCode, tuple2._2)).iterator();
//				} catch(Exception e) {
//					return null;
//				}
//			}
//		});
//
//        
//        //报警数据先进行过滤，剩下的都是有问题连接异常的数据
//        JavaPairDStream<String, String> filtered = lines.filter(new Function<Tuple2<String, String>, Boolean>() {
//            private static final long serialVersionUID = -3302292144646922821L;
//
//			public Boolean call(Tuple2<String, String> tuple2) throws Exception {
//                String s = tuple2._1;
//                return s.equals("0198") || s.equals("0199") || s.equals("0298")
//                        || s.equals("0299") || s.equals("4998") || s.equals("4999");
//            }
//        });
//
//        //将过滤后的数据缓存起来，后面两次用到
//        JavaPairDStream<String, String> persist = filtered.persist(StorageLevel.MEMORY_AND_DISK());
//
//        //接下来分两步走
//        //第一步走so_early_warning 库
//        //剩下的所有数据都是连接超时的数据了，生成("timeOut", 1)，进行聚合
//        JavaPairDStream<String, Integer> timeOutTuple = persist.mapToPair(new PairFunction<Tuple2<String, String>, String, Integer>() {
//            private static final long serialVersionUID = -4162335378298019871L;
//
//			public Tuple2<String, Integer> call(Tuple2<String, String> tuple2) throws Exception {
//
//                return new Tuple2<String, Integer>("timeOut", 1);
//            }
//        });
//
//        //进行聚合
//        JavaPairDStream<String, Integer> timeOutSumed = timeOutTuple.reduceByKey(new Function2<Integer, Integer, Integer>() {
//            private static final long serialVersionUID = -5824057116803137099L;
//
//            public Integer call(Integer v1, Integer v2) throws Exception {
//                return v1 + v2;
//            }
//        });
//
//        //思路：将正常的数据生成(响应码， 所有字段)类型， 先根据响应码进行过滤，剩下的数据
//        //都是有问题的数据，将剩下的数据分两步走，一部分生成("timeOut", 1)类型，调用reduceByKey
//        //再调用window算子，走so_early_warning库，另一部分取所有字段即明细数据，走so_android_time库
//        timeOutSumed.foreachRDD(new VoidFunction<JavaPairRDD<String, Integer>>() {
//            private static final long serialVersionUID = 8515083313165243140L;
//
//            public void call(JavaPairRDD<String, Integer> rdd) throws Exception {
//                List<Tuple2<String, Integer>> collect = rdd.collect();
//                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                int timeOutNum = 0;
//                //从redis中拿取操作状态值，如果是人工解除预警      也不用发送钉钉
//                flagOperStatus = RedisUtils.getTimeOutNumOperStatus();
//                for (Tuple2<String, Integer> tuple2 : collect) {
//                     timeOutNum = tuple2._2;
//                     SoEarlyWarning soEarlyWarning = new SoEarlyWarning();
//                     //报警时间
//                     Date errorTime = new Date(System.currentTimeMillis());
//                     //从redis中拿取值(标准值比较值)
//                     errorTimes = RedisUtils.getwarningPresentTimeOutNum(); //3次
//                     if (timeOutNum >= Long.valueOf(errorTimes)) {
//                        //soEarlyWarning
//
//                        warnningQuota = timeOutNum;
//                        
//                        soEarlyWarning.setQuotaCode("YJ002");
//                        soEarlyWarning.setQuotaPresent(timeOutNum);
//                        soEarlyWarning.setWarningPresent(errorTimes);
//						soEarlyWarning.setStatus(0);
//						flagStatus = 0;
//						
//                        soEarlyWarning.setType("34");//android指标
//                        soEarlyWarning.setCreateTime(errorTime);
//                        soEarlyWarning.setCreateUser("TimeOutNumsMonitor");
//                        soEarlyWarning.setUpdateTime(errorTime);
//                        soEarlyWarning.setUpdateUser("TimeOutNumsMonitor");
//						soEarlyWarning.setOperStatus(0); //预警
//
//                        SqlSession sqlSession = MyBatisUtils.getSqlSession();
//                        sqlSession.insert("insertToSoEarlyWarning", soEarlyWarning);
//                        try {
//                            sqlSession.commit();
//                        } catch (Exception e) {
//                        	sqlSession.rollback();
//                            e.printStackTrace();
//                        }finally {
//                            sqlSession.close();
//                        }
//
//                        warnningDataId = soEarlyWarning.getId();
//                     }else if(timeOutNum > 0 && timeOutNum < Long.valueOf(errorTimes)){
//                        //soEarlyWarning
//                        warnningQuota = timeOutNum;
//                        
//                        soEarlyWarning.setQuotaCode("YJ002");
//                        soEarlyWarning.setQuotaPresent(timeOutNum);
//                        soEarlyWarning.setWarningPresent(errorTimes);
//                        if (flagStatus == 0) {
//                            soEarlyWarning.setStatus(1); //系统自动解除
//                        } else {
//                            soEarlyWarning.setStatus(2);
//                        }
//                        soEarlyWarning.setType("34");//android指标
//                        soEarlyWarning.setCreateTime(errorTime);
//                        soEarlyWarning.setCreateUser("TimeOutNumsMonitor");
//                        soEarlyWarning.setUpdateTime(errorTime);
//                        soEarlyWarning.setUpdateUser("TimeOutNumsMonitor");
//                        if (flagStatus == 0){
//                            soEarlyWarning.setOperStatus(1); //系统自动解除
//                            flagStatus = 1;
//                        }else {
//                            soEarlyWarning.setOperStatus(3); //正常
//                            flagStatus = 2;
//                        }
//
//                        SqlSession sqlSession = MyBatisUtils.getSqlSession();
//                        sqlSession.insert("insertToSoEarlyWarning", soEarlyWarning);
//                        try {
//                            sqlSession.commit();
//                        } catch (Exception e) {
//                        	sqlSession.rollback();
//                            e.printStackTrace();
//                        }finally {
//                            sqlSession.close();
//                        }
//                        warnningDataId = soEarlyWarning.getId();
//                    }
//
//                    //数据插入数据库后，按条件进行预警
////                    if (flagOperStatus == 2) { //是从redis里拿的数据
////                        //donothing 不发预警了
////                    } else if(flagOperStatus == 0) {
////                        if (flagStatus == 0 && timeOutNum >= Long.valueOf(errorTimes)) { //预警
////                            String message = "{'msgtype':'text','text':{'content':"
////                                    + "'服务器:安卓TSM平台"
////                                    + "\\r\\n发生了:交易超时次数报警" 
////                                    + "\\r\\n故障级别:报警/P2"
////                                    + "\\r\\n故障状态:PROBLEM"
////                                    + "\\r\\n内容为:当前" + rate +"分钟内,交易错误次数为" + timeOutNum + "次;"
////                                    + "当前报警指标值为" + errorTimes + "次;" + "创建报警时间为" + simpleDateFormat.format(errorTime)
////                                    + "\\r\\n事件ID:" + warnningDataId +"'}}";
////                            DingTalkUtils.earlyWarning(message);
////                        } else if (flagStatus == 1 && timeOutNum > 0 && timeOutNum < Long.valueOf(errorTimes)) { //系统自动解除预警
////                            String message = "{'msgtype':'text','text':{'content':"
////                                    + "'服务器:安卓TSM平台"
////                                    + "\\r\\n发生了:交易超时次数报警自动解除" 
////                                    + "\\r\\n故障级别:解除/P2"
////                                    + "\\r\\n故障状态:OK"
////                                    + "\\r\\n内容为:当前" + rate +"分钟内,交易错误次数为" + timeOutNum + "次;"
////                                    + "当前报警指标值为" + errorTimes + "次;" + "预警解除时间为" + simpleDateFormat.format(errorTime)
////                                    + "\\r\\n\\r\\n事件ID:" + warnningDataId +"'}}";
////                            DingTalkUtils.earlyWarning(message);
////                        }
////                    }
//                }
//            }
//        });
//
//        //第二步 明细数据走so_android_time库
//        persist.foreachRDD(new VoidFunction<JavaPairRDD<String, String>>() {
//            private static final long serialVersionUID = -1912870696655562553L;
//
//            public void call(JavaPairRDD<String, String> rdd) throws Exception {
//                List<Tuple2<String, String>> collect = rdd.collect();
//
//                SqlSession sqlSession1 = MyBatisUtils.getSqlSession();
//                System.out.println("明细数据");
//                for (Tuple2<String, String> tuple2 : collect) {
//                    System.out.println(tuple2);
//                    JSONObject jsonObject = JSONObject.parseObject(tuple2._2);
//                    Date createTime = new Date(System.currentTimeMillis());
//
//                    //正常情况下 用jsonObject.get()得到它的明细数据，给它封装到对象中
//                    SoAndroidTime soAndroidTime = new SoAndroidTime();
//                    // 正常情况
//
//                    soAndroidTime.setSellNo(jsonObject.getString("SellNo"));
//                    soAndroidTime.setCardCode(jsonObject.getString("CardCode"));
//                    soAndroidTime.setUserMobile(jsonObject.getString("UserMobile"));
//                    soAndroidTime.setDeviceID(jsonObject.getString("DeviceId"));
//                    soAndroidTime.setResponseCode(tuple2._1);
//                    soAndroidTime.setMessage(jsonObject.getString("Message"));
//                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                    
//                    soAndroidTime.setErrorTime(sdf.parse((jsonObject.getString("ErrorTime")).substring(0, 19)));
//                    soAndroidTime.setQuotaPresent(warnningQuota);
//                    soAndroidTime.setCreateTime(createTime);
//                    // 监听者启动的时间往前推rate分钟，是数据开始的时间
//    		        long time = myTestStreamingListener.getWatchStartTime().getTime();
//    		        long timeStar = time - (rate * 60 * 1000);
//                    soAndroidTime.setStartTime(new Timestamp(timeStar));
//                    soAndroidTime.setEndTime(new Timestamp(time));
//                    soAndroidTime.setWariningID(warnningDataId);
//                    
//                    sqlSession1.insert("insertToSoAndroidTime",soAndroidTime);
//                }
//                try {
//                    sqlSession1.commit();
//                } catch (Exception e) {
//                	sqlSession1.rollback();
//                    e.printStackTrace();
//                }finally {
//                    sqlSession1.close();
//                }
//            }
//        });
//
//        //使用完释放缓存
//        persist.foreachRDD(new VoidFunction<JavaPairRDD<String,String>>() {
//
//			private static final long serialVersionUID = 6439271566241771629L;
//
//			public void call(JavaPairRDD<String, String> rdd) throws Exception {
//				rdd.unpersist();
//			}
//		});
//        
//        jsc.start();
//        try {
//			jsc.awaitTermination();
//		} catch (InterruptedException e) {
//		}
//        jsc.close();
//    }
//}
