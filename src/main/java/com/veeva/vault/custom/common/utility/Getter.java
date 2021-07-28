package com.veeva.vault.custom.common.utility;

import java.util.List;

import com.veeva.vault.sdk.api.core.StringUtils;
import com.veeva.vault.sdk.api.core.UserDefinedClassInfo;
import com.veeva.vault.sdk.api.core.ValueType;
import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.data.Record;
import com.veeva.vault.sdk.api.document.DocumentVersion;
import com.veeva.vault.sdk.api.query.QueryResult;

@UserDefinedClassInfo()
public class Getter {
	
	protected Object obj;
	public final static int RECORD = 0;
	public final static int QR = 1;
	public final static int DV = 2;
	protected int ot = -1;
	
	public Getter(Object o) {
		if(o instanceof Record)
			ot = RECORD;
		if(o instanceof QueryResult)
			ot = QR;
		if(o instanceof DocumentVersion)
			ot = DV;
		if(ot == -1)
			Log.throwError("Getter only takes Record, QueryResult or DocumentVersion");
		obj = o;
	}
	
	public static String getFldName(String fld) {
		String ret = fld;
		if(ret.contains(":"))
			ret = StringUtils.split(ret, ":")[0];
		return ret;
	}
	
	public int getObjectType() {
		return ot;
	}
	
	@SuppressWarnings("unchecked")
	public static List<Getter> getRecordList(List<Record> rList) {
		List<Getter>ret = VaultCollections.newList();
		for(Record r : rList)
			ret.add(new Getter(r));
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	public static List<Getter> getQRList(List<QueryResult> qList) {
		List<Getter>ret = VaultCollections.newList();
		for(QueryResult r : qList)
			ret.add(new Getter(r));
		return ret;
	}
	
	public Object getObj() {return obj;}
	
	@SuppressWarnings("unchecked")
	public  Object getValue(String val) {
		Object ret = null;
		if(val.contains(":")) {
			String[] valArr = StringUtils.split(val, ":");
			ret = getValue(valArr[0],getValueType(valArr[1]));
		}else {
			ret = getValue(val,ValueType.STRING);
		}
		
		return ret;
	}
	
	@SuppressWarnings("rawtypes")
	public static ValueType getValueType(String vtCode) {
		return RecordUtil.getValTypeAbrev(vtCode);
	}
	
	public <T> T getValue(String val, ValueType<T> vt) {
		T ret = null;
		switch(ot) {
		case QR:
			ret = ((QueryResult)obj).getValue(val, vt);
			break;
		case DV:
			ret = ((DocumentVersion)obj).getValue(val, vt);
			break;
		case RECORD:
			ret = ((Record)obj).getValue(val, vt);
			break;
		default:
			Log.throwError("Getter only takes Record, QueryResult or DocumentVersion");
		}
		return ret;
	}
}