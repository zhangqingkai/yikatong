package com.bmac.utils;



import java.io.Serializable;

import redis.clients.jedis.Jedis;


public  class RedisUtils implements Serializable{

	private static final long serialVersionUID = -3277434003872374647L;
	private static Jedis jedis;
	static {
		jedis = new Jedis("172.16.4.106",6379);
//		jedis = new Jedis("172.16.7.110",6379);
	}	
	
	//默认值
	private static int take_rate = 10;
	private static int defaultTimeOutNum = 3;
	private static int defaultExceptionNum = 5;
	private static int defaultTimeOutStatus = 0; //交易超时次数 操作状态默认值
	private static int defaultExceptionStatus = 0; //交易错误数  操作状态默认值
	private static String defaultTimeOutNumStartTime = "2019-01-01 00:00:00";//交易超时次数指标生效日期默认
	private static String defaultTimeOutNumEndTime = "2099-01-01 00:00:00";//交易超时次数指标失效日期默认
	private static String defaultTimeOutNumTakeTime = "00:00:00";//交易超时次数指标预警时间段(开始)
	private static String defaultTimeOutNumInvalidTime = "23:59:59";//交易超时次数指标预警时间段(结束)
	
	private static String defaultExceptionNumStartTime = "2019-01-01 00:00:00";//交易错误数指标生效日期默认
	private static String defaultExceptionNumEndTime = "2099-01-01 00:00:00";//交易错误数指标失效日期默认
	private static String defaultExceptionNumTakeTime = "00:00:00";//交易错误数指标预警时间段(开始)
	private static String defaultExceptionNumInvalidTime = "23:59:59";//交易错误数指标预警时间段(结束)
	
	// Android交易超时次数，预警频率，滑动窗口的长度K
	private static String key = "So_quota:quota_code:YJ002:take_rate";
	// 预警指标，android交易超时次数K
	private static String key2 = "So_quota:quota_code:YJ002:warning_present";
	//Android交易错误次数，预警频率，滑动窗口的长度K
	private static String key3 = "So_quota:quota_code:YJ005:take_rate";
	// Android交易错误次数
	private static String key4 = "So_quota:quota_code:YJ005:warning_present";
	//交易超时次数 人工解除状态标识
	private static String key5 = "So_quota:quota_code:YJ002:manual_opt";
	//交易错误次数 人工解除状态标识
	private static String key6 = "So_quota:quota_code:YJ005:manual_opt";
	//交易超时次数的指标生效日期
	private static String timeOutNumStartTimeKey = "So_quota:quota_code:YJ002:start_time";
	//交易超时次数的指标失效日期
	private static String timeOutNumEndTimeKey = "So_quota:quota_code:YJ002:end_time";
	//交易超时次数的指标预警时间段(开始)
	private static String timeOutNumTakeTimeKey = "So_quota:quota_code:YJ002:take_time";
	//交易超时次数的指标预警时间段(结束)
	private static String timeOutNumInvalidTimeKey = "So_quota:quota_code:YJ002:invalid_time"; 
	
	//交易错误数的指标生效日期
	private static String exceptionNumStartTimeKey = "So_quota:quota_code:YJ005:start_time";
	//交易错误数的指标失效日期
	private static String exceptionNumEndTimeKey = "So_quota:quota_code:YJ005:end_time";
	//交易错误数指标的预警时间段(开始)
	private static String exceptionNumTakeTimeKey = "So_quota:quota_code:YJ005:take_time";
	//交易错误数指标的预警时间段(结束)
	private static String exceptionNumInvalidTimeKey = "So_quota:quota_code:YJ005:invalid_time";
	//获取交易超时次数的指标生效日期
	public static String getTimeOutStartTime() {
		try {
			if(jedis.exists(timeOutNumStartTimeKey)) {
				defaultTimeOutNumStartTime = jedis.get(timeOutNumStartTimeKey);
			}
		}catch(Exception e) {
			return defaultTimeOutNumStartTime;
		}
		return defaultTimeOutNumStartTime;
	}
	//获取交易超时次数的指标失效日期
	public static String getTimeOutEndTime() {
		try {
			if(jedis.exists(timeOutNumEndTimeKey)) {
				defaultTimeOutNumEndTime = jedis.get(timeOutNumEndTimeKey);
			}
		}catch(Exception e) {
			return defaultTimeOutNumEndTime;
		}
		return defaultTimeOutNumEndTime;
	}
	//获取交易超时次数预警时间端	(开始)
	public static String getTimeOutTakeTime() {
		try {
			if(jedis.exists(timeOutNumTakeTimeKey)) {
				defaultTimeOutNumTakeTime = jedis.get(timeOutNumTakeTimeKey);
			}
		}catch(Exception e) {
			return defaultTimeOutNumTakeTime;
		}
		return defaultTimeOutNumTakeTime;
	}
	//获取交易超时次数预警时间段(结束)
	public static String getTimeOutInvalidTime() {
		try {
			if(jedis.exists(timeOutNumInvalidTimeKey)) {
				defaultTimeOutNumInvalidTime = jedis.get(timeOutNumInvalidTimeKey);
			}
		}catch(Exception e) {
			return defaultTimeOutNumInvalidTime;
		}
		return defaultTimeOutNumInvalidTime;
	}
	//获取Android交易超时次数窗口间隔，即预警频率
	public  static int getTakeRateTimeOut() {
		try{
			if(jedis.exists(key)){
				String str = jedis.get(key);
				Integer valueOf = Integer.valueOf(str);
				int a = valueOf;
				take_rate = a;
			}	
		} catch (Exception e) {
			return take_rate;
		}
		return take_rate;
	}	
	
	//获取交易超时次数  人工解除报警标识值
	public static int getTimeOutNumOperStatus() {
		try {
			if (jedis.exists(key5)) {
				String value = jedis.get(key5);
				defaultTimeOutStatus = Integer.valueOf(value);
			}
		}catch(Exception e) {
			return defaultTimeOutStatus;
		}
		return defaultTimeOutStatus;
	}
	//获取交易超时次数预警指标值
	public static int getwarningPresentTimeOutNum() {
		try {
			if(jedis.exists(key2)){
				String str = jedis.get(key2);
				Double valueOf = Double.valueOf(str);
				double d = valueOf;
				defaultTimeOutNum = (int)d;
			}
		}catch(Exception e) {
			return defaultTimeOutNum;
		}
		return defaultTimeOutNum;
	}	

	//获取交易错误次数的滑动窗口长度
	public static int getTakeRateException() {
		try {
			if(jedis.exists(key3)){
				String str = jedis.get(key3);
				Integer valueOf = Integer.valueOf(str);
				take_rate = valueOf;
			}
		}catch(Exception e) {
			return take_rate;
		}
		return take_rate;
	}

	//获取获取交易错误数的指标生效日期
	public static String getExceptionNumStartTime() {
		try {
			if(jedis.exists(exceptionNumStartTimeKey)) {
				defaultExceptionNumStartTime = jedis.get(exceptionNumStartTimeKey);
			}
		}catch(Exception e) {
			return defaultExceptionNumStartTime;
		}
		return defaultExceptionNumStartTime;
	}
	//获取获取交易错误数的指标失效日期
	public static String getExceptionNumEndTime() {
		try {
			if(jedis.exists(exceptionNumEndTimeKey)) {
				defaultExceptionNumEndTime = jedis.get(exceptionNumEndTimeKey);
			}
		}catch(Exception e) {
			return defaultExceptionNumEndTime;
		}
		return defaultExceptionNumEndTime;
	}
	//获取获取交易错误数预警时间端(开始)
	public static String getExceptionNumTakeTime() {
		try {
			if(jedis.exists(exceptionNumTakeTimeKey)) {
				defaultExceptionNumTakeTime = jedis.get(exceptionNumTakeTimeKey);
			}
		}catch(Exception e) {
			return defaultExceptionNumTakeTime;
		}
		return defaultExceptionNumTakeTime;
	}
	//获取获取交易错误数预警时间段(结束)
	public static String getExceptionNumInvalidTime() {
		try {
			if(jedis.exists(exceptionNumInvalidTimeKey)) {
				defaultExceptionNumInvalidTime = jedis.get(exceptionNumInvalidTimeKey);
			}
		}catch(Exception e) {
			return defaultExceptionNumInvalidTime;
		}
		return defaultExceptionNumInvalidTime;
	}
	//获取交易错误数预警指标值
	public static int getwarningPresentExceptionNum() {
		try {
			if(jedis.exists(key4)){
				String str = jedis.get(key4);
				Double valueOf = Double.valueOf(str);
				double d = valueOf;
				defaultExceptionNum = (int)d;
			}
		}catch(Exception e) {
			return defaultExceptionNum;
		}
		return defaultExceptionNum;
	}

	//获取交易错误数  人工解除报警标识值
	public static int getExceptionNumOperStatus() {
		try {
			if (jedis.exists(key6)) {
				String value = jedis.get(key6);
				defaultExceptionStatus = Integer.valueOf(value);
			}
		}catch(Exception e) {
			return defaultExceptionStatus;
		}
		return defaultExceptionStatus;
	}
	//关闭jedis
	public static void closeJedis() {
		try {
			jedis.close();
		}catch(Exception e) {
			jedis.close();
			e.printStackTrace();
		}
	}
}
