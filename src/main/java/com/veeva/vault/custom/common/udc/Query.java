package com.veeva.vault.custom.common.udc;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.veeva.vault.sdk.api.core.LogService;
import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.UserDefinedClassInfo;
import com.veeva.vault.sdk.api.core.ValueType;
import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.query.QueryResponse;
import com.veeva.vault.sdk.api.query.QueryResult;
import com.veeva.vault.sdk.api.query.QueryService;

@UserDefinedClassInfo()
public class Query {
	@SuppressWarnings("unchecked")
	public static List<QueryResult> queryList(String q){
		List<QueryResult> ret = VaultCollections.newList();
		Iterator<QueryResult> i = query(q);
		while(i.hasNext())ret.add(i.next());
		return ret;
	}
	
	public static Stream<QueryResult> queryStream(String q){
		Debug.debug("Running query:" + q);
		QueryService queryService = ServiceLocator.locate(QueryService.class);
        QueryResponse queryResponse = queryService.query(q);
        Debug.debug("Count:" + queryResponse.getResultCount());
        return queryResponse.streamResults();
	}
	
	public static Iterator<QueryResult> query(String q){
		debug("Running query:" + q);
        QueryService queryService = ServiceLocator.locate(QueryService.class);
        QueryResponse queryResponse = queryService.query(q);
        Iterator<QueryResult> i = queryResponse.streamResults().iterator();
        return i;
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
	
	public static void debug(String s) {
		LogService ls = ServiceLocator.locate(LogService.class);
		ls.debug(s);
	}
}