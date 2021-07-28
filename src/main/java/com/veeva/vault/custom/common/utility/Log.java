package com.veeva.vault.custom.common.utility;

import java.util.List;
import java.util.Map;

import com.veeva.vault.sdk.api.core.LogService;
import com.veeva.vault.sdk.api.core.RequestContext;
import com.veeva.vault.sdk.api.core.RequestContextValueType;
import com.veeva.vault.sdk.api.core.RollbackException;
import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.UserDefinedClassInfo;
import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.json.JsonArrayBuilder;
import com.veeva.vault.sdk.api.json.JsonData;
import com.veeva.vault.sdk.api.json.JsonObject;
import com.veeva.vault.sdk.api.json.JsonObjectBuilder;
import com.veeva.vault.sdk.api.json.JsonService;
import com.veeva.vault.sdk.api.queue.MessageContext;

@UserDefinedClassInfo()
public class Log {
    public static final String ID = "id";
    public static final String OBJECT_API_NAME = "objectApiName";
    public static final String ASYNC_PROCESS = "asyncProcess";
    public static final String ID_LIST = "idList";
    public static final String MESS = "mess";

    private static final String VOBJECT_API_ENDPOINT_PREFIX = "/api/v20.1/vobjects/";
    private static final String ERROR_LOG_CUSTOM_SETTING_EXT_ID = "em";
    private static final String MESSAGE_FIELD = "messageField";

	public static void throwError(String errMsg) {
		RequestContext rc = RequestContext.get();
		String id = rc.getValue(ID, RequestContextValueType.STRING);
		String objectApiName = rc.getValue(OBJECT_API_NAME, RequestContextValueType.STRING);

		if (id != null && objectApiName != null) {
            Setting errorLogSettings = CustomSettings.getCS().get(ERROR_LOG_CUSTOM_SETTING_EXT_ID);
            String messageField = errorLogSettings.getChild(objectApiName).getChild(MESSAGE_FIELD).getValue();

            JsonService jsonService = ServiceLocator.locate(JsonService.class);
            JsonArrayBuilder jsonArrayBuilder = jsonService.newJsonArrayBuilder();
            JsonObjectBuilder jsonObjectBuilder = jsonService.newJsonObjectBuilder();
            jsonObjectBuilder.setValue(messageField, errMsg);
            jsonObjectBuilder.setValue(ID, id);
            jsonArrayBuilder.add(jsonObjectBuilder.build());

            Connection connection = new Connection();
            String apiUrl = VOBJECT_API_ENDPOINT_PREFIX + objectApiName;
            JsonData response = connection.putRequestMap(apiUrl, VaultCollections.newMap(), jsonArrayBuilder.build());
        }

		String ap = rc.getValue(ASYNC_PROCESS, RequestContextValueType.STRING);
		Log.debug("AP:" + ap);
		if (ap != null) {
			String ids = rc.getValue(ID_LIST, RequestContextValueType.STRING);
			createErrorRecord(ap, errMsg, ids);
		}
		throw new RollbackException("OPERATION_NOT_ALLOWED", errMsg);
	}
	
	public static void memory(String s) {
		LogService ls = ServiceLocator.locate(LogService.class);
		ls.logResourceUsage(s);
	}

	@SuppressWarnings("unchecked")
	public static void queueErrorRecord(String proc, String mess, String id) {
		Map<String,String> propMap = VaultCollections.newMap();
		List<String> messList = VaultCollections.asList(proc);
		propMap.put(ID, id);
		propMap.put(MESS, mess);
		Queue.queueMessage(messList, propMap, "error_queue__c");
	}
	
	@SuppressWarnings("unchecked")
	public static void createErrorRecord(String proc, String mess, String id) {
		Connection c = new Connection();
		JsonService js = ServiceLocator.locate(JsonService.class);
		JsonArrayBuilder jab = js.newJsonArrayBuilder();
		JsonObjectBuilder job = js.newJsonObjectBuilder();
		job.setValue("id_list__c", Queue.truncate(id,1500));
		job.setValue("process__c", Queue.truncate(proc,255));
		job.setValue("message__c", Queue.truncate(mess,1500));
		jab.add(job.build());
		Map<String,String>headerMap = VaultCollections.newMap();
		headerMap.put("Content-Type", "application/json");
		JsonObject resp = c.postRequestMap("/api/v20.2/vobjects/error__c",VaultCollections.newMap(),jab.build()).getJsonObject();
		Log.debug(resp.asString());
	}
	
	public static void debug(String s) {
		LogService ls = ServiceLocator.locate(LogService.class);
		ls.debug(s);
	}
	
	public static void setAsync(MessageContext mc) {
		RequestContext rc = RequestContext.get();
		rc.setValue(ASYNC_PROCESS, mc.getQueueName());
		rc.setValue(ID_LIST, String.join(",", mc.getMessage().getMessageItems()));
	}

    public static void setContext(String objectApiName, String id) {
        RequestContext rc = RequestContext.get();
        rc.setValue(OBJECT_API_NAME, objectApiName);
        rc.setValue(ID, id);
    }
}