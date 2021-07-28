package com.veeva.vault.custom.common.uds;

import com.veeva.vault.custom.common.utility.Getter;
import com.veeva.vault.custom.common.utility.Log;
import com.veeva.vault.custom.common.utility.Query;
import com.veeva.vault.custom.common.utility.RecordUtil;
import com.veeva.vault.custom.common.utility.Setting;
import com.veeva.vault.custom.fmc.util.ValidationUtils;
import com.veeva.vault.sdk.api.core.UserDefinedServiceInfo;
import com.veeva.vault.sdk.api.core.ValueType;
import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.core.VaultCollectors;
import com.veeva.vault.sdk.api.query.QueryResult;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.veeva.vault.custom.fmc.Constants.CustomSetting.FIELDS;
import static com.veeva.vault.custom.fmc.Constants.CustomSetting.OBJECT;

@UserDefinedServiceInfo
public class CSServiceImpl implements CSService{
	@SuppressWarnings("unchecked")
	public Map<String,Setting> getCSMap(){
		Map<String,Setting> csMap = VaultCollections.newMap();
		String q = "select id,name__v,value__c,parent__cr.external_id__c,external_id__c from custom_settings__c WHERE status__v = 'active__v'";
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
        Log.debug("csMap Built");
        for(String parentExt : childMap.keySet()) {
        	Setting parent = csMap.get(parentExt);
        	for(String kidExt : childMap.get(parentExt)) {
        		parent.addChild(csMap.get(kidExt));
        	}
        }
        Log.debug("parents Updated");
        return csMap;
	}

    @Override
    public List<String> getFieldNameAndTypeList(Setting fieldsCustomSetting) {
        return fieldsCustomSetting.getKids2()
            .stream()
            .map(fieldSetting -> fieldSetting.getName())
            .collect(VaultCollectors.toList());
    }

    @Override
    public List<String> getFieldNames(List<String> fieldNameAndTypeList) {
        List<String> selectFieldList = fieldNameAndTypeList.stream()
            .map(fieldNameType -> RecordUtil.getFieldName(fieldNameType))
            .collect(VaultCollectors.toList());
        return selectFieldList;
    }

    @Override
    public Object getFieldValue(Getter getter, Setting fieldsCustomSetting, String fieldNameAndType, Map<String, Map<String, String>> srcRecIdToNewRecIdByRelatedObjMap) {
        Object fieldValue;
        String objectName = Optional.ofNullable(fieldsCustomSetting)
            .map(fieldCs -> fieldCs.getChild(fieldNameAndType))
            .map(childCs -> childCs.getValue(OBJECT))
            .orElse(null);
        Map<String, String> relatedObjSrcRecIdToNewRecId = srcRecIdToNewRecIdByRelatedObjMap.get(objectName);
        if (!ValidationUtils.isNullOrBlank(objectName) && relatedObjSrcRecIdToNewRecId != null && relatedObjSrcRecIdToNewRecId.size() > 0) {
            fieldValue = relatedObjSrcRecIdToNewRecId.get(RecordUtil.getValue(getter, fieldNameAndType));
        } else {
            fieldValue = RecordUtil.getValue(getter, fieldNameAndType);
        }
        return fieldValue;
    }
}