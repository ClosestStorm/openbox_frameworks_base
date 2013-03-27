package com.allwinner.netshare;


public class SmbFile extends Object
{
	private String mPath = null;
	private NtlmPasswordAuthentication mNtlm = new NtlmPasswordAuthentication(null, null, null);
	
	/* 这里定义了Samba类型 */
	public static final int  TYPE_WORKGROUP=1;
	public static final int  TYPE_SERVER=2;
	public static final int  TYPE_FILE_SHARE=3;
	public static final int  TYPE_PRINTER_SHARE=4;
	public static final int  TYPE_COMMS_SHARE=5;
	public static final int  TYPE_IPC_SHARE=6;
	public static final int  TYPE_DIR=7;
	public static final int  TYPE_FILE=8;
	public static final int  TYPE_LINK=9;
	public static final int  TYPE_UNKNOWN = 0;
	
	public static final int EACCES = 13;  //"Permission denied".
	public static final int EINVAL = 22;  //"A NULL file/URL was passed, or the URL would not parse,".
	public static final int ENOENT = 2;  //"Durl does not exist, or name is an"
	public static final int ENOTDIR = 20; //"Name is not a directory".
	public static final int EPERM  = 1;  //"The workgroup could not be found".
	public static final int ENODEV = 19;  //"The workgroup or server could not be found".
	public static final int ENOMEM = 12;  //"Insufficient memory to complete the operation". 
	public static final int EUNKNOWN = -1;
	public static final int SUCCESS = 0;
	
	public int type = TYPE_UNKNOWN;
	public int dirlen;
	public int commentlen;
	public String comment;
	public int namelen;  
	public String name; 

	
	public SmbFile(String url)
	{
		this(url, null);
	}
	
	public SmbFile(String url, NtlmPasswordAuthentication ntlm)
	{
		mPath = url;
		if(ntlm != null)
		{
			mNtlm = ntlm;
		}
		initAuthentication(mNtlm.mDomain, mNtlm.mUsername, mNtlm.mPassword);
	}
	
	public NtlmPasswordAuthentication getNtlm()
	{
		return mNtlm;
	}
	
	public void setNtlm(NtlmPasswordAuthentication ntlm)
	{
		if(ntlm != null)
		{
			mNtlm = ntlm;
		}
	}
	
	public void login(String username, String password)
	{
		mNtlm.mPassword = password;
		mNtlm.mUsername = username;
	}
	
	public String getPath()
	{
		return mPath;
	}
	
	private boolean checkPathAvailable()
	{
		if(mPath == null || mPath.isEmpty())
		{
			return false;
		}
		return true;
	}
	
	public boolean canExecute()
	{
		if(!checkPathAvailable())
		{
			return false;
		}
		return canExecuteImpl(mPath);
	}

	public boolean canRead()
	{
		if(!checkPathAvailable())
		{
			return false;
		}
		return canReadImpl(mPath);
	}
	
	public boolean canWrite()
	{
		if(!checkPathAvailable())
		{
			return false;
		}
		return canWriteImpl(mPath);
	}

	public boolean exists()
	{
		if(!checkPathAvailable())
		{
			return false;
		}
		return existsImpl(mPath);
	}
	
	public SmbFile[] list()
	{
		initAuthentication(mNtlm.mDomain, mNtlm.mUsername, mNtlm.mPassword);
		SmbFile[] smbFile = listImpl(mPath);
		if(smbFile == null)
			return null;
		/* 设置每个SmbFile的用户信息 */
		NtlmPasswordAuthentication npa;
		if(this.getType() == TYPE_WORKGROUP)
		{
			npa = new NtlmPasswordAuthentication(this.mPath, this.mNtlm.mUsername, this.mNtlm.mPassword);
		}
		else
		{
			npa = new NtlmPasswordAuthentication(this.mNtlm.mDomain, this.mNtlm.mUsername, this.mNtlm.mPassword);
		}
		
		for(int i = 0; i < smbFile.length; i++)
		{
			if(mPath.equals("smb://") || this.getType() == TYPE_WORKGROUP)
			{
				smbFile[i].mPath = "smb://" + smbFile[i].name;
			}
			else
			{
				smbFile[i].mPath = this.getPath() + "/" + smbFile[i].name;
			}
			smbFile[i].setNtlm(npa);
		}
		
		return smbFile;
	}
	
	
	public int list(final SmbReceiver rc)
	{
		if(this.mPath == null)
			return -1;
		initAuthentication(mNtlm.mDomain, mNtlm.mUsername, mNtlm.mPassword);
		OnReceiverListenner ls = new OnReceiverListenner() {
			@Override
			public void onReceiver(SmbFile smbFile) {
				/* 设置每个SmbFile的用户信息 */
				NtlmPasswordAuthentication npa;
				if(SmbFile.this.getType() == TYPE_WORKGROUP)
				{
					npa = new NtlmPasswordAuthentication(SmbFile.this.mPath, SmbFile.this.mNtlm.mUsername, SmbFile.this.mNtlm.mPassword);
				}
				else
				{
					npa = new NtlmPasswordAuthentication(SmbFile.this.mNtlm.mDomain, SmbFile.this.mNtlm.mUsername, SmbFile.this.mNtlm.mPassword);
				}
				if(mPath.equals("smb://") || SmbFile.this.getType() == TYPE_WORKGROUP)
				{
					smbFile.mPath = "smb://" + smbFile.name;
				}
				else
				{
					smbFile.mPath = SmbFile.this.getPath() + "/" + smbFile.name;
				}
				smbFile.setNtlm(npa);
				rc.accept(smbFile);
			}
		};
		int ret = listCallbackImpl(this.mPath, ls);
		return ret;
	}
	
	public int getType()
	{
		if(this.type < 0)
		{
			return this.getTypeImpl(mPath);
		}
		else
			return this.type;
	}
	
	public interface SmbReceiver
	{
		void accept(SmbFile smbFile);
	}
	
	static
	{
		System.loadLibrary("smbjni");
	}
	
	private static native void initAuthentication(String domain, String username, String password);
	
	private static native boolean canExecuteImpl(String path);
	
	private static native boolean canReadImpl(String path);
	
	private static native boolean canWriteImpl(String path);
	
	private static native boolean existsImpl(String path);
	
	private static native SmbFile[] listImpl(String path);
	
	private static native int listCallbackImpl(String path, OnReceiverListenner ls);

	private native int getTypeImpl(String smbUrl);

    private static native boolean createNewFileImpl(String path);
    private static native boolean deleteImpl(String path);
    private static native long getFreeSpaceImpl(String path);
    private static native long getTotalSpaceImpl(String path);
    private static native long getUsableSpaceImpl(String path);
    private static native boolean isDirectoryImpl(String path);
    private static native boolean isFileImpl(String path);
    private static native long lastModifiedImpl(String path);
    private static native long lengthImpl(String path);
    private static native boolean mkdirImpl(String path);
    private static native String readlink(String path);
    private static native boolean renameToImpl(String oldPath, String newPath);
    private static native boolean setExecutableImpl(String path, boolean set, boolean ownerOnly);
    private static native boolean setLastModifiedImpl(String path, long ms);
    private static native boolean setReadableImpl(String path, boolean set, boolean ownerOnly);
    private static native boolean setWritableImpl(String path, boolean set, boolean ownerOnly);
	public static native int mount(String source, String target, String username, String password);
	public static native int umount(String mountpoint);
}
