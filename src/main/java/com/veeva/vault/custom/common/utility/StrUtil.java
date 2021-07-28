package com.veeva.vault.custom.common.utility;


import com.veeva.vault.sdk.api.core.StringUtils;
import com.veeva.vault.sdk.api.core.UserDefinedClassInfo;

@UserDefinedClassInfo()
public class StrUtil {
	
	public static String leadingZeros(int s, int total) {
		String ret = String.valueOf(s);
		while(ret.length() < total)
			ret = "0" + ret;
		return ret;
	}

	public static String strip(String s) {
		String ret = s;
		ret = StringUtils.replaceAll(ret, "\\s", "");
		ret = StringUtils.replaceAll(ret, "\\.", "");
		ret = StringUtils.replaceAll(ret, ",", "");
		ret = StringUtils.replaceAll(ret, "!", "");
		return ret;
	}
	
	public static String[] split(String in,String reg) {
		return StringUtils.split(in, reg);
	}
}