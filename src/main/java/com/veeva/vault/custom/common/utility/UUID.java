package com.veeva.vault.custom.common.utility;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.veeva.vault.sdk.api.core.StringUtils;
import com.veeva.vault.sdk.api.core.UserDefinedClassInfo;
import com.veeva.vault.sdk.api.core.ValueType;
import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.data.Record;
import com.veeva.vault.sdk.api.data.RecordChange;
import com.veeva.vault.sdk.api.data.RecordTriggerContext;
import com.veeva.vault.sdk.api.query.QueryResult;

@UserDefinedClassInfo
public class UUID {
	
	@SuppressWarnings("unchecked")
	public static void fillInId(List<Record> recList, String obj) {
		Setting uuid = CustomSettings.getCS("uuid", obj);
		String uuidFld = uuid.getValue();
		Map<String,Record> uuidToRecord = VaultCollections.newMap();
		for(Record r : recList) {
			String uuidVal = r.getValue(uuidFld, ValueType.STRING);
			uuidToRecord.put(uuidVal, r);
		}
		
		String q = "SELECT id," + uuidFld + " FROM " + obj + " WHERE "+uuidFld+" CONTAINS('" +String.join("','", uuidToRecord.keySet())+ "')";
		Query.queryStream(q).forEach(qr -> {
			String id = qr.getValue("id", ValueType.STRING);
			String uuidVal = qr.getValue(uuidFld, ValueType.STRING);
			Record r = uuidToRecord.get(uuidVal);
			r.setValue("id", id);
		});
	}
	
	@SuppressWarnings("unchecked")
	public static void assignUUID(List<Record> recList, String obj) {
		Setting uuid = CustomSettings.getCS("uuid", obj);
		
		Map<String,BigDecimal> otMap = VaultCollections.newMap();
		Map<String,ObjectMapping> mappingMap = VaultCollections.newMap();
		for(Record r : recList) {
			String ot = r.getValue("object_type__v", ValueType.STRING);
			String otName = RecordTypeUtil.getRTName(obj, ot);
			if(uuid.hasChild(otName)) {
				Setting otSet = uuid.getChild(otName);
				if(otSet.getValue().contains("#")) {
					otMap.put(otName, BigDecimal.valueOf(0));
				}else {
					for(Setting objSet : uuid.getChild(otName).getKids2()) {
						String[] valArr = StringUtils.split(objSet.getValue(), ":");
						if(valArr.length != 2)
							Log.throwError("Improperly configured object field in UUID custom settings " + objSet.getValue());
						String objName = valArr[0];
						String fld = valArr[1];
						String objId = r.getValue(objSet.getName(), ValueType.STRING);
						mappingMap.computeIfAbsent(objName, k -> new ObjectMapping(objName,fld)).addId(objId);
					}
				}
			}
		}
		
		for(ObjectMapping om : mappingMap.values())
			om.runMapping();
		
		for(String ot : otMap.keySet()) {
			String q = "SELECT name__v FROM " + obj + " WHERE object_type__vr.api_name__v = '"+ot+"' ORDER BY created_date__v DESC LIMIT 1";
			Optional<QueryResult> opt = Query.queryStream(q).findFirst();
			if(opt.isPresent()) {
				QueryResult qr = opt.get();
				String name = qr.getValue("name__v", ValueType.STRING);
				String[] nameArr = StringUtils.split(name, "-");
				if(nameArr.length == 2) {
					BigDecimal bd = new BigDecimal(nameArr[1]);
					otMap.put(ot, bd.add(BigDecimal.valueOf(1)));
				}
			}
		}
		
		for(Record r : recList) {
			String ot = r.getValue("object_type__v", ValueType.STRING);
			String otName = RecordTypeUtil.getRTName(obj, ot);
			if(uuid.hasChild(otName)) {
				Setting otSet = uuid.getChild(otName);
				String fldVal = otSet.getValue();
				String uuidVal;
				if(fldVal.contains("#")) {
					BigDecimal curVal = otMap.get(otName);
					uuidVal = fldVal.replace("#", curVal.toString());
					otMap.put(otName, curVal.add(new BigDecimal(1)));
				}else {
					String[] fldArr = StringUtils.split(fldVal,",");
					List<String> valSet = VaultCollections.newList();
					for(String fld : fldArr) {
						String val = r.getValue(fld, ValueType.STRING);
						if(otSet.hasChild(fld)) {
							Setting objSet = otSet.getChild(fld);
							String objName = StringUtils.split(objSet.getValue(),":")[0];
							val = mappingMap.get(objName).getValue(val);
						}
						valSet.add(val);
					}
					uuidVal = String.join(":", valSet);
				}
				r.setValue(uuid.getValue(), uuidVal);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public static void assignUUID(RecordTriggerContext rtc, String obj) {
		List<Record> recList = VaultCollections.newList();
		for(RecordChange rc : rtc.getRecordChanges())
			recList.add(rc.getNew());
		assignUUID(recList,obj);
	}

}