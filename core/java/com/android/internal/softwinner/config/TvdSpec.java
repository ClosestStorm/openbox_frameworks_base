package com.android.internal.softwinner.config;

/**
 *
 * {@hide}
 */
public abstract class TvdSpec implements ProductSpec
{
	public boolean haveEthernet()
	{
		return false;
	}
	
	public boolean haveWifi()
	{
		return true;
	}	
		
	public boolean haveTelephony()
	{
		return false;
	}
	
	public boolean havewinmax()
	{
		return false;
	}
	public boolean haveGps()
	{
		return false;
	}

	public boolean haveBluetooth()
	{
		return false;
	}
	
    /* add by Gary. start {{----------------------------------- */
    /* 2012-01-29 */
    /* add a new function 'getProductName()' in ProducSpec and define some produects' names */	
    public String getProductName(){
        return "tvd";
    }
    /* add by Gary. end   -----------------------------------}} */
}
