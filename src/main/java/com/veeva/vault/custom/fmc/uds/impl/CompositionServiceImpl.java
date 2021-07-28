package com.veeva.vault.custom.fmc.uds.impl;

import com.veeva.vault.custom.common.uds.CSService;
import com.veeva.vault.custom.common.utility.CustomSettings;
import com.veeva.vault.custom.common.utility.Getter;
import com.veeva.vault.custom.common.utility.RecordUtil;
import com.veeva.vault.custom.common.utility.Setting;
import com.veeva.vault.custom.fmc.Constants;
import com.veeva.vault.custom.fmc.Constants.Composition;
import com.veeva.vault.custom.fmc.Constants.Grouping;
import com.veeva.vault.custom.fmc.Constants.Formulation;
import com.veeva.vault.custom.fmc.uds.CompositionService;
import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.UserDefinedServiceInfo;
import com.veeva.vault.sdk.api.core.ValueType;
import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.data.Record;
import com.veeva.vault.sdk.api.data.RecordService;
import com.veeva.vault.sdk.api.query.QueryResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of the service containing functionality related to Composition object
 */
@UserDefinedServiceInfo
public class CompositionServiceImpl implements CompositionService {

    @Override
    public void copyRegisteredSpecCompositions(Map<String, Record> registeredSpecIdToRecordMap, Map<String, List<QueryResult>> versionSourceIdToCompositionsMap, Map<String, String> groupingSrcRecIdToNewRecIdMap) {
        RecordService recordService = ServiceLocator.locate(RecordService.class);
        List<Record> compositions = VaultCollections.newList();

        CSService csService = ServiceLocator.locate(CSService.class);
        Setting fieldsCustomSetting = Optional.ofNullable(CustomSettings.getCS(Constants.CustomSetting.CREATE_COUNTRY_REGISTRATION_EXT_ID, Composition.NAME))
            .map(customSetting -> customSetting.getChild(Constants.CustomSetting.FIELDS))
            .orElse(null);

        if (fieldsCustomSetting != null) {
            List<String> selectFieldNameAndTypeList = csService.getFieldNameAndTypeList(fieldsCustomSetting);

            Map<String, Map<String, String>> groupingSrcRecIdToNewRecIdByGroupingObjMap = VaultCollections.newMap();
            groupingSrcRecIdToNewRecIdByGroupingObjMap.put(Grouping.NAME, groupingSrcRecIdToNewRecIdMap);

            registeredSpecIdToRecordMap.forEach((registeredSpecId, registeredSpecRecord) -> {
                String versionSourceId = registeredSpecRecord.getValue(Formulation.Field.VERSION_SOURCE, ValueType.STRING);
                List<QueryResult> compositionQueryResults = versionSourceIdToCompositionsMap.get(versionSourceId);
                List<Getter> getters = Getter.getQRList(compositionQueryResults);
                getters.forEach(getter -> {
                    Record composition = recordService.newRecord(Composition.NAME);
                    selectFieldNameAndTypeList.forEach(fieldNameAndType -> {
                        Object fieldValue = csService.getFieldValue(getter, fieldsCustomSetting, fieldNameAndType, groupingSrcRecIdToNewRecIdByGroupingObjMap);
                        String fieldName = RecordUtil.getFieldName(fieldNameAndType);
                        composition.setValue(fieldName, fieldValue);
                    });
                    composition.setValue(Composition.Field.SPEC, registeredSpecId);
                    composition.setValue(Composition.Field.PARENT, versionSourceId);
                    compositions.add(composition);
                });
            });
            RecordUtil.save(compositions);
        }
    }
}