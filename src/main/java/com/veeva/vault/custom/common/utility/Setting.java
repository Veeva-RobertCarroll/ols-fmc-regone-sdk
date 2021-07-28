package com.veeva.vault.custom.common.utility;

import java.util.Set;

import com.veeva.vault.sdk.api.core.RequestContextValue;
import com.veeva.vault.sdk.api.core.UserDefinedClassInfo;
import com.veeva.vault.sdk.api.core.ValueType;
import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.query.QueryResult;

@UserDefinedClassInfo()
public class Setting implements RequestContextValue {
	
	private String name,value;
	private Set<Setting> kids;
	private Setting parent;
	private String extId;
	
	public Setting() {}
	
	@SuppressWarnings("unchecked")
	public Setting(QueryResult qr) {
		kids = VaultCollections.newSet();
		name = qr.getValue("name__v", ValueType.STRING);
		value = qr.getValue("value__c", ValueType.STRING);
		extId = qr.getValue("external_id__c", ValueType.STRING);
	}
	
	public String getExtId() {
		return extId;
	}
	
	public void addChild(Setting cs) {
		kids.add(cs);
		cs.setParent(this);
	}
	
	public void setParent(Setting cs) {
		parent = cs;
	}
	
	public String getName() {
		return name;
	}
	
	public String getValue() {
		return value;
	}
	
	public String getValue(String s) {
		String ret = "";
		Setting kid = getChild(s);
		if(kid != null) ret = kid.getValue();
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	public Set<String> getValueSet(){
		Set<String> ret = VaultCollections.newSet();
		for(Setting kid : getKids2())
			ret.add(kid.value);
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	public Set<String> getNameSet(){
		Set<String> ret = VaultCollections.newSet();
		for(Setting kid : getKids2())
			ret.add(kid.name);
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	public Set<String> getValueSet(String s) {
		Set<String> ret = VaultCollections.newSet();
		Setting kid = getChild(s);
		if(kid != null) ret.add(kid.getValue());
		return ret;
	}
	
	public Setting getChild(String s) {
		for(Setting cs : kids)
			if(cs.getName().equals(s))return cs;
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public Set<Setting> getChildren(String s) {
		Set<Setting> ret = VaultCollections.newSet();
		for(Setting cs : kids)
			if(cs.getName().equals(s))ret.add(cs);
		return ret;
	}
	
	public boolean hasChild(String s) {
		return getChild(s) != null;
	}
	
	public Setting getParent() {
		return parent;
	}
	
	@SuppressWarnings("unchecked")
	public Set<Setting> getKids2(){
		Set<Setting> ret = VaultCollections.newSet();
		ret.addAll(kids);
		return ret;
	}
}