package com.veeva.vault.custom.common.udc;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.veeva.vault.sdk.api.core.RequestContext;
import com.veeva.vault.sdk.api.core.RequestContextValue;
import com.veeva.vault.sdk.api.core.UserDefinedClassInfo;
import com.veeva.vault.sdk.api.core.ValueType;
import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.data.Record;
import com.veeva.vault.sdk.api.query.QueryResult;

@UserDefinedClassInfo(name="vps_custom_settings__c")
public class CustomSettings  implements RequestContextValue{
	
	private Map<String,Setting> csMap;
	
	private CustomSettings() {
		csMap = VaultCollections.newMap();
		
		
        String q = "select id,name__v,value__c,parent__cr.external_id__c,external_id__c from custom_settings__c WHERE state__v = 'active_state__c'";
        Iterator<QueryResult> i = Utility.query(q);
        
        Map<String,MyStringSet> childMap = VaultCollections.newMap(); 
        while(i.hasNext()) {
        	QueryResult qr = i.next();
        	String extId = qr.getValue("external_id__c", ValueType.STRING);
        	csMap.put(extId, new Setting(qr));
        	String parent = qr.getValue("parent__cr.external_id__c", ValueType.STRING);
        	if(parent != null && !parent.equals("")) {
        		if(!childMap.containsKey(parent)) childMap.put(parent, new MyStringSet());
        		childMap.get(parent).add(extId);
        	}
        }
        
        for(String parentExt : childMap.keySet()) {
        	Setting parent = csMap.get(parentExt);
        	for(String kidExt : childMap.get(parentExt)) {
        		parent.addChild(csMap.get(kidExt));
        	}
        }
	}
	
	public static CustomSettings getCS() {
		RequestContext rc = RequestContext.get();
		CustomSettings ret = rc.getValue("customSettings", CustomSettings.class);
		if(ret == null) {
			ret = new CustomSettings();
			rc.setValue("customSettings", ret);
		}
		//CustomSettings ret = new CustomSettings();
		return ret;
	}
	
	public static String getMessage(String mess) {
		CustomSettings cs = getCS();
		String ret = cs.get("mess").getValue(mess);
		return ret;
	}
	
	public static Setting getCS(String ext,String obj) {
		CustomSettings cs = getCS();
		return cs.get(ext).getChild(obj);
	}
	
	public Setting get(String ext) {
		return csMap.get(ext);
	}
	
	public static boolean meetsCriteria(Record r, Setting crit) {
		boolean ret = false;
		String fldName = crit.getName();
		String[] fldArr = Utility.split(fldName, ":");
		if(fldArr.length == 1) {
			String val1 = r.getValue(crit.getName(), ValueType.STRING);
			String val2 = crit.getValue();
			if((val1 == null && val2 == null) || (val1 != null && val1.equals(val2)))ret = true;
		}else if(fldArr.length == 2 && fldArr[1].equals("pl")){
			List<String> strList = r.getValue(fldArr[0], ValueType.PICKLIST_VALUES);
			if(strList != null) {
				for(String s : strList) {
					if(s.equals(crit.getValue())) ret = true;
				}
			}
		}
		return ret;
	}
	
	public static boolean meetsCriteria(QueryResult r, Setting crit) {
		boolean ret = false;
		String fldName = crit.getName();
		String[] fldArr = Utility.split(fldName, ":");
		if(fldArr.length == 1) {
			String val1 = r.getValue(crit.getName(), ValueType.STRING);
			String val2 = crit.getValue();
			if((val1 == null && val2 == null) || (val1 != null && val1.equals(val2)))ret = true;
		}else if(fldArr.length == 2 && fldArr[1].equals("pl")){
			List<String> strList = r.getValue(fldArr[0], ValueType.PICKLIST_VALUES);
			if(strList != null) {
				for(String s : strList) {
					if(s.equals(crit.getValue())) ret = true;
				}
			}
		}
		return ret;
	}
	
	public static boolean checkCriteria(Setting set, QueryResult r) {
		boolean ret = true;
		Setting critSetting = set.getChild("criteria");
		if(critSetting != null) {
			for(Setting crit : critSetting.getKids2()) {
				if(!meetsCriteria(r, crit))ret = false;
			}
		}
		return ret;
	}
	
	public static boolean checkCriteria(Setting set, Record r) {
		boolean ret = true;
		Setting critSetting = set.getChild("criteria");
		if(critSetting != null) {
			for(Setting crit : critSetting.getKids2()) {
				if(!meetsCriteria(r, crit))ret = false;
			}
		}
		return ret;
	}
}