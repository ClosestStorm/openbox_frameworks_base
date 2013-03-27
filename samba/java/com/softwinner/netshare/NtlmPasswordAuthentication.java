package com.softwinner.netshare;

public class NtlmPasswordAuthentication
{
	public String mDomain = null;
	public String mUsername = null;
	public String mPassword = null;
	public NtlmPasswordAuthentication(String domain, String username, String password)
	{
		mDomain = domain;
		mUsername = username;
		mPassword = password;
	}
}