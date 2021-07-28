package com.veeva.vault.custom.common.utility;

import java.util.Map;

import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.UserDefinedClassInfo;
import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.http.HttpMethod;
import com.veeva.vault.sdk.api.http.HttpRequest;
import com.veeva.vault.sdk.api.http.HttpResponseBodyValueType;
import com.veeva.vault.sdk.api.http.HttpService;
import com.veeva.vault.sdk.api.json.JsonArray;
import com.veeva.vault.sdk.api.json.JsonData;

@UserDefinedClassInfo
public class Connection {
	
	private String connName;
	private HttpService hs;
	
	public Connection() {connName = null;hs = ServiceLocator.locate(HttpService.class);}
	public Connection(String n) {this();connName = n;}
	
	@SuppressWarnings("unchecked")
	public JsonData getRequest(String path) {
		return getRequest(path, VaultCollections.newMap());
	}
	
	public JsonData getRequest(String path,Map<String,String> headerMap) {
		HttpRequest req = getRequestObj(path,headerMap);
		req.setMethod(HttpMethod.GET);
		return getResponse(req);
	}

	public JsonData postRequestMap(String path,Map<String,String> headerMap,Map<String,String> bodyMap) {
		HttpRequest req = getRequestObj(path,headerMap);
		req.setMethod(HttpMethod.POST);
		for(String k : bodyMap.keySet()) {
			Log.debug("Setting body paramter: " + k + " to " + bodyMap.get(k));
			req.setBodyParam(k, bodyMap.get(k));
		}
		return getResponse(req);
	}
	
	public JsonData postRequestMap(String path,Map<String,String> headerMap,JsonArray ja) {
		HttpRequest req = getRequestObj(path,headerMap);
		req.setMethod(HttpMethod.POST);
		Log.debug(ja.asString());
		req.setBody(ja);
		return getResponse(req);
	}

    public JsonData postRequestMap(String path,Map<String,String> headerMap,JsonArray ja, Map<String, String> queryParams) {
        HttpRequest req = getRequestObj(path,headerMap);
        req.setMethod(HttpMethod.POST);
        Log.debug(ja.asString());
        req.setBody(ja);
        for(String name : queryParams.keySet()) {
            req.setQuerystringParam(name, queryParams.get(name));
        }
        return getResponse(req);
    }
	
	public JsonData putRequestMap(String path,Map<String,String> headerMap,JsonArray ja) {
		HttpRequest req = getRequestObj(path,headerMap);
		req.setMethod(HttpMethod.PUT);
		req.setBody(ja);
		return getResponse(req);
	}
	
	private HttpRequest getRequestObj(String path,Map<String,String> headerMap) {
		HttpRequest ret;
		if(connName == null) {
			ret = hs.newLocalHttpRequest();
		}else {
			ret = hs.newHttpRequest(connName);
		}
		ret.appendPath(path);
		for(String k : headerMap.keySet()) {
			Log.debug("Setting header parameter: " + k + " to " + headerMap.get(k));
			ret.setHeader(k,headerMap.get(k));
		}
		return ret;
	}
	
	private JsonData getResponse(HttpRequest req) {
		Response r = new Response();
		hs.send(req, HttpResponseBodyValueType.JSONDATA)
		.onSuccess(httpResponse -> {
			JsonData response = httpResponse.getResponseBody();
			Log.debug("executionId:"+httpResponse.getHeaderValues("x-vaultapi-executionid"));
			if(response.isValidJson()) {
				r.setResp(response);
			}else {
				Log.debug(httpResponse.toString());
				Log.throwError("Improperly formatted JSON returned");
			}
		})
		.onError(err ->{
			Log.debug(err.getHttpResponse().getResponseBody());
			Log.throwError("Error running callout:" + err.getMessage());
		}).execute();
		return r.getResp();
	}
}