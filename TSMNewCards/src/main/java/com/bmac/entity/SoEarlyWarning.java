package com.bmac.entity;

import java.io.Serializable;
import java.util.Date;

public class SoEarlyWarning implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private int id;
	private String quotaCode;//指标编码
	private double quotaPresent;//当前指标
	private double warningPresent; //报警指标值
	private int status; //指标状态(0 报警 1 解除 2 正常)
	private String type; //指标类型
	private Date createTime;//创建时间
	private String createUser; //创建人
	private Date updateTime; //更新时间
	private String updateUser; //更新人
	private int operStatus;//操作状态(0 报警未解除 1系统自动解除 2人工解除报警 3 系统数据正常)

	public SoEarlyWarning() {
		super();
	}

	public SoEarlyWarning(int id, String quotaCode, double quotaPresent, double warningPresent, int status, String type, Date createTime, String createUser, Date updateTime, String updateUser, int operStatus) {
		this.id = id;
		this.quotaCode = quotaCode;
		this.quotaPresent = quotaPresent;
		this.warningPresent = warningPresent;
		this.status = status;
		this.type = type;
		this.createTime = createTime;
		this.createUser = createUser;
		this.updateTime = updateTime;
		this.updateUser = updateUser;
		this.operStatus = operStatus;
	}

	
	
	public double getWarningPresent() {
		return warningPresent;
	}

	public void setWarningPresent(double warningPresent) {
		this.warningPresent = warningPresent;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getQuotaCode() {
		return quotaCode;
	}

	public void setQuotaCode(String quotaCode) {
		this.quotaCode = quotaCode;
	}

	public double getQuotaPresent() {
		return quotaPresent;
	}

	public void setQuotaPresent(double quotaPresent) {
		this.quotaPresent = quotaPresent;
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

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public String getCreateUser() {
		return createUser;
	}

	public void setCreateUser(String createUser) {
		this.createUser = createUser;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public String getUpdateUser() {
		return updateUser;
	}

	public void setUpdateUser(String updateUser) {
		this.updateUser = updateUser;
	}

	public int getOperStatus() {
		return operStatus;
	}

	public void setOperStatus(int operStatus) {
		this.operStatus = operStatus;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((createTime == null) ? 0 : createTime.hashCode());
		result = prime * result + ((createUser == null) ? 0 : createUser.hashCode());
		result = prime * result + id;
		result = prime * result + operStatus;
		result = prime * result + ((quotaCode == null) ? 0 : quotaCode.hashCode());
		long temp;
		temp = Double.doubleToLongBits(quotaPresent);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + status;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((updateTime == null) ? 0 : updateTime.hashCode());
		result = prime * result + ((updateUser == null) ? 0 : updateUser.hashCode());
		temp = Double.doubleToLongBits(warningPresent);
		result = prime * result + (int) (temp ^ (temp >>> 32));
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
		SoEarlyWarning other = (SoEarlyWarning) obj;
		if (createTime == null) {
			if (other.createTime != null)
				return false;
		} else if (!createTime.equals(other.createTime))
			return false;
		if (createUser == null) {
			if (other.createUser != null)
				return false;
		} else if (!createUser.equals(other.createUser))
			return false;
		if (id != other.id)
			return false;
		if (operStatus != other.operStatus)
			return false;
		if (quotaCode == null) {
			if (other.quotaCode != null)
				return false;
		} else if (!quotaCode.equals(other.quotaCode))
			return false;
		if (Double.doubleToLongBits(quotaPresent) != Double.doubleToLongBits(other.quotaPresent))
			return false;
		if (status != other.status)
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		if (updateTime == null) {
			if (other.updateTime != null)
				return false;
		} else if (!updateTime.equals(other.updateTime))
			return false;
		if (updateUser == null) {
			if (other.updateUser != null)
				return false;
		} else if (!updateUser.equals(other.updateUser))
			return false;
		if (Double.doubleToLongBits(warningPresent) != Double.doubleToLongBits(other.warningPresent))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SoEarlyWarning [id=" + id + ", quotaCode=" + quotaCode + ", quotaPresent=" + quotaPresent
				+ ", warningPresent=" + warningPresent + ", status=" + status + ", type=" + type + ", createTime="
				+ createTime + ", createUser=" + createUser + ", updateTime=" + updateTime + ", updateUser="
				+ updateUser + ", operStatus=" + operStatus + "]";
	}
}
