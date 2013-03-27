package com.android.internal.allwinner.config;

import android.os.Build;
import android.util.Log;

/**
 *
 * {@hide}
 */

public class ProductConfig
{
	private static final String 	 TAG  = "ProductConfig";
	private static ProductSpec spec = null;

    /* add by Gary. start {{----------------------------------- */
    /* 2012-01-29 */
    /* add a new function 'getProductName()' in ProducSpec and define some produects' names */	
    public static String PRODUCT_NAME_CRANE       = "crane";
    public static String PRODUCT_NAME_APOLLO      = "apollo";
    public static String PRODUCT_NAME_APOLLO_MELE = "apollo_mele";
    /* add by Gary. end   -----------------------------------}} */

    /* add by Gary. start {{----------------------------------- */
    /* 2012-05-31 */
    /* add a new attibute "chip_type" */	
    public static String CHIP_TYPE         = "ro.chip_type";
    public static String CHIP_TYPE_A10     = "a10";
    public static String CHIP_TYPE_A10S    = "a10s";
    public static String CHIP_TYPE_DEFAULT = CHIP_TYPE_A10;
    /* add by Gary. end   -----------------------------------}} */

	public static ProductSpec getProductSpec()
	{
		String device = Build.DEVICE;
		if( spec == null )
		{
			if( device.contains("crane") )
			{
				spec = new CraneSpec();
				//Log.i(TAG, device+" have framework config class");
			}else if(device.contains("apollo") ){
				if(device.contains("apollo-mele"))
    				spec = new ApolloMeleSpec();
    		    else				    
				    spec = new ApolloSpec();
				//Log.i(TAG, device+" have framework config class");
			}else{
				Log.e(TAG, device+" not have framework config class");			
                spec = new ApolloSpec();
			}					
		}

		return spec;
	}	
}
