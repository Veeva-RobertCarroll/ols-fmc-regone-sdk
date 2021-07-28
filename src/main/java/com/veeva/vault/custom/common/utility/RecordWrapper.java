package com.veeva.vault.custom.common.utility;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.StringUtils;
import com.veeva.vault.sdk.api.core.UserDefinedClassInfo;
import com.veeva.vault.sdk.api.core.ValueType;
import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.data.Record;
import com.veeva.vault.sdk.api.data.RecordService;
import com.veeva.vault.sdk.api.query.QueryResult;

@UserDefinedClassInfo
public class RecordWrapper {
	
	private Setting recSet;
	private String id;
	private Record r;
	private Map<String,Map<String,RecordWrapper>> idToFldToRecordMap;
	
	public RecordWrapper(Record rIn, Setting objSet,Map<String,Map<String,RecordWrapper>>mapIn) {
		id = null;
		recSet = objSet;
		r = rIn;
		idToFldToRecordMap = mapIn;
	}
	
	@SuppressWarnings("unchecked")
	public String getWhereClause() {
		Set<String>ret = VaultCollections.newSet();
		Setting uSet = recSet.getChild("unique");
		if(uSet != null && uSet.getKids2().size() > 0)
			for(Setting fldSet : uSet.getKids2()) {
				ret.add(fldSet.getName() + " = '" + r.getValue(fldSet.getValue(), ValueType.STRING) +"'");
			}
		return String.join(" AND ", ret);
	}
	
	public boolean match(QueryResult qr) {
		boolean ret = true;
		Setting uSet = recSet.getChild("unique");
		for(Setting fldSet : uSet.getKids2()) {
			String rVal = r.getValue(fldSet.getValue(), ValueType.STRING);
			String qrVal = qr.getValue(fldSet.getName(), ValueType.STRING);
			if(!rVal.equals(qrVal)) {
				ret = false;
				break;
			}
		}
		if(ret)
			id = qr.getValue("id", ValueType.STRING);
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	public Record getNewRecord() {
		Record ret = null;
		if(id == null) {
			RecordService rs = ServiceLocator.locate(RecordService.class);
			ret = rs.newRecord(recSet.getName());
			Setting mapSet = recSet.getChild("mapping");
			if(mapSet == null)
				Log.throwError("mapping not setup properly for " + recSet.getName());
			for(Setting fldSet : mapSet.getKids2()) {
				String destFld = fldSet.getName();
				String sourceFld = fldSet.getValue();
				String val;
				if(destFld.equals("object_type__v")) {
					val = RecordTypeUtil.getRTId(recSet.getName(), sourceFld);
				}else if(sourceFld.contains("-")){
					List<String> valList = VaultCollections.newList();
					for(String s : StringUtils.split(sourceFld, "-")) {
						valList.add(r.getValue(s, ValueType.STRING));
					}
					val = String.join("-", valList);
				}else if(sourceFld.startsWith("::")){
					String fldName = sourceFld.substring(2,sourceFld.length());
					String id = r.getValue("id", ValueType.STRING);
					Map<String,RecordWrapper> rwMap = idToFldToRecordMap.get(id);
					RecordWrapper rw = rwMap.get(fldName);
					val = rw.getId();
				}else if(sourceFld.startsWith(":")){
					val = sourceFld.substring(1,sourceFld.length());
				}else {
					val = r.getValue(sourceFld, ValueType.STRING);
				}
				ret.setValue(destFld, val);
			}
		}
		return ret;
	}
	
	public void setId(String s) {
		id = s;
	}
	
	public String getId() {
		return id;
	}
}