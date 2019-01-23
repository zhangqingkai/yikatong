package com.bmac.entity;

import java.util.Date;

/**
 * @Author: zqk
 * @Date: 2018/12/03 9:10
 * @Description: 卡号库存量中间表实体类
 */

public class SoAndroidCard {

    private String sellNo; //销售单位
    private String quotaCode; //指标编码
    private int status; //指标状态
    private String type; //指标类型
    private int operStatus; //操作状态
    private String openFactories; //手机品牌


    public SoAndroidCard() {}

    public SoAndroidCard(String sellNo, String quotaCode, int status, String type, Date createTime, String createUser, Date updateTime, String updateUser, int operStatus, String openFactories, String sortDesc, int cardCategory, String cardState) {
        this.sellNo = sellNo;
        this.quotaCode = quotaCode;
        this.status = status;
        this.type = type;
        this.operStatus = operStatus;
        this.openFactories = openFactories;
    }

    public String getSellNo() {
        return sellNo;
    }

    public void setSellNo(String sellNo) {
        this.sellNo = sellNo;
    }

    public String getQuotaCode() {
        return quotaCode;
    }

    public void setQuotaCode(String quotaCode) {
        this.quotaCode = quotaCode;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    public int getOperStatus() {
		return operStatus;
	}

	public void setOperStatus(int operStatus) {
		this.operStatus = operStatus;
	}

	public String getOpenFactories() {
        return openFactories;
    }

    public void setOpenFactories(String openFactories) {
        this.openFactories = openFactories;
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((openFactories == null) ? 0 : openFactories.hashCode());
		result = prime * result + operStatus;
		result = prime * result + ((quotaCode == null) ? 0 : quotaCode.hashCode());
		result = prime * result + ((sellNo == null) ? 0 : sellNo.hashCode());
		result = prime * result + status;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SoAndroidCard other = (SoAndroidCard) obj;
		if (openFactories == null) {
			if (other.openFactories != null)
				return false;
		} else if (!openFactories.equals(other.openFactories))
			return false;
		if (operStatus != other.operStatus)
			return false;
		if (quotaCode == null) {
			if (other.quotaCode != null)
				return false;
		} else if (!quotaCode.equals(other.quotaCode))
			return false;
		if (sellNo == null) {
			if (other.sellNo != null)
				return false;
		} else if (!sellNo.equals(other.sellNo))
			return false;
		if (status != other.status)
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}
}
