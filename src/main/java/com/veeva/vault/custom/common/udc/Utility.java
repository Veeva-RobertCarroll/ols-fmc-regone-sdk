package com.veeva.vault.custom.common.udc;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.veeva.vault.sdk.api.core.StringUtils;

import com.veeva.vault.sdk.api.core.RollbackException;
import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.UserDefinedClassInfo;
import com.veeva.vault.sdk.api.core.ValueType;
import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.data.Record;
import com.veeva.vault.sdk.api.data.RecordChange;
import com.veeva.vault.sdk.api.data.RecordEvent;
import com.veeva.vault.sdk.api.data.RecordService;
import com.veeva.vault.sdk.api.job.JobParameters;
import com.veeva.vault.sdk.api.job.JobService;
import com.veeva.vault.sdk.api.query.QueryResponse;
import com.veeva.vault.sdk.api.query.QueryResult;
import com.veeva.vault.sdk.api.query.QueryService;
import com.veeva.vault.sdk.api.role.RecordRoleService;
import com.veeva.vault.sdk.api.role.RecordRoleUpdate;

@UserDefinedClassInfo()
public class Utility {
	
	@SuppressWarnings("unchecked")
	public static List<QueryResult> queryList(String q){
		List<QueryResult> ret = VaultCollections.newList();
		Iterator<QueryResult> i = query(q);
		while(i.hasNext())ret.add(i.next());
		return ret;
	}
	
	public static Stream<QueryResult> queryStream(String q){
		Debug.debug("Running query:" + q);
		QueryService queryService = ServiceLocator.locate(QueryService.class);
        QueryResponse queryResponse = queryService.query(q);
        Debug.debug("Count:" + queryResponse.getResultCount());
        return queryResponse.streamResults();
	}
	
	@SuppressWarnings("unchecked")
	public static Map<String,String> getIdMap(String id,String val, String obj, String where){
		Map<String,String> ret = VaultCollections.newMap();
		String q = "SELECT " + id + "," + val + " FROM " + obj;
		if(where != null)
			q += " WHERE " + where;
		for(QueryResult qr : queryList(q))
			ret.put(qr.getValue(id, ValueType.STRING), qr.getValue(val, ValueType.STRING));
		return ret;
	}
	
	public static Map<String,String> getIdMap(String id,String val, String obj){
		return getIdMap(id,val,obj,null);
	}
	
	@SuppressWarnings("unchecked")
	public static Map<String,QueryResult> getRecordMap(String obj, Set<String> fldSet, Set<String> idSet){
		Map<String,QueryResult> ret = VaultCollections.newMap();
		Set<String> curFieldSet = VaultCollections.newSet();
		for(String s : fldSet)curFieldSet.add(s);
		curFieldSet.add("id");
		String q = "SELECT " + String.join(",", curFieldSet) + " FROM " + obj;
		if(idSet != null && idSet.size() > 0) {
			q += " WHERE id CONTAINS ('" + String.join("','", idSet) + "')";
		}
		
		queryStream(q).forEach(qr -> {
			ret.put(qr.getValue("id", ValueType.STRING), qr);
		});
		return ret;
	}
	
	public static Iterator<QueryResult> query(String q){
        return queryStream(q).iterator();
	}
	
	@SuppressWarnings("unchecked")
	public static void startWF(Record record, String wf,String action,Map<String,Object> params) {
		List<Record> recordList = VaultCollections.newList();
		recordList.add(record);
		startWF(recordList,wf,action,params);
	}
	
	public static void addRole(Record r, String role,String user) {
		editRole(r,role,user,true);
	}
	
	public static void removeRole(Record r, String role,String user) {
		editRole(r,role,user,false);
	}
	
	@SuppressWarnings("unchecked")
	public static void editRole(Record r, String role,String user,boolean add) {
		RecordRoleService roleService = ServiceLocator.locate(RecordRoleService.class);
		RecordRoleUpdate roleUpdate = roleService.newRecordRoleUpdate(role, r);
		List<String> userList = VaultCollections.newList();
		userList.add(user);
		if(add) {
			roleUpdate.addUsers(userList);
		}else {
			roleUpdate.removeUsers(userList);
		}
		List<RecordRoleUpdate> ruList = VaultCollections.newList();
		ruList.add(roleUpdate);
		roleService.batchUpdateRecordRoles(ruList).onErrors(batchOperationErrors -> {
            batchOperationErrors.stream().findFirst().ifPresent(error -> {
                String errMsg = error.getError().getMessage();
                throw new RollbackException("OPERATION_NOT_ALLOWED", "Role update failed: "
                        + errMsg);
            });
        })
        .execute();
	}
	
	
	
	public static void startWF(List<Record> records,String workflow, String action,Map<String,Object>params) {
		//Get an instance of Job for invoking user actions, such as changing state and starting workflow
        JobService jobService = ServiceLocator.locate(JobService.class);
        JobParameters jobParameters = jobService.newJobParameters(workflow);
        jobParameters.setValue("user_action_name", action); 
        jobParameters.setValue("records", records);
        for(String s : params.keySet()) {
        	jobParameters.setValue(s, params.get(s));
        }
        jobService.run(jobParameters);
	}
	
	public static ValueType getValueType(String type) {
		if(type.equals("date"))return ValueType.DATE;
		if(type.equals("pl"))return ValueType.PICKLIST_VALUES;
		return ValueType.STRING;
	}
	
	public static ValueType getFieldValueType(String s) {
		String[] arr = StringUtils.split(s, ":");
		String vt = "";
		if(arr.length > 1) {
			vt = arr[1];
		}
		return getValueType(vt);
	}
	
	public static String getStringValue(Record r, String s) {
		ValueType vt = getFieldValueType(s);
		String fn = getFieldName(s);
		String ret = null;
		if(vt.equals(ValueType.STRING)) {
			ret = (String)r.getValue(fn, vt);
		}else if(vt.equals(ValueType.PICKLIST_VALUES)) {
			List<String> strList = (List<String>) r.getValue(fn, vt); 
			if(strList != null)
				ret = String.join(",", strList);
		}
		return ret;
	}
	
	public static String getStringValue(QueryResult r, String s) {
		ValueType vt = getFieldValueType(s);
		String fn = getFieldName(s);
		String ret = null;
		if(vt.equals(ValueType.STRING)) {
			ret = (String)r.getValue(fn, vt);
		}else if(vt.equals(ValueType.PICKLIST_VALUES)) {
			List<String> strList = (List<String>) r.getValue(fn, vt); 
			if(strList != null)
				ret = String.join(",", strList);
		}
		return ret;
	}
	
	public static String getFieldName(String s) {
		return StringUtils.split(s, ":")[0];
	}
	
	public static Object getValue(Getter r, String fldStr) {
		String[] stringArr = StringUtils.split(fldStr, ":");
		String fld = stringArr[0];
		String type = "";
		if(stringArr.length == 2)
			type = stringArr[1];
		
		return r.getValue(fld, getValueType(type));
	}

	
	public static Object getValue(Record r, String fldStr) {
		return getValue(new Getter(r),fldStr);
	}
	public static Object getValue(QueryResult r, String fldStr) {
		return getValue(new Getter(r),fldStr);
	}
	
	public static List<Record> saveBatchCheck(List<Record> in){
		return saveBatchCheck(in,500);
	}
	
	public static List<Record> saveBatchCheck(List<Record> in, int max){
		List<Record> ret;
		if(in.size() >= max) {
			save(in);
			ret = VaultCollections.newList();
		}else {
			ret = in;
		}
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	public static List<String> save(List<Record> recordListIn) {
		List<String> ret = VaultCollections.newList();
		if(recordListIn.size() > 0) {
			List<Record> recordList = VaultCollections.newList();
			for(int i = 0;i<recordListIn.size();i++) {
				recordList.add(recordListIn.get(i));
				if(i % 500 == 0 || i == recordListIn.size() - 1) {
					RecordService recordService = ServiceLocator.locate(RecordService.class);
					recordService.batchSaveRecords(recordList)
			        .onErrors(batchOperationErrors -> {
			            batchOperationErrors.stream().findFirst().ifPresent(error -> {
			                String errMsg = error.getError().getMessage();
			                int errPosition = error.getInputPosition();
			                //String name = recordList.get(errPosition).getValue("name__v", ValueType.STRING);
			                throw new RollbackException("OPERATION_NOT_ALLOWED", "Unable to perform batch operation: "
			                          + errMsg);
			            });
			        }).onSuccesses(success -> {
			        	for(int p = 0; p < success.size();p++)
			        		ret.add(success.get(p).getRecordId());
			        })
			        .execute();
					recordList = VaultCollections.newList();
				}
			}
		}
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	public static Map<String,Record> saveMap(List<Record> recordListIn) {
		Map<String,Record> ret = VaultCollections.newMap();
		
		RecordService recordService = ServiceLocator.locate(RecordService.class);
		recordService.batchSaveRecords(recordListIn)
        .onErrors(batchOperationErrors -> {
            batchOperationErrors.stream().findFirst().ifPresent(error -> {
                String errMsg = error.getError().getMessage();
                int errPosition = error.getInputPosition();
                //String name = recordList.get(errPosition).getValue("name__v", ValueType.STRING);
                throw new RollbackException("OPERATION_NOT_ALLOWED", "Unable to perform batch operation: "
                          + errMsg);
            });
        }).onSuccesses(success -> {
        	for(int p = 0; p < success.size();p++)
        		ret.put(success.get(p).getRecordId(),recordListIn.get(success.get(p).getInputPosition()));
        })
        .execute();
				
		
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	public static void deleteQuery(String obj, String where) {
		RecordService rs = ServiceLocator.locate(RecordService.class);
		String q = "SELECT id FROM " + obj + " WHERE "+ where;
		List<Record>delList = VaultCollections.newList();
		for(QueryResult qr : queryList(q)) {
			Record r = rs.newRecord(obj);
			r.setValue("id", qr.getValue("id", ValueType.STRING));
			delList.add(r);
		}
		delete(delList);
	}
	
	public static void delete(List<Record> recordListIn) {		
		if(recordListIn.size() > 0) {
			List<Record> recordList = VaultCollections.newList();
			for(int i = 0;i<recordListIn.size();i++) {
				recordList.add(recordListIn.get(i));
				if(i % 500 == 0 || i == recordListIn.size() - 1) {
					RecordService recordService = ServiceLocator.locate(RecordService.class);
					recordService.batchDeleteRecords(recordList)
			        .onErrors(batchOperationErrors -> {
			            batchOperationErrors.stream().findFirst().ifPresent(error -> {
			                String errMsg = error.getError().getMessage();
			                int errPosition = error.getInputPosition();
			                //String name = recordList.get(errPosition).getValue("name__v", ValueType.STRING);
			                throw new RollbackException("OPERATION_NOT_ALLOWED", "Unable to perform batch operation: "
			                          + errMsg);
			            });
			        })
			        .execute();
					recordList = VaultCollections.newList();
				}
			}
		}
	}
	
	public static void throwError(String errMsg) {
		throw new RollbackException("OPERATION_NOT_ALLOWED", errMsg);
	}
	
	
	
	public static boolean fieldChanged(RecordChange rc, String fld, ValueType vt,RecordEvent re) {
		Object old = "";
		if(re.equals(RecordEvent.AFTER_UPDATE) || re.equals(RecordEvent.BEFORE_UPDATE))old = rc.getOld().getValue(fld, vt);
		Object newObj = rc.getNew().getValue(fld, vt);
		boolean ret;
		if(newObj == null && old != null) {
			ret = true;
		}else if(newObj != null && old == null) {
			ret = true;
		}else if(newObj == null && old == null) {
			ret = false;
		}else {
			ret = !newObj.equals(old);
		}
		return ret;
	}
	
	public static String[] split(String str,String c) {
		List<String> retList = VaultCollections.newList();
		while(str.contains(c)) {
			int i = str.indexOf(c);
			if(i != 0)retList.add(str.substring(0,i));
			int pos = i + c.length();
			if(pos == str.length()) {
				str = "";
			}else {
				str = str.substring(pos);
			}	
		}
		if(!str.equals(""))retList.add(str);
		String[] ret = new String[retList.size()];
		for(int i = 0; i < retList.size();i++) {
			ret[i] = retList.get(i);
		}
		return ret;
	}
}