package com.veeva.vault.custom.common.utility;

import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.UserDefinedClassInfo;
import com.veeva.vault.sdk.api.core.ValueType;
import com.veeva.vault.sdk.api.http.HttpMethod;
import com.veeva.vault.sdk.api.http.HttpRequest;
import com.veeva.vault.sdk.api.http.HttpResponseBodyValueType;
import com.veeva.vault.sdk.api.http.HttpService;
import com.veeva.vault.sdk.api.json.JsonArray;
import com.veeva.vault.sdk.api.json.JsonData;
import com.veeva.vault.sdk.api.json.JsonValueType;
import com.veeva.vault.sdk.api.query.QueryResult;

@UserDefinedClassInfo()
public class RemoteVault {
	
	private String connection;
	private HttpService httpService;
	
	public RemoteVault(String conn) {
		connection = conn;
		httpService = ServiceLocator.locate(HttpService.class);
	}
	
	public static RemoteVault getRemoteVaultFromId(String id) {
		String q = "SELECT api_name__sys FROM connection__sys WHERE remote_vault_id__sys = '"+id+"'";
		QueryResult first = Query.queryStream(q).findFirst().orElse(null);
		if(first == null)
			Log.throwError("Connection for Vault not found:" + id);
		return new RemoteVault(first.getValue("api_name__sys", ValueType.STRING));
	}
	
	private HttpRequest getRequest(HttpMethod meth, String url) {
		HttpRequest request = httpService.newHttpRequest(connection);
		request.setMethod(meth);
		request.appendPath(url);
		request.setHeader("Content-Type", "application/x-www-form-urlencoded");
		return request;
	}

	public JsonArray query(String q) {
		HttpRequest req = getRequest(HttpMethod.POST,"/api/v19.1/query");
		req.setBodyParam("q", q);
		ReturnValue ret = new ReturnValue();
		httpService.send(req, HttpResponseBodyValueType.JSONDATA)
		.onSuccess(httpResponse -> {
			JsonData response = httpResponse.getResponseBody();
			if(response.isValidJson()) {
				String responseStatus = response.getJsonObject().getValue("responseStatus", JsonValueType.STRING);
				if(responseStatus.equals("SUCCESS")) {
					ret.addReturn(response);
				}else {
					Log.throwError("Query Failed:" + q);
				}
			}else {
				Log.debug(httpResponse.toString());
				Log.throwError("Improperly formatted JSON returned");
			}
		})
		.onError(err ->{
			Log.throwError("Error running query:" + q + "----" + err.getMessage());
		}).execute();
		JsonData jd = ret.getRet();
		JsonArray ja = jd.getJsonObject().getValue("data", JsonValueType.ARRAY);
		return ja;
	}
	
	public class ReturnValue{
		private JsonData ret = null;
		public ReturnValue() {}
		public void addReturn(JsonData r) {ret = r;}
		public JsonData getRet() {return ret;}
	}
}