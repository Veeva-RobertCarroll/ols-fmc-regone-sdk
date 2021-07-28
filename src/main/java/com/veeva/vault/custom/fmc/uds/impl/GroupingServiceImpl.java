package com.veeva.vault.custom.fmc.uds.impl;

import com.veeva.vault.custom.common.utility.RecordUtil;
import com.veeva.vault.custom.fmc.Constants.Grouping;
import com.veeva.vault.custom.fmc.Constants.Formulation;
import com.veeva.vault.custom.fmc.uds.GroupingService;
import com.veeva.vault.custom.fmc.util.ValidationUtils;
import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.UserDefinedServiceInfo;
import com.veeva.vault.sdk.api.core.ValueType;
import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.data.Record;
import com.veeva.vault.sdk.api.data.RecordService;
import com.veeva.vault.sdk.api.query.QueryResult;

import java.util.List;
import java.util.Map;

import static com.veeva.vault.custom.fmc.Constants.Record.ID;
import static com.veeva.vault.custom.fmc.Constants.Record.NAME;

/**
 * Implementation of the service containing functionality related to Grouping object
 */
@UserDefinedServiceInfo
public class GroupingServiceImpl implements GroupingService {

    @Override
    public Map<String, String> copyRegisteredSpecGroupings(Map<String, Record> registeredSpecIdToRecordMap, Map<String, List<QueryResult>> versionSourceIdToGroupingsMap) {
        Map<String, String> groupingSrcRecIdToNewRecIdMap = VaultCollections.newMap();
        Map<Integer, String> groupingNewRecIndexToSrcRecIdMap = VaultCollections.newMap();

        List<Record> groupings = getGroupingRecords(registeredSpecIdToRecordMap, versionSourceIdToGroupingsMap, groupingNewRecIndexToSrcRecIdMap);

        if (ValidationUtils.isNotEmpty(groupings)) {
            List<String> groupingRecordIds = RecordUtil.save(groupings);

            for (int i = 0; i < groupingNewRecIndexToSrcRecIdMap.size(); i++) {
                String srcRecId = groupingNewRecIndexToSrcRecIdMap.get(i);
                String newRecId = groupingRecordIds.get(i);
                groupingSrcRecIdToNewRecIdMap.put(srcRecId, newRecId);
            }
        }

        return groupingSrcRecIdToNewRecIdMap;
    }

    List<Record> getGroupingRecords(Map<String, Record> registeredSpecIdToRecordMap, Map<String, List<QueryResult>> versionSourceIdToGroupingsMap, Map<Integer, String> groupingNewRecIndexToSrcRecIdMap) {
        RecordService recordService = ServiceLocator.locate(RecordService.class);
        List<Record> groupings = VaultCollections.newList();
        int groupingRecordIndex = 0;
        for (Map.Entry<String, Record> registeredSpecIdToRecMapEntry : registeredSpecIdToRecordMap.entrySet()) {
            String registeredSpecId = registeredSpecIdToRecMapEntry.getKey();
            Record registeredSpecRecord = registeredSpecIdToRecMapEntry.getValue();
            String versionSourceId = registeredSpecRecord.getValue(Formulation.Field.VERSION_SOURCE, ValueType.STRING);
            List<QueryResult> groupingQueryResults = versionSourceIdToGroupingsMap.get(versionSourceId);
            for (QueryResult groupingQueryResult : groupingQueryResults) {
                Record grouping = recordService.newRecord(Grouping.NAME);
                grouping.setValue(NAME, groupingQueryResult.getValue(NAME, ValueType.STRING));
                grouping.setValue(Grouping.Field.FORMULATION, registeredSpecId);
                groupings.add(grouping);
                String groupingSrcRecId = groupingQueryResult.getValue(ID, ValueType.STRING);
                groupingNewRecIndexToSrcRecIdMap.put(groupingRecordIndex, groupingSrcRecId);
                groupingRecordIndex++;
            }
        }
        return groupings;
    }

}