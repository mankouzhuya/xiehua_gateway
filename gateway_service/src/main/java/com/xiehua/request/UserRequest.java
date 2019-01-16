package com.xiehua.request;

import java.io.Serializable;

public class UserRequest implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String accept;
	
	private String acceptEncoding;
	
	private String acceptLanguage;
	
	private String cacheControl;
	
	private String connection;
	
	private String cookie;
	
	private String host;
	
	private String upgradeInsecureRequests;
	
	private String userAgent;
	
	private String referer;
	
	

	public UserRequest() {
		// TODO Auto-generated constructor stub
	}

	public String getAccept() {
		return accept;
	}

	public void setAccept(String accept) {
		this.accept = accept;
	}

	public String getAcceptEncoding() {
		return acceptEncoding;
	}

	public void setAcceptEncoding(String acceptEncoding) {
		this.acceptEncoding = acceptEncoding;
	}

	public String getAcceptLanguage() {
		return acceptLanguage;
	}

	public void setAcceptLanguage(String acceptLanguage) {
		this.acceptLanguage = acceptLanguage;
	}

	public String getCacheControl() {
		return cacheControl;
	}

	public void setCacheControl(String cacheControl) {
		this.cacheControl = cacheControl;
	}

	public String getConnection() {
		return connection;
	}

	public void setConnection(String connection) {
		this.connection = connection;
	}

	public String getCookie() {
		return cookie;
	}

	public void setCookie(String cookie) {
		this.cookie = cookie;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getUpgradeInsecureRequests() {
		return upgradeInsecureRequests;
	}

	public void setUpgradeInsecureRequests(String upgradeInsecureRequests) {
		this.upgradeInsecureRequests = upgradeInsecureRequests;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public String getReferer() {
		return referer;
	}

	public void setReferer(String referer) {
		this.referer = referer;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		UserRequest that = (UserRequest) o;

		if (accept != null ? !accept.equals(that.accept) : that.accept != null) return false;
		if (acceptEncoding != null ? !acceptEncoding.equals(that.acceptEncoding) : that.acceptEncoding != null)
			return false;
		if (acceptLanguage != null ? !acceptLanguage.equals(that.acceptLanguage) : that.acceptLanguage != null)
			return false;
		if (cacheControl != null ? !cacheControl.equals(that.cacheControl) : that.cacheControl != null) return false;
		if (connection != null ? !connection.equals(that.connection) : that.connection != null) return false;
		if (cookie != null ? !cookie.equals(that.cookie) : that.cookie != null) return false;
		if (host != null ? !host.equals(that.host) : that.host != null) return false;
		if (upgradeInsecureRequests != null ? !upgradeInsecureRequests.equals(that.upgradeInsecureRequests) : that.upgradeInsecureRequests != null)
			return false;
		if (userAgent != null ? !userAgent.equals(that.userAgent) : that.userAgent != null) return false;
		return referer != null ? referer.equals(that.referer) : that.referer == null;
	}

	@Override
	public int hashCode() {
		int result = accept != null ? accept.hashCode() : 0;
		result = 31 * result + (acceptEncoding != null ? acceptEncoding.hashCode() : 0);
		result = 31 * result + (acceptLanguage != null ? acceptLanguage.hashCode() : 0);
		result = 31 * result + (cacheControl != null ? cacheControl.hashCode() : 0);
		result = 31 * result + (connection != null ? connection.hashCode() : 0);
		result = 31 * result + (cookie != null ? cookie.hashCode() : 0);
		result = 31 * result + (host != null ? host.hashCode() : 0);
		result = 31 * result + (upgradeInsecureRequests != null ? upgradeInsecureRequests.hashCode() : 0);
		result = 31 * result + (userAgent != null ? userAgent.hashCode() : 0);
		result = 31 * result + (referer != null ? referer.hashCode() : 0);
		return result;
	}
}