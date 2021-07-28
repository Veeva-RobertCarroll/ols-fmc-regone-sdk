package com.veeva.vault.custom.common.udc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import com.veeva.vault.sdk.api.core.RollbackException;
import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.StringUtils;
import com.veeva.vault.sdk.api.core.UserDefinedClassInfo;
import com.veeva.vault.sdk.api.core.ValueType;
import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.csv.CsvValueType;
import com.veeva.vault.sdk.api.data.Record;
import com.veeva.vault.sdk.api.data.RecordService;

@UserDefinedClassInfo()
public class RecordUtil {
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
	public static Map<Integer,String> saveMap(List<Record> recordList) {
		Map<Integer,String> ret = VaultCollections.newMap();
		if(recordList.size() > 0) {
			RecordService recordService = ServiceLocator.locate(RecordService.class);
			recordService.batchSaveRecords(recordList)
	        .onErrors(batchOperationErrors -> {
	            batchOperationErrors.stream().findFirst().ifPresent(error -> {
	                String errMsg = error.getError().getMessage();
	                int errPosition = error.getInputPosition();
	                String name = recordList.get(errPosition).getValue("name__v", ValueType.STRING);
	                throw new RollbackException("OPERATION_NOT_ALLOWED", "Unable to perform operation: "
	                        + name + " because of " + errMsg);
	            });
	        })
	        .onSuccesses(successBatch -> {
	        	successBatch.stream().forEach(success ->{
	        		int pos = success.getInputPosition();
	        		String id = success.getRecordId();
	        		ret.put(pos, id);
	        	});
	        })
	        .execute();
		}
		return ret;
	}
	
	@SuppressWarnings("unchecked")
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
			                //int errPosition = error.getInputPosition();
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
	
	public static String getFieldName(String name) {
		String ret;
		if(name.contains(":")) {
			ret = StringUtils.split(name, ":")[0];
		}else { 
			ret = name;
		}
		return ret.trim();
	}
	
	@SuppressWarnings("rawtypes")
	public static ValueType getValType(String name) {
		ValueType ret = ValueType.STRING;
		String[]nameArr = StringUtils.split(name, ":");
		if(nameArr != null && nameArr.length == 2) {
			switch(nameArr[1]) {
				case "NUM":
					ret = ValueType.NUMBER;
					break;
				default:
					ret = ValueType.STRING;
					break;
			}
		}
		return ret;
	}
	
	@SuppressWarnings({ "rawtypes" })
	public static CsvValueType getCsvType(String type) {
		CsvValueType ret = CsvValueType.STRING;
		switch(type) {
		case "Date":
			ret = CsvValueType.DATE;
			break;
		case "Boolean":
			ret = CsvValueType.BOOLEAN;
			break;
		case "DateTime":
			ret = CsvValueType.DATETIME;
			break;
		case "Number":
			ret = CsvValueType.NUMBER;
			break;
		}
		return ret;
	}
	
	@SuppressWarnings("rawtypes")
	public static Object getObject(ValueType vt,String s) {
		Object ret;
		if(vt.equals(ValueType.STRING)) {
			ret = s;
		}else if(vt.equals(ValueType.NUMBER)){
			ret = new BigDecimal(s);
		}else {
			ret = s;
		}
		return ret;
	}
}