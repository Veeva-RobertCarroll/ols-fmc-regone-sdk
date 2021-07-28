package com.veeva.vault.custom.common.utility;

import java.util.List;
import java.util.Map;

import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.UserDefinedClassInfo;
import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.json.JsonArray;
import com.veeva.vault.sdk.api.json.JsonArrayBuilder;
import com.veeva.vault.sdk.api.json.JsonObject;
import com.veeva.vault.sdk.api.json.JsonObjectBuilder;
import com.veeva.vault.sdk.api.json.JsonService;
import com.veeva.vault.sdk.api.json.JsonValueType;

@UserDefinedClassInfo
public class EDL {
	@SuppressWarnings("unchecked")
	public static Map<String,String> getIdMap(List<String>idList){
		//RuntimeService rs = ServiceLocator.locate(RuntimeService.class);
		//rs.sleep(60000);
		Map<String,String> ret = VaultCollections.newMap();
		Connection c = new Connection();
		JsonService js = ServiceLocator.locate(JsonService.class);
		JsonArrayBuilder jab = js.newJsonArrayBuilder();
		for(String s : idList) {
			JsonObjectBuilder job = js.newJsonObjectBuilder();
			job.setValue("ref_id__v", s);
			jab.add(job.build());
		}
		JsonObject resp = c.postRequestMap("/api/v20.2/composites/trees/edl_hierarchy__v/actions/listnodes",VaultCollections.newMap(),jab.build()).getJsonObject();
		if(!resp.getValue("responseStatus", JsonValueType.STRING).equals("SUCCESS"))
			Log.throwError("Callout Failed for ListNodes");
		Log.debug(resp.asString());
		JsonArray ja = resp.getValue("data", JsonValueType.ARRAY);
		for(int i = 0; i < ja.getSize();i++) {
			JsonObject jo = ja.getValue(i, JsonValueType.OBJECT);
			if(jo.getValue("responseStatus",JsonValueType.STRING).equals("SUCCESS")) {
				ret.put(jo.getValue("ref_id__v",JsonValueType.STRING), jo.getValue("id",JsonValueType.STRING));
			}
		}
		return ret;
	}
}