package com.veeva.vault.custom.common.utility;

import java.util.List;

import com.veeva.vault.sdk.api.core.UserDefinedClassInfo;
import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.data.Record;
import com.veeva.vault.sdk.api.data.RecordChange;
import com.veeva.vault.sdk.api.data.RecordEvent;
import com.veeva.vault.sdk.api.data.RecordTriggerContext;

@UserDefinedClassInfo()
public class RecordTriggerContextWrapper {
	
	private RecordTriggerContext cont;
	private List<Getter> getterList = null;
	private List<Setter> setterList = null;
	
	public RecordTriggerContextWrapper(RecordTriggerContext r) {
		cont=r;
	}
	
	public List<Getter> getGetter(){
		if(getterList == null)
			populateLists();
		return getterList;
	}
	
	public List<Setter> getSetter(){
		if(setterList == null)
			populateLists();
		return setterList;
	}
	
	@SuppressWarnings("unchecked")
	private void populateLists() {
		getterList = VaultCollections.newList();
		setterList = VaultCollections.newList();
		for(RecordChange rc : cont.getRecordChanges()) {
			Record r = rc.getNew();
			Getter g = new Getter(r);
			Setter s = new Setter(r);
			getterList.add(g);
			setterList.add(s);
		}
	}
	
	public boolean isInsert() {
		boolean ret = false;
		if(cont.getRecordEvent().equals(RecordEvent.BEFORE_INSERT) || cont.getRecordEvent().equals(RecordEvent.AFTER_INSERT))
			ret = true;
		return ret;
	}
	
	public boolean isDelete() {
		boolean ret = false;
		if(cont.getRecordEvent().equals(RecordEvent.BEFORE_DELETE) || cont.getRecordEvent().equals(RecordEvent.AFTER_DELETE))
			ret = true;
		return ret;
	}
	
	public boolean isUpdate() {
		boolean ret = false;
		if(cont.getRecordEvent().equals(RecordEvent.BEFORE_UPDATE) || cont.getRecordEvent().equals(RecordEvent.AFTER_UPDATE))
			ret = true;
		return ret;
	}
	
	public boolean isBefore() {
		boolean ret = false;
		if(cont.getRecordEvent().equals(RecordEvent.BEFORE_INSERT)  || cont.getRecordEvent().equals(RecordEvent.BEFORE_DELETE) || cont.getRecordEvent().equals(RecordEvent.BEFORE_UPDATE))
			ret = true;
		return ret;
	}
	public boolean isAfter() {
		boolean ret = false;
		if(cont.getRecordEvent().equals(RecordEvent.AFTER_INSERT)  || cont.getRecordEvent().equals(RecordEvent.AFTER_DELETE) || cont.getRecordEvent().equals(RecordEvent.AFTER_UPDATE))
			ret = true;
		return ret;
	}
}