package com.veeva.vault.custom.common.utility;

import java.util.Map;

import com.veeva.vault.sdk.api.core.RequestContext;
import com.veeva.vault.sdk.api.core.RequestContextValue;
import com.veeva.vault.sdk.api.core.UserDefinedClassInfo;
import com.veeva.vault.sdk.api.core.ValueType;
import com.veeva.vault.sdk.api.core.VaultCollections;

@UserDefinedClassInfo()
public class RecordTypeUtil implements RequestContextValue{
	
	private Map<String,Map<String,String>> rtuMap;
	
	@SuppressWarnings("unchecked")
	private RecordTypeUtil() {
		rtuMap = VaultCollections.newMap();
		String q = "SELECT id,api_name__v,object_name__v FROM object_type__v";
		Query.queryStream(q).forEach(qr -> {
			String id = qr.getValue("id", ValueType.STRING);
			String api = qr.getValue("api_name__v", ValueType.STRING);
			String obj = qr.getValue("object_name__v", ValueType.STRING);
			rtuMap.computeIfAbsent(obj, k -> VaultCollections.newMap()).put(api,id);
		});
	}
	
	private static RecordTypeUtil getRTU() {
		RequestContext rc = RequestContext.get();
		RecordTypeUtil ret = rc.getValue("RecordTypeUtil", RecordTypeUtil.class);
		if(ret == null) {
			ret = new RecordTypeUtil();
			rc.setValue("RecordTypeUtil", ret);
		}
		return ret;
	}
	
	public static String getRTId(String obj, String name) {
		return getRTU().rtuMap.get(obj).get(name);
	}
	
	public static String getRTName(String obj,String id) {
		Map<String,String> objToId = getRTU().rtuMap.get(obj);
		String ret = null;
		for(String name : objToId.keySet()) {
			String val = objToId.get(name);
			if(val.equals(id)) {
				ret = name;
				break;
			}
				
		}
		return ret;
	}
}