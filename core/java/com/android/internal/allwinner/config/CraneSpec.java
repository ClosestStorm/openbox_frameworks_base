package com.android.internal.allwinner.config;

/**
 *
 * {@hide}
 */
public class CraneSpec extends PadSpec
{
    /* add by Gary. start {{----------------------------------- */
    /* 2012-01-29 */
    /* add a new function 'getProductName()' in ProducSpec and define some produects' names */
    @Override	
    public String getProductName(){
        return ProductConfig.PRODUCT_NAME_CRANE;
    }
    /* add by Gary. end   -----------------------------------}} */
} 
