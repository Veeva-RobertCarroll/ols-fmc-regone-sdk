package com.veeva.vault.custom.common.utility;

import com.veeva.vault.sdk.api.core.UserDefinedClassInfo;
import com.veeva.vault.sdk.api.json.JsonData;

@UserDefinedClassInfo()
public class Response{
	
	public JsonData resp; 
	
	public Response() {}
	
	public JsonData getResp() {return resp;}
	
	public void setResp(JsonData obj) {
		resp = obj;
	}
	
}