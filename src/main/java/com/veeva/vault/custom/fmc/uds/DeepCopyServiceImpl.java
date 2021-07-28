package com.veeva.vault.custom.fmc.uds;

import com.veeva.vault.custom.common.uds.CSService;
import com.veeva.vault.custom.common.utility.CustomSettings;
import com.veeva.vault.custom.common.utility.Getter;
import com.veeva.vault.custom.common.utility.Query;
import com.veeva.vault.custom.common.utility.RecordUtil;
import com.veeva.vault.custom.common.utility.Setting;
import com.veeva.vault.custom.fmc.Constants.CustomSetting;
import com.veeva.vault.custom.fmc.util.ArgumentUtils;
import com.veeva.vault.custom.fmc.util.ValidationUtils;
import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.StringUtils;
import com.veeva.vault.sdk.api.core.UserDefinedServiceInfo;
import com.veeva.vault.sdk.api.core.ValueType;
import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.core.VaultCollectors;
import com.veeva.vault.sdk.api.data.Record;
import com.veeva.vault.sdk.api.data.RecordEvent;
import com.veeva.vault.sdk.api.data.RecordService;
import com.veeva.vault.sdk.api.data.RecordTriggerContext;
import com.veeva.vault.sdk.api.query.QueryResult;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.veeva.vault.custom.fmc.Constants.ID_FIELD_NAME;

/**
 * Implementation of service to copy related records based on Custom Settings configuration object named "Deep Copy"
 */
@UserDefinedServiceInfo
public class DeepCopyServiceImpl implements DeepCopyService {

    static final String RELATED_RECORDS_QUERY = "SELECT id, %s from %s WHERE %s CONTAINS('%s') AND status__v = 'active__v'";

    @Override
    public void deepCopy(RecordTriggerContext recordTriggerContext, String objectName) {
        List<Record> deepCopyObjRecords = recordTriggerContext.getRecordChanges().stream()
            .map(recordChange -> recordChange.getNew())
            .filter(newRecord -> !ValidationUtils.isNullOrBlank(newRecord.getSourceRecordId()))
            .collect(VaultCollectors.toList());

        if (ValidationUtils.isNotEmpty(deepCopyObjRecords)) {
            Setting deepCopyCustomSettings = CustomSettings.getCS(CustomSetting.DEEP_COPY_EXTERNAL_ID, objectName);
            if (deepCopyCustomSettings != null) {
                if (RecordEvent.BEFORE_INSERT == recordTriggerContext.getRecordEvent()) {
                    String idField = deepCopyCustomSettings.getValue(CustomSetting.ID_FIELD);
                    if (!ValidationUtils.isNullOrBlank(idField)) {
                        deepCopyObjRecords.forEach(newRecord -> {
                            String sourceRecordId = newRecord.getSourceRecordId();
                            newRecord.setValue(idField, sourceRecordId);
                        });
                    }
                } else if (RecordEvent.AFTER_INSERT == recordTriggerContext.getRecordEvent()) {
                    deepCopyRelatedRecords(deepCopyObjRecords, deepCopyCustomSettings);
                }
            }
        }
    }

    /**
     * Copy records of related objects (e.g. grouping__c, composition__c) based on the order specified in the value field of the object's (formulation__v) custom setting
     * @param deepCopyObjRecords new records of the object being copied
     * @param deepCopyCustomSettings deep copy custom setting of the object
     */
    void deepCopyRelatedRecords(List<Record> deepCopyObjRecords, Setting deepCopyCustomSettings) {
        Map<String, Map<String, String>> srcRecIdToNewRecIdByRelatedObjMap = VaultCollections.newMap();

        RecordService recordService = ServiceLocator.locate(RecordService.class);
        CSService csService = ServiceLocator.locate(CSService.class);
        Map<String, String> srcRecIdToNewRecIdMap = deepCopyObjRecords.stream()
            .collect(VaultCollectors.toMap(
                newRecord -> newRecord.getSourceRecordId(),
                newRecord -> newRecord.getValue(ID_FIELD_NAME, ValueType.STRING)
            ));

        Stream.of(StringUtils.split(deepCopyCustomSettings.getValue(), ","))
            .forEach(relatedObjectName -> {
                deepCopyCustomSettings.getChildren(relatedObjectName)
                    .forEach(setting -> {
                        copyRelatedRecords(recordService, csService, setting, srcRecIdToNewRecIdMap, srcRecIdToNewRecIdByRelatedObjMap);
                    });
            });
    }

    /**
     * Copy and persist records of an object (e.g. composition__c) related to the main object being copied over
     * @param recordService Record Service
     * @param csService Custom Settings Service
     * @param setting related object's (e.g. composition__c) custom setting consisting of the fields to be copied over
     * @param srcRecIdToNewRecIdMap map of source record ids to new record ids of the main object being copied over e.g. formulation__c
     * @param srcRecIdToNewRecIdByRelatedObjMap map of source record ids to new record ids of related objects to be used for populating references among them
     */
    void copyRelatedRecords(RecordService recordService, CSService csService, Setting setting, Map<String, String> srcRecIdToNewRecIdMap, Map<String, Map<String, String>> srcRecIdToNewRecIdByRelatedObjMap) {

        List<Record> copiedRecords = VaultCollections.newList();
        Map<Integer, String> relatedObjNewRecIndexToSrcRecIdMap = VaultCollections.newMap();
        int relatedObjRecordIndex = 0;

        Setting fieldsCustomSetting = setting.getChild(CustomSetting.FIELDS);

        if (fieldsCustomSetting != null) {

            String relatedObjectName = setting.getName();
            String referenceObjectFieldName = setting.getValue();

            ArgumentUtils.validateNotNull(relatedObjectName, "Related Object Name must not be empty or null");
            ArgumentUtils.validateNotNull(referenceObjectFieldName, "Reference Object Field Name must not be empty or null");

            Map<String, String> relatedObjSrcRecIdToNewRecIdMap = srcRecIdToNewRecIdByRelatedObjMap.computeIfAbsent(relatedObjectName, (key) -> VaultCollections.newMap());

            if (!ValidationUtils.isNullOrBlank(referenceObjectFieldName)) {
                List<String> fieldNameAndTypeList = csService.getFieldNameAndTypeList(fieldsCustomSetting);
                ArgumentUtils.validateNotNullAndEmptyCollection(fieldNameAndTypeList, "fields custom settings configuration must not be empty");

                List<String> selectFieldList = csService.getFieldNames(fieldNameAndTypeList);
                List<Getter> getters = getRelatedRecords(selectFieldList, relatedObjectName, referenceObjectFieldName, srcRecIdToNewRecIdMap);

                for (Getter getter: getters) {
                    Record newRecord = recordService.newRecord(relatedObjectName);
                    String srcRecId = getter.getValue(referenceObjectFieldName, ValueType.STRING);
                    String relSrcRecId = getter.getValue("id", ValueType.STRING);
                    newRecord.setValue(referenceObjectFieldName, srcRecIdToNewRecIdMap.get(srcRecId));
                    fieldNameAndTypeList.forEach(fieldNameAndType -> {
                        Object fieldValue = csService.getFieldValue(getter, fieldsCustomSetting, fieldNameAndType, srcRecIdToNewRecIdByRelatedObjMap);
                        String fieldName = RecordUtil.getFieldName(fieldNameAndType);
                        newRecord.setValue(fieldName, fieldValue);
                    });
                    relatedObjNewRecIndexToSrcRecIdMap.put(relatedObjRecordIndex, relSrcRecId);
                    relatedObjRecordIndex++;
                    copiedRecords.add(newRecord);
                }
                if (ValidationUtils.isNotEmpty(copiedRecords)) {
                    List<String> records = RecordUtil.save(copiedRecords);
                    for (int i = 0; i < relatedObjNewRecIndexToSrcRecIdMap.size(); i++) {
                        String srcRecId = relatedObjNewRecIndexToSrcRecIdMap.get(i);
                        String newRecId = records.get(i);
                        relatedObjSrcRecIdToNewRecIdMap.put(srcRecId, newRecId);
                    }
                }
            }
        }
    }

    protected List<Getter> getRelatedRecords(List<String> selectFieldList, String relatedObjectName, String referenceObjectFieldName, Map<String, String> srcRecIdToNewRecIdMap) {
        List<Getter> getters = VaultCollections.newList();

        if (ValidationUtils.isNotEmpty(selectFieldList)) {
            selectFieldList.add(referenceObjectFieldName);
            String relatedRecordsQuery = String.format(RELATED_RECORDS_QUERY, String.join(", ", selectFieldList),
                relatedObjectName, referenceObjectFieldName, String.join("','", srcRecIdToNewRecIdMap.keySet()));
            List<QueryResult> queryResults = Query.queryStream(relatedRecordsQuery).collect(VaultCollectors.toList());
            getters = Getter.getQRList(queryResults);
        }

        return getters;
    }
}