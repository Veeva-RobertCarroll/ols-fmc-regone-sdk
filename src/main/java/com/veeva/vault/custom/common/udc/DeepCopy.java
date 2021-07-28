package com.veeva.vault.custom.common.udc;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.UserDefinedClassInfo;
import com.veeva.vault.sdk.api.core.ValueType;
import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.data.Record;
import com.veeva.vault.sdk.api.data.RecordService;
import com.veeva.vault.sdk.api.query.QueryResult;

@UserDefinedClassInfo(name="vps_deep_copy__c")
public class DeepCopy {
	
	private Set<String>flds;
	private String obj;
	private String parentFld;
	private String fieldFrom,fieldTo;
	private RecordService rs = ServiceLocator.locate(RecordService.class);
	
	public DeepCopy(String o, String pf, Set<String>f,String ff,String ft) {
		flds = f;
		obj = o;
		parentFld = pf;
		fieldFrom = ff;
		fieldTo = ft;
	}
	
	@SuppressWarnings("unchecked")
	public void copy(List<Record> originals) {
		List<Getter> getterList = VaultCollections.newList();
		List<Record> newValueList = VaultCollections.newList();
		for(Record r : originals) {
			Record newR = rs.newRecord(obj);
			for(String s : flds) {
				newR.setValue(s, Utility.getValue(r, s));
			}
			newR.setValue(fieldTo, r.getValue(fieldFrom, ValueType.STRING));
			newR.setValue(fieldFrom, "");
			newR.setValue("id", r.getValue("id", ValueType.STRING));
			newValueList.add(newR);
			getterList.add(new Getter(newR));
		}
		copy(getterList, VaultCollections.newMap());
		
		//Clear the copy field
		List<Record> update = VaultCollections.newList();
		for(Record r : originals) {
			Record newR = rs.newRecord(obj);
			newR.setValue("id", r.getValue("id", ValueType.STRING));
			newR.setValue(fieldFrom, "");
			newR.setValue("migration__c", true);
			update.add(newR);
		}
		Utility.save(update);
	}
	
	@SuppressWarnings("unchecked")
	public void copy(List<Getter> originals, Map<String,String> originalToCopyIdMap) {
		List<Record> copies = VaultCollections.newList();
		Set<String> originalIdSet = VaultCollections.newSet();
		for(Getter original : originals) {
			Record copy = rs.newRecord(obj);
			originalIdSet.add((String)original.getValue("id", ValueType.STRING));
			for(String fld : flds) {
				Object value = Utility.getValue(original, fld);
				if(fld.equals(parentFld)) {
					String parentId = (String)value;
					if(originalToCopyIdMap.containsKey(parentId))
						value = originalToCopyIdMap.get(parentId);
				}
				copy.setValue(fld, value);
			}
			copies.add(copy);
		}
		
		List<String> copyIdList = Utility.save(copies);
		for(int i = 0; i < copyIdList.size();i++) {
			originalToCopyIdMap.put((String)originals.get(i).getValue("id", ValueType.STRING), copyIdList.get(i));
		}
		
		Set<String> queryFlds = VaultCollections.newSet();
		queryFlds.add("id");
		queryFlds.addAll(flds);
		queryFlds.add(parentFld);
		
		String q = "SELECT " + String.join(",", queryFlds) + " FROM " + obj + " WHERE " + parentFld + " CONTAINS ('" + String.join("','", originalIdSet) + "')";
		List<Getter> newOriginals = VaultCollections.newList();
		for(QueryResult qr : Utility.queryList(q))
			newOriginals.add(new Getter(qr));
		
		if(newOriginals.size() > 0)
			copy(newOriginals,originalToCopyIdMap);
	}

}