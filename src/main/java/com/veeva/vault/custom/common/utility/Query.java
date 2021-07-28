package com.veeva.vault.custom.common.utility;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.UserDefinedClassInfo;
import com.veeva.vault.sdk.api.core.ValueType;
import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.query.QueryResponse;
import com.veeva.vault.sdk.api.query.QueryResult;
import com.veeva.vault.sdk.api.query.QueryService;

@UserDefinedClassInfo()
public class Query {
	public static Stream<QueryResult> queryStream(String q){
		Log.debug("Running query:" + q);
		QueryService queryService = ServiceLocator.locate(QueryService.class);
        QueryResponse queryResponse = queryService.query(q);
        Log.debug("Count:" + queryResponse.getResultCount());
        return queryResponse.streamResults();
	}
	
	public static Iterator<QueryResult> query(String q){
		return queryStream(q).iterator();
	}
	
	@SuppressWarnings("unchecked")
	public static Map<String,QueryResult> queryMap(String q){
		Map<String,QueryResult> idMap = VaultCollections.newMap();
		Query.queryStream(q).forEach(qr -> {
			String id = qr.getValue("id", ValueType.STRING);
			idMap.put(id, qr);
		});
		return idMap;
	}
	
	public static String escape(String s) {
		QueryService qs = ServiceLocator.locate(QueryService.class);
		return qs.escape(s);
	}
	
	@SuppressWarnings("unchecked")
	public static Map<String,QueryResult> getRecordMap(String obj, Set<String> fldSet, Set<String> idSet){
		Map<String,QueryResult> ret = VaultCollections.newMap();
		Set<String> curFieldSet = VaultCollections.newSet();
		for(String s : fldSet)curFieldSet.add(s);
		curFieldSet.add("id");
		String q = "SELECT " + String.join(",", curFieldSet) + " FROM " + obj;
		if(idSet != null && idSet.size() > 0) {
			q += " WHERE id CONTAINS ('" + String.join("','", idSet) + "')";
		}
		
		queryStream(q).forEach(qr -> {
			ret.put(qr.getValue("id", ValueType.STRING), qr);
		});
		return ret;
	}
}