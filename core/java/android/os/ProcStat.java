package android.os;

public class ProcStat {

	private long utime;
	private long cutime;
	private long stime;
	private long cstime;
	private String state;
	
	public long getUtime() {
		return utime;
	}
	public void setUtime(long utime) {
		this.utime = utime;
	}
	public long getCutime() {
		return cutime;
	}
	public void setCutime(long cutime) {
		this.cutime = cutime;
	}
	public long getStime() {
		return stime;
	}
	public void setStime(long stime) {
		this.stime = stime;
	}
	public long getCstime() {
		return cstime;
	}
	public void setCstime(long cstime) {
		this.cstime = cstime;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	
	
}

