package com.bmac.dao.impl;

import org.apache.spark.streaming.scheduler.*;

import java.io.Serializable;
import java.sql.Timestamp;

public class MyTestStreamingListener implements StreamingListener,Serializable{

	private static final long serialVersionUID = 3363581762202227719L;
	private Timestamp watchStartTime;
	
	//外部调用该时间
	public Timestamp getWatchStartTime() {
		return watchStartTime;
	}
	
	public void onBatchCompleted(StreamingListenerBatchCompleted arg0) {
	}

	public void onBatchStarted(StreamingListenerBatchStarted arg0) {
		Timestamp dateStart = new Timestamp(System
				.currentTimeMillis());
		watchStartTime = dateStart;
	}

	public void onBatchSubmitted(StreamingListenerBatchSubmitted arg0) {
	}

	public void onOutputOperationCompleted(StreamingListenerOutputOperationCompleted arg0) {
	}

	public void onOutputOperationStarted(StreamingListenerOutputOperationStarted arg0) {
	}

	public void onReceiverError(StreamingListenerReceiverError arg0) {
	}

	public void onReceiverStarted(StreamingListenerReceiverStarted arg0) {
	}

	public void onReceiverStopped(StreamingListenerReceiverStopped arg0) {
	}

	public void onStreamingStarted(StreamingListenerStreamingStarted arg0) {
	}
}
