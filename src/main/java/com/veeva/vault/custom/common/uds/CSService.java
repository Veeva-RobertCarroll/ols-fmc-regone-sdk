package com.veeva.vault.custom.common.uds;

import java.util.List;
import java.util.Map;

import com.veeva.vault.custom.common.utility.Getter;
import com.veeva.vault.custom.common.utility.Setting;
import com.veeva.vault.sdk.api.core.UserDefinedService;
import com.veeva.vault.sdk.api.core.UserDefinedServiceInfo;

@UserDefinedServiceInfo
public interface CSService extends UserDefinedService{
	public Map<String,Setting> getCSMap();

    /**
     * Retrieves a list of strings representing the field name and type of the fields under the "fields" custom setting
     * @param customSetting custom setting containing the fields as children
     * @return list of field name and type strings in the form of fieldName:type
     */
    List<String> getFieldNameAndTypeList(Setting customSetting);

    /**
     * Extracts the list of field names from the list of field name and type strings
     * @param fieldNameAndTypeList list of field name and type strings in the form of fieldName:type
     * @return list of just the field names
     */
    List<String> getFieldNames(List<String> fieldNameAndTypeList);

    /**
     * Get field Value for field name belonging to the current object of the getter (e.g. composition__c)
     * If the field's custom setting has a child named "object" then it indicates that this field is a reference field
     * The value of that object custom setting represents the name of the related object (e.g. grouping__c) whose newly copied record id needs to be referenced in this field
     * The field value will be retrieved from the srcRecIdToNewRecIdByRelatedObjMap using the value of the field (e.g. grouping__c) in the source record
     * If the field's custom setting doesn't have any children then the value in the source record is directly copied over
     * @param getter source record containing field values that need to be copied over.
     * @param fieldsCustomSetting "fields" custom setting containing the configuration of all fields to be copied over
     * @param fieldNameAndType name of the field for which value is returned along with it's data type
     * @param srcRecIdToNewRecIdByRelatedObjMap map of source record ids to new record ids of related objects to be used for populating references among them
     * @return field value
     */
    Object getFieldValue(Getter getter, Setting fieldsCustomSetting, String fieldNameAndType, Map<String, Map<String, String>> srcRecIdToNewRecIdByRelatedObjMap);
}