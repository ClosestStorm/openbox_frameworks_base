package com.android.internal.allwinner.config;

/**
 * 
 * {@hide}
 */
public interface ProductSpec
{
	/* Network config spec */
	public boolean haveEthernet();
	public boolean haveWifi();
	public boolean haveTelephony();
	public boolean havewinmax();
	public boolean haveGps();
	public boolean haveBluetooth();
    /* add by Gary. start {{----------------------------------- */
    /* 2012-01-29 */
    /* add a new function 'getProductName()' in ProducSpec and define some produects' names */	
    public String getProductName();
    /* add by Gary. end   -----------------------------------}} */
}

