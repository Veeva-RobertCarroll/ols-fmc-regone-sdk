package com.veeva.vault.custom.common.udc;

import java.util.Set;

import com.veeva.vault.sdk.api.core.RequestContextValue;
import com.veeva.vault.sdk.api.core.UserDefinedClassInfo;
import com.veeva.vault.sdk.api.core.ValueType;
import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.query.QueryResult;

@UserDefinedClassInfo()
public class Setting implements RequestContextValue {
	
	private String name,value;
	private MySettingSet kids;
	private Setting parent;
	
	public Setting() {}
	
	public Setting(QueryResult qr) {
		kids = new MySettingSet();
		name = qr.getValue("name__v", ValueType.STRING);
		value = qr.getValue("value__c", ValueType.STRING);
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
	
	public MySettingSet getKids2(){
		MySettingSet ret = new MySettingSet();
		ret.addAll(kids);
		return ret;
	}
}