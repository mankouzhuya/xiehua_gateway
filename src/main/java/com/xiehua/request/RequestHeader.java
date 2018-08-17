package com.xiehua.request;

import java.io.Serializable;

public class RequestHeader implements Serializable{

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
	
	

	public RequestHeader() {
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

		RequestHeader that = (RequestHeader) o;

		if (acceptEncoding != null ? !acceptEncoding.equals(that.acceptEncoding) : that.acceptEncoding != null)
			return false;
		return acceptLanguage != null ? acceptLanguage.equals(that.acceptLanguage) : that.acceptLanguage == null;
	}

	@Override
	public int hashCode() {
		int result = acceptEncoding != null ? acceptEncoding.hashCode() : 0;
		result = 31 * result + (acceptLanguage != null ? acceptLanguage.hashCode() : 0);
		return result;
	}
}