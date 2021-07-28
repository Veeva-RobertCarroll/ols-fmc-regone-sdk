package com.veeva.vault.custom.common.udc;

import com.veeva.vault.sdk.api.core.UserDefinedClassInfo;
import com.veeva.vault.sdk.api.data.Record;
import com.veeva.vault.sdk.api.document.DocumentVersion;

@UserDefinedClassInfo(name="vps_setter__c")
public class Setter extends Getter{

	public Setter(Object o) {
		super(o);
		if(ot == QR)
			Utility.throwError("Setter only takes Record or DocumentVersion");
	}
	
	public void setValue(String fld, Object val) {
		switch(ot) {
		case DV:
			((DocumentVersion)obj).setValue(fld, val);
			break;
		case RECORD:
			((Record)obj).setValue(fld, val);
			break;
		default:
			Utility.throwError("Setter only takes Record, QueryResult or DocumentVersion");
		}
	}

}