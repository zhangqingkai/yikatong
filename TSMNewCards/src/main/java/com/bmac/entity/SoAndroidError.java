package com.bmac.entity;

import java.io.Serializable;
import java.util.Date;

/**
 * @Author: zqk
 * @Date: 2018/12/01 12:27
 * @Description:
 */

public class SoAndroidError implements Serializable{

    private static final long serialVersionUID = 7862548211982193803L;
	private int id;
    private String sellNo; //销售单位
    private String cardCode; //卡号
    private String userMobile; //手机号
    private String deviceID; //设备标识符
    private String responseCode; //应答码
    private String message; //错误详情
    private Date errorTime; //发生时间
    private double quotaPresent; //当前指标
    private Date createTime; //时间
    private Date startTime; //监控开始时间
    private Date endTime; //监控结束时间
    private long wariningID; //报警id

    public SoAndroidError(){}

    public SoAndroidError(int id, String sellNo, String cardCode, String userMobile, String deviceID, String responseCode, String message, Date errorTime, double quotaPresent, Date createTime, Date startTime, Date endTime, long wariningID) {
        this.id = id;
        this.sellNo = sellNo;
        this.cardCode = cardCode;
        this.userMobile = userMobile;
        this.deviceID = deviceID;
        this.responseCode = responseCode;
        this.message = message;
        this.errorTime = errorTime;
        this.quotaPresent = quotaPresent;
        this.createTime = createTime;
        this.startTime = startTime;
        this.endTime = endTime;
        this.wariningID = wariningID;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSellNo() {
        return sellNo;
    }

    public void setSellNo(String sellNo) {
        this.sellNo = sellNo;
    }

    public String getCardCode() {
        return cardCode;
    }

    public void setCardCode(String cardCode) {
        this.cardCode = cardCode;
    }

    public String getUserMobile() {
        return userMobile;
    }

    public void setUserMobile(String userMobile) {
        this.userMobile = userMobile;
    }

    public String getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getErrorTime() {
        return errorTime;
    }

    public void setErrorTime(Date errorTime) {
        this.errorTime = errorTime;
    }

    public double getQuotaPresent() {
        return quotaPresent;
    }

    public void setQuotaPresent(double quotaPresent) {
        this.quotaPresent = quotaPresent;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public long getWariningID() {
        return wariningID;
    }

    public void setWariningID(long wariningID) {
        this.wariningID = wariningID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SoAndroidError that = (SoAndroidError) o;

        if (id != that.id) return false;
        if (Double.compare(that.quotaPresent, quotaPresent) != 0) return false;
        if (wariningID != that.wariningID) return false;
        if (sellNo != null ? !sellNo.equals(that.sellNo) : that.sellNo != null) return false;
        if (cardCode != null ? !cardCode.equals(that.cardCode) : that.cardCode != null) return false;
        if (userMobile != null ? !userMobile.equals(that.userMobile) : that.userMobile != null) return false;
        if (deviceID != null ? !deviceID.equals(that.deviceID) : that.deviceID != null) return false;
        if (responseCode != null ? !responseCode.equals(that.responseCode) : that.responseCode != null) return false;
        if (message != null ? !message.equals(that.message) : that.message != null) return false;
        if (errorTime != null ? !errorTime.equals(that.errorTime) : that.errorTime != null) return false;
        if (createTime != null ? !createTime.equals(that.createTime) : that.createTime != null) return false;
        if (startTime != null ? !startTime.equals(that.startTime) : that.startTime != null) return false;
        return endTime != null ? endTime.equals(that.endTime) : that.endTime == null;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = id;
        result = 31 * result + (sellNo != null ? sellNo.hashCode() : 0);
        result = 31 * result + (cardCode != null ? cardCode.hashCode() : 0);
        result = 31 * result + (userMobile != null ? userMobile.hashCode() : 0);
        result = 31 * result + (deviceID != null ? deviceID.hashCode() : 0);
        result = 31 * result + (responseCode != null ? responseCode.hashCode() : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (errorTime != null ? errorTime.hashCode() : 0);
        temp = Double.doubleToLongBits(quotaPresent);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (createTime != null ? createTime.hashCode() : 0);
        result = 31 * result + (startTime != null ? startTime.hashCode() : 0);
        result = 31 * result + (endTime != null ? endTime.hashCode() : 0);
        result = 31 * result + (int) (wariningID ^ (wariningID >>> 32));
        return result;
    }
}
