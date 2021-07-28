package com.veeva.vault.custom.common.utility;

import java.util.List;
import java.util.Map;

import com.veeva.vault.custom.common.uds.CSService;
import com.veeva.vault.sdk.api.core.RequestContext;
import com.veeva.vault.sdk.api.core.RequestContextValue;
import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.StringUtils;
import com.veeva.vault.sdk.api.core.UserDefinedClassInfo;
import com.veeva.vault.sdk.api.core.ValueType;
import com.veeva.vault.sdk.api.data.Record;
import com.veeva.vault.sdk.api.query.QueryResult;

@UserDefinedClassInfo
public class CustomSettings  implements RequestContextValue{
	
	private Map<String,Setting> csMap;
	
	@SuppressWarnings("unchecked")
	private CustomSettings() {
		CSService cServ = ServiceLocator.locate(CSService.class);
		csMap = cServ.getCSMap();
		/**
		csMap = VaultCollections.newMap();
		
		
		String q = "select id,name__v,value__c,parent__cr.external_id__c,external_id__c from custom_settings__c WHERE state__v = 'active_state__c'";
        Iterator<QueryResult> i = Query.query(q);
        
        Map<String,Set<String>> childMap = VaultCollections.newMap(); 
        while(i.hasNext()) {
        	QueryResult qr = i.next();
        	String extId = qr.getValue("external_id__c", ValueType.STRING);
        	csMap.put(extId, new Setting(qr));
        	String parent = qr.getValue("parent__cr.external_id__c", ValueType.STRING);
        	if(parent != null && !parent.equals("")) {
        		if(!childMap.containsKey(parent)) childMap.put(parent, VaultCollections.newSet());
        		childMap.get(parent).add(extId);
        	}
        }
        
        for(String parentExt : childMap.keySet()) {
        	Setting parent = csMap.get(parentExt);
        	for(String kidExt : childMap.get(parentExt)) {
        		parent.addChild(csMap.get(kidExt));
        	}
        }
        **/
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
		Setting extSet = cs.get(ext);
		if(extSet == null)
			Log.throwError("Custom Setting not found:" + ext);
		return cs.get(ext).getChild(obj);
	}
	
	public Setting get(String ext) {
		return csMap.get(ext);
	}
	
	public static boolean meetsCriteria(Getter r, Setting crit) {
		boolean ret = false;
		String fldName = crit.getName();
		String[] fldArr = StringUtils.split(fldName, ":");
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
				if(!meetsCriteria(new Getter(r), crit))ret = false;
			}
		}
		return ret;
	}
	
	public static boolean checkCriteria(Setting set, Record r) {
		boolean ret = true;
		Setting critSetting = set.getChild("criteria");
		if(critSetting != null) {
			for(Setting crit : critSetting.getKids2()) {
				if(!meetsCriteria(new Getter(r), crit))ret = false;
			}
		}
		return ret;
	}
}