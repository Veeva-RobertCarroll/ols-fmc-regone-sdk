package com.veeva.vault.custom.common.trigger;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.veeva.vault.custom.common.udc.DeepCopy;
import com.veeva.vault.custom.common.udc.Query;
import com.veeva.vault.custom.common.udc.RecordTriggerContextWrapper;
import com.veeva.vault.custom.common.udc.RecordUtil;
import com.veeva.vault.custom.common.udc.Utility;
import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.ValueType;
import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.data.Record;
import com.veeva.vault.sdk.api.data.RecordChange;
import com.veeva.vault.sdk.api.data.RecordEvent;
import com.veeva.vault.sdk.api.data.RecordService;
import com.veeva.vault.sdk.api.data.RecordTrigger;
import com.veeva.vault.sdk.api.data.RecordTriggerContext;
import com.veeva.vault.sdk.api.data.RecordTriggerInfo;
import com.veeva.vault.sdk.api.query.QueryResult;

@RecordTriggerInfo(object = "custom_settings__c", events = {RecordEvent.BEFORE_DELETE,RecordEvent.BEFORE_UPDATE,RecordEvent.BEFORE_INSERT,RecordEvent.AFTER_UPDATE,RecordEvent.AFTER_INSERT})
public class CustomSettings implements RecordTrigger{
	
	private static final String ext = "external_id__c";
	private static final String cs = "custom_settings__c";
	private static final String migration = "migration__c";
	private static final String parent = "parent__c";
	private static final String local = "local_id__c";
	private static final String sep = ":";
	private static final String id = "id";

	@SuppressWarnings("unchecked")
	@Override
	public void execute(RecordTriggerContext arg0) {
		if(arg0.getRecordEvent().equals(RecordEvent.BEFORE_DELETE)) {
			Set<String>idSet = VaultCollections.newSet();
			for(RecordChange rc : arg0.getRecordChanges()) {
				Record r = rc.getOld();
				idSet.add(r.getValue("id", ValueType.STRING));
			}
			RecordService rs = ServiceLocator.locate(RecordService.class);
			List<Record>delList = VaultCollections.newList();
			String q = "SELECT "+id+" FROM "+cs+" WHERE "+parent+" CONTAINS('"+String.join("','", idSet)+"')";
			Query.queryStream(q).forEach(qr -> {
				Record r = rs.newRecordWithId(cs, qr.getValue(id, ValueType.STRING));
				delList.add(r);
			});
			RecordUtil.delete(delList);
		}else {
			RecordTriggerContextWrapper rtc = new RecordTriggerContextWrapper(arg0);
			//Set External Id
			if(rtc.isBefore()) {
				Set<String> parentIdSet = VaultCollections.newSet();
				for(RecordChange rc : arg0.getRecordChanges()) {
					Record r = rc.getNew();
					boolean mig = isMigration(r);
					if(!mig) {
						String parentId = r.getValue(parent, ValueType.STRING);
						if(parentId != null && !parentId.equals(""))
							parentIdSet.add(parentId);
					}
					
					//Prevent changing the local Id as it would change the external id
					if(rtc.isUpdate() && Utility.fieldChanged(rc, local,ValueType.STRING, arg0.getRecordEvent()))
						Utility.throwError("Can not change Local Id");
				}
				
				Map<String,QueryResult> parentMap = null;
				if(parentIdSet.size() > 0) {
					Set<String>fldSet = VaultCollections.newSet();
					fldSet.add(ext);
					parentMap = Utility.getRecordMap(cs, fldSet, parentIdSet);
				}
				
				for(RecordChange rc : arg0.getRecordChanges()) {
					Record r = rc.getNew();
					boolean mig = isMigration(r);
					if(!mig) {
						String parentId = r.getValue(parent, ValueType.STRING);
						String localId = r.getValue(local, ValueType.STRING);
						if(parentId != null && !parentId.equals("")) {
							QueryResult parentRes = parentMap.get(parentId);
							String parentExt = parentRes.getValue(ext, ValueType.STRING);
							String newExtId = parentExt + sep + localId;
							r.setValue(ext, newExtId);
						}else {
							r.setValue(ext, localId);
						}
					}else {
						r.setValue(migration, false);
					}
				}
			//Check for copy
			}else {
				List<Record> copyRecords = VaultCollections.newList();
				for(RecordChange rc : arg0.getRecordChanges()) {
					Record r = rc.getNew();
					String copyId = r.getValue("copy_local_id__c", ValueType.STRING);
					if(copyId != null && !copyId.equals("")) {
						copyRecords.add(r);
					}
				}
				if(copyRecords.size() > 0) {
					Set<String> cpFields = getCopyFields();
					DeepCopy dc = new DeepCopy("custom_settings__c","parent__c",cpFields,"copy_local_id__c","local_id__c");
					dc.copy(copyRecords);
				}
			}
		}
		
	}
	
	@SuppressWarnings("unchecked")
	private static Set<String> getCopyFields(){
		Set<String> ret = VaultCollections.newSet();
		ret.add("local_id__c");
		ret.add("name__v");
		ret.add("parent__c");
		ret.add("value__c");
		return ret;
	}
	
	
	
	private static boolean isMigration(Record r) {
		boolean ret = false;
		Boolean mig = r.getValue(migration, ValueType.BOOLEAN);
		if(mig != null && mig)
			ret = true;
		return ret;
	}

}