package com.veeva.vault.custom.common.utility;

import java.util.Map;
import java.util.Set;

import com.veeva.vault.sdk.api.core.UserDefinedClassInfo;
import com.veeva.vault.sdk.api.core.ValueType;
import com.veeva.vault.sdk.api.core.VaultCollections;

@UserDefinedClassInfo
public class ObjectMapping {
	
	private String obj,fld;
	private Map<String,String>mapping;
	private Set<String> idSet;
	
	@SuppressWarnings("unchecked")
	public ObjectMapping(String o,String f) {
		idSet = VaultCollections.newSet();
		obj = o;
		fld = f;
	}
	
	public void addId(String s) {
		idSet.add(s);
	}
	
	@SuppressWarnings("unchecked")
	public void runMapping() {
		mapping = VaultCollections.newMap();
		String q = "SELECT id," + fld + " FROM " + obj + " WHERE id CONTAINS('" +String.join("','", idSet)+ "')";
		Query.queryStream(q).forEach(qr -> {
			mapping.put(qr.getValue("id", ValueType.STRING), qr.getValue(fld, ValueType.STRING));
		});
	}
	
	public String getValue(String id) {
		return mapping.get(id);
	}
}