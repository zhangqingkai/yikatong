package com.bmac.utils;

import java.io.Serializable;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class DingTalkUtils implements Serializable {

	private static final long serialVersionUID = -6262381932288498295L;

	// 发送数据的正式网址
	public static String WEBHOOK_TOKEN = "https://oapi.dingtalk.com/robot/send?access_token=e6762d85ce3663e99150ff7aaa45a77769bcf2af09fba5d4a39b2ec0edf16ba3";
	// 发送数据的测试网址
//	public static String WEBHOOK_TOKEN = "https://oapi.dingtalk.com/robot/send?access_token=7f33a9cb633898af60a60bb483550b9593152f2d6d3b946b2578bec9ca2962cd";
	public static void earlyWarning(String message) {

		// 创建Http客户端对象
		HttpClient httpclient = HttpClients.createDefault();

		// 访问地址
		HttpPost httppost = new HttpPost(WEBHOOK_TOKEN);
		// 添加请求头，固定模式
		httppost.addHeader("Content-Type", "application/json; charset=utf-8");


		// 发送消息
		// 每个机器人每分钟最多发送20条。
		StringEntity se = new StringEntity(message, "utf-8");
		httppost.setEntity(se);

		/*
		 * 获取发送是否成功的信息
		 */
		HttpResponse response;
		try {
			response = httpclient.execute(httppost);
			// 返回的状态码是200，说明发送成功；获取返回信息；HttpStatus.SC_OK是为200
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				// 获取返回结果
				EntityUtils.toString(response.getEntity(),
						"utf-8");
			}
		} catch (Exception e) {
			System.err.println("建立响应对象失败");
		}

	}

}
