package com.veeva.vault.custom.common.udc;

import com.veeva.vault.sdk.api.core.LogService;
import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.UserDefinedClassInfo;

@UserDefinedClassInfo(name="vps_debug__c")
public class Debug {
	
	public static void debug(String s) {
		LogService ls = ServiceLocator.locate(LogService.class);
		ls.debug(s);
	}

}