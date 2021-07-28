package com.veeva.vault.custom.fmc.uds.impl;

import com.veeva.vault.custom.common.uds.CSService;
import com.veeva.vault.custom.common.utility.CustomSettings;
import com.veeva.vault.custom.common.utility.Getter;
import com.veeva.vault.custom.common.utility.Query;
import com.veeva.vault.custom.common.utility.RecordTypeUtil;
import com.veeva.vault.custom.common.utility.RecordUtil;
import com.veeva.vault.custom.common.utility.Setting;
import com.veeva.vault.custom.fmc.Constants.Composition;
import com.veeva.vault.custom.fmc.Constants.CustomSetting;
import com.veeva.vault.custom.fmc.uds.CompositionService;
import com.veeva.vault.custom.fmc.uds.GroupingService;
import com.veeva.vault.custom.fmc.uds.LocationService;
import com.veeva.vault.custom.fmc.uds.RegisteredSpecificationService;
import com.veeva.vault.custom.fmc.util.ArgumentUtils;
import com.veeva.vault.custom.fmc.util.ValidationUtils;
import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.UserDefinedServiceInfo;
import com.veeva.vault.sdk.api.core.ValueType;
import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.core.VaultCollectors;
import com.veeva.vault.sdk.api.data.Record;
import com.veeva.vault.sdk.api.data.RecordService;
import com.veeva.vault.sdk.api.query.QueryResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.veeva.vault.custom.fmc.Constants.Formulation;
import static com.veeva.vault.custom.fmc.Constants.FormulationCountryJoin;
import static com.veeva.vault.custom.fmc.Constants.Record.ID;
import static com.veeva.vault.custom.fmc.Constants.Record.OBJECT_TYPE;

/**
 * Implementation of the service containing functionality related to Registered Specification formulation object type
 */
@UserDefinedServiceInfo
public class RegisteredSpecificationServiceImpl implements RegisteredSpecificationService {

    static final String PLANT_COUNTRY_SPEC_QUERY = "SELECT id, version_source__vr.title__v, %s, (SELECT id, name__v, formulation__c from groupings__cr WHERE status__v = 'active__v'), (SELECT grouping__c, %s from formulation_compositions1__cr WHERE status__v = 'active__v') from formulation__v WHERE id CONTAINS ('%s') AND status__v = 'active__v'";

    @Override
    public void createRegisteredSpecifications(List<Record> updatedFormulationCountryJoinRecords) {

        if (ValidationUtils.isNotEmpty(updatedFormulationCountryJoinRecords)) {
            Map<String, List<String>> plantCountrySpecToLocationsMap = getPlantCountrySpecToLocationsMap(updatedFormulationCountryJoinRecords);

            Map<String, QueryResult> specIdToQueryResultMap = VaultCollections.newMap();
            Map<String, List<QueryResult>> versionSourceIdToCompositionsMap = VaultCollections.newMap();
            Map<String, List<QueryResult>> versionSourceIdToGroupingsMap = VaultCollections.newMap();

            //For each plant country spec, query associated info needed to create registered specs (incl. groupings and compositions)
            Set<String> plantCountrySpecIds = plantCountrySpecToLocationsMap.keySet();

            CSService csService = ServiceLocator.locate(CSService.class);
            Setting compositionFieldsCustomSetting = Optional.ofNullable(CustomSettings.getCS(CustomSetting.CREATE_COUNTRY_REGISTRATION_EXT_ID, Composition.NAME))
                .map(customSetting -> customSetting.getChild(CustomSetting.FIELDS))
                .orElse(null);
            List<String> compositionSelectFieldNameAndTypeList = csService.getFieldNameAndTypeList(compositionFieldsCustomSetting);
            List<String> compositionFields = csService.getFieldNames(compositionSelectFieldNameAndTypeList);

            Setting fieldsCustomSetting = Optional.ofNullable(CustomSettings.getCS(CustomSetting.CREATE_COUNTRY_REGISTRATION_EXT_ID, CustomSetting.FIELDS))
                .orElse(null);
            ArgumentUtils.validateNotNull(fieldsCustomSetting, "fields custom setting is required");

            List<String> fieldNameAndTypeList = csService.getFieldNameAndTypeList(fieldsCustomSetting);
            ArgumentUtils.validateNotNull(fieldsCustomSetting, "plant country spec fields to be copied over are required");

            List<String> plantCountrySpecFields = csService.getFieldNames(fieldNameAndTypeList);

            String query = String.format(PLANT_COUNTRY_SPEC_QUERY, String.join(",", plantCountrySpecFields), String.join(",", compositionFields), String.join("','", plantCountrySpecIds));

            Query.queryStream(query).forEach(queryResult -> {
                String specId = queryResult.getValue(ID, ValueType.STRING);
                specIdToQueryResultMap.put(specId, queryResult);
                List<QueryResult> compositionQueryResults = queryResult.getSubqueryResponse(Formulation.Relationship.COMPOSITIONS2).streamResults().collect(VaultCollectors.toList());
                List<QueryResult> groupingQueryResults = queryResult.getSubqueryResponse(Formulation.Relationship.GROUPINGS).streamResults().collect(VaultCollectors.toList());
                String versionSourceId = queryResult.getValue(Formulation.Field.VERSION_SOURCE, ValueType.STRING);
                versionSourceIdToCompositionsMap.put(versionSourceId, compositionQueryResults);
                versionSourceIdToGroupingsMap.put(versionSourceId, groupingQueryResults);
            });

            //Persist registered spec records
            Map<String, Record> registeredSpecIdToRecordMap = saveRegisteredSpecifications(plantCountrySpecToLocationsMap, specIdToQueryResultMap, fieldNameAndTypeList);

            //Create Grouping records for each newly created registered spec record
            GroupingService groupingService = ServiceLocator.locate(GroupingService.class);
            Map<String, String> groupingSrcRecIdToNewRecIdMap = groupingService.copyRegisteredSpecGroupings(registeredSpecIdToRecordMap, versionSourceIdToGroupingsMap);

            //Create Grouping records for each newly created registered spec record
            CompositionService compositionService = ServiceLocator.locate(CompositionService.class);
            compositionService.copyRegisteredSpecCompositions(registeredSpecIdToRecordMap, versionSourceIdToCompositionsMap, groupingSrcRecIdToNewRecIdMap);
        }
    }

    /**
     * Persists registered specification records and returns a map of persisted ids to records
     * @param plantCountrySpecToLocationsMap map of plant country specification id to location ids
     * @param specIdToQueryResultMap map of plant country specification id to plant country spec query result
     * @return map of persisted id to corresponding records
     */
    Map<String, Record> saveRegisteredSpecifications(Map<String, List<String>> plantCountrySpecToLocationsMap, Map<String, QueryResult> specIdToQueryResultMap, List<String> fieldNameAndTypeList) {
        Map<String, Record> registeredSpecIdToRecordMap = VaultCollections.newMap();
        Map<Integer, Record> registeredSpecNewRecIndexToSrcRecMap = VaultCollections.newMap();

        List<Record> registeredSpecRecords = getRegisteredSpecRecords(plantCountrySpecToLocationsMap, specIdToQueryResultMap, registeredSpecNewRecIndexToSrcRecMap, fieldNameAndTypeList);

        if (ValidationUtils.isNotEmpty(registeredSpecRecords)) {
            List<String> registeredSpecRecordIds = RecordUtil.save(registeredSpecRecords);

            for (int i = 0; i < registeredSpecNewRecIndexToSrcRecMap.size(); i++) {
                Record registeredSpecRecord = registeredSpecNewRecIndexToSrcRecMap.get(i);
                String newRecId = registeredSpecRecordIds.get(i);
                registeredSpecIdToRecordMap.put(newRecId, registeredSpecRecord);
            }
        }

        return registeredSpecIdToRecordMap;
    }

    List<Record> getRegisteredSpecRecords(Map<String, List<String>> plantCountrySpecToLocationsMap, Map<String, QueryResult> specIdToQueryResultMap, Map<Integer, Record> registeredSpecNewRecIndexToSrcRecMap, List<String> fieldNameAndTypeList) {
        List<Record> registeredSpecRecords = VaultCollections.newList();

        Setting objectTypeSetting = CustomSettings.getCS(CustomSetting.CREATE_COUNTRY_REGISTRATION_EXT_ID, CustomSetting.OBJECT_TYPE);
        ArgumentUtils.validateNotNull(objectTypeSetting, "objectType custom setting is required");

        String objTypeName = objectTypeSetting.getValue();
        ArgumentUtils.validateNotNull(objTypeName, "Object type name missing from custom setting");

        String registeredSpecObjectTypeId = RecordTypeUtil.getRTId(Formulation.NAME, objTypeName);

        if (!ValidationUtils.isNullOrBlank(registeredSpecObjectTypeId)) {
            RecordService recordService = ServiceLocator.locate(RecordService.class);
            LocationService locationService = ServiceLocator.locate(LocationService.class);

            Map<String, String> locationIdToNameMap = locationService.getLocationIdToNameMap(plantCountrySpecToLocationsMap);

            int registeredSpecRecordIndex = 0;

            for (Map.Entry<String, List<String>> plantCountrySpecToLocationsMapEntry : plantCountrySpecToLocationsMap.entrySet()) {
                String specId = plantCountrySpecToLocationsMapEntry.getKey();
                List<String> locations = plantCountrySpecToLocationsMapEntry.getValue();
                for (String locationId : locations) {
                    Record registeredSpecRecord = recordService.newRecord(Formulation.NAME);
                    registeredSpecRecord.setValue(OBJECT_TYPE, registeredSpecObjectTypeId);
                    registeredSpecRecord.setValue(Formulation.Field.VERSION, BigDecimal.ONE);
                    registeredSpecRecord.setValue(Formulation.Field.LOCATION, locationId);

                    QueryResult plantCountrySpecQueryResult = specIdToQueryResultMap.get(specId);

                    fieldNameAndTypeList.forEach(fieldNameAndType -> {
                        Getter getter = new Getter(plantCountrySpecQueryResult);
                        String fieldName = RecordUtil.getFieldName(fieldNameAndType);
                        registeredSpecRecord.setValue(fieldName, RecordUtil.getValue(getter, fieldNameAndType));
                    });

                    String versionSourceTitle = plantCountrySpecQueryResult.getValue(Formulation.Relationship.VERSION_SOURCE_TITLE, ValueType.STRING);
                    String locationName = locationIdToNameMap.get(locationId);
                    registeredSpecRecord.setValue(Formulation.Field.TITLE, versionSourceTitle + "/" + locationName);

                    registeredSpecRecords.add(registeredSpecRecord);

                    registeredSpecNewRecIndexToSrcRecMap.put(registeredSpecRecordIndex, registeredSpecRecord);
                    registeredSpecRecordIndex++;
                }
            }
        }
        return registeredSpecRecords;
    }

    /**
     * Creates a map of plant country specification id to list of location ids for the given formulation country join records
     * @param formulationCountryJoinRecords list of formulation country join records for which create registered spec checkbox was updated
     * @return map of plant country specification id to list of location ids
     */
    Map<String, List<String>> getPlantCountrySpecToLocationsMap(List<Record> formulationCountryJoinRecords) {
        Map<String, List<String>> plantCountrySpecToLocationsMap = VaultCollections.newMap();
        formulationCountryJoinRecords.forEach(formulationCountryJoinRecord -> {
            String plantCountrySpecId = formulationCountryJoinRecord.getValue(FormulationCountryJoin.Field.SPEC, ValueType.STRING);
            String locationId = formulationCountryJoinRecord.getValue(FormulationCountryJoin.Field.LOCATION, ValueType.STRING);
            List<String> locations = plantCountrySpecToLocationsMap.computeIfAbsent(plantCountrySpecId, (k) -> VaultCollections.newList());
            locations.add(locationId);
        });
        return plantCountrySpecToLocationsMap;
    }
}