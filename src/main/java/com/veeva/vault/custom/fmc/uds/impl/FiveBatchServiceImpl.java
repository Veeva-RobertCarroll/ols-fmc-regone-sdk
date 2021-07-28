package com.veeva.vault.custom.fmc.uds.impl;

import com.veeva.vault.custom.common.utility.Query;
import com.veeva.vault.custom.common.utility.RecordTypeUtil;
import com.veeva.vault.custom.common.utility.RecordUtil;
import com.veeva.vault.custom.fmc.Constants.Composition;
import com.veeva.vault.custom.fmc.Constants.Formulation;
import com.veeva.vault.custom.fmc.Constants.Purpose;
import com.veeva.vault.custom.fmc.uds.FiveBatchService;
import com.veeva.vault.custom.fmc.util.ValidationUtils;
import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.UserDefinedServiceInfo;
import com.veeva.vault.sdk.api.core.ValueType;
import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.core.VaultCollectors;
import com.veeva.vault.sdk.api.data.Record;
import com.veeva.vault.sdk.api.data.RecordService;
import com.veeva.vault.sdk.api.query.QueryResponse;
import com.veeva.vault.sdk.api.query.QueryResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.veeva.vault.custom.fmc.Constants.Record.ID;
import static com.veeva.vault.custom.fmc.Constants.Record.OBJECT_TYPE;

@UserDefinedServiceInfo
public class FiveBatchServiceImpl implements FiveBatchService {

    static final String QUERY = "SELECT id, title__v, version_source__v, version_source__vr.external_id__v, location__c, organization__c, (SELECT id, parent__v, purpose__c, order__c, target__c, child__v FROM child_compositions__vr WHERE status__v = 'active__v') FROM formulation__v WHERE id CONTAINS ('%s') AND status__v = 'active__v'";
    static final String NAME_SUFFIX = "-Result";

    @Override
    public Map<String, Map<String, List<QueryResult>>> getChildIdToCompositionsByParentMap(List<Record> formulationRecords) {
        Map<String, Map<String, List<QueryResult>>> childIdToCompositionsByParentMap = VaultCollections.newMap();
        List<Record> fiveBatchResults = VaultCollections.newList();

        List<String> formulationIds = formulationRecords.stream()
            .map(formulationRecord -> formulationRecord.getValue(ID, ValueType.STRING))
            .collect(VaultCollectors.toList());

        List<QueryResult> formulationQueryResults = Query.queryStream(String.format(QUERY, String.join(", ", formulationIds)))
            .filter(queryResult -> queryResult != null)
            .collect(VaultCollectors.toList());

        RecordService recordService = ServiceLocator.locate(RecordService.class);
        Map<String, String> srcRecIdToNewRecIdMap = VaultCollections.newMap();
        Map<Integer, String> newRecIndexToSrcRecIdMap = VaultCollections.newMap();
        int recordIndex = 0;

        String fiveBatchResultObjTypeId = RecordTypeUtil.getRTId(Formulation.NAME, Formulation.ObjectType.FIVE_BATCH_RESULT);

        for (QueryResult queryResult : formulationQueryResults) {
            Record fiveBatchResult = recordService.newRecord(Formulation.NAME);
            String srcRecId = queryResult.getValue(ID, ValueType.STRING);
            String name = queryResult.getValue(Formulation.Field.TITLE, ValueType.STRING) + NAME_SUFFIX;
            fiveBatchResult.setValue(Formulation.Field.TITLE, name);
            fiveBatchResult.setValue(Formulation.Field.EXTERNAL_ID, queryResult.getValue(Formulation.Relationship.VERSION_SOURCE_EXTERNAL_ID, ValueType.STRING) + "/" + name);
            fiveBatchResult.setValue(Formulation.Field.VERSION_SOURCE, queryResult.getValue(Formulation.Field.VERSION_SOURCE, ValueType.STRING));
            fiveBatchResult.setValue(Formulation.Field.LOCATION, queryResult.getValue(Formulation.Field.LOCATION, ValueType.STRING));
            fiveBatchResult.setValue(Formulation.Field.ORGANIZATION, queryResult.getValue(Formulation.Field.ORGANIZATION, ValueType.STRING));
            fiveBatchResult.setValue(Formulation.Field.PREVIOUS_VERSION, srcRecId);
            fiveBatchResult.setValue(OBJECT_TYPE, fiveBatchResultObjTypeId);
            fiveBatchResults.add(fiveBatchResult);
            newRecIndexToSrcRecIdMap.put(recordIndex, srcRecId);
            recordIndex++;
        }

        if (ValidationUtils.isNotEmpty(fiveBatchResults)) {
            List<String> fiveBatchResultIds = RecordUtil.save(fiveBatchResults);
            for (int i = 0; i < newRecIndexToSrcRecIdMap.size(); i++) {
                String srcRecId = newRecIndexToSrcRecIdMap.get(i);
                String newRecId = fiveBatchResultIds.get(i);
                srcRecIdToNewRecIdMap.put(srcRecId, newRecId);
            }

            formulationQueryResults.forEach(formulationQueryResult -> {
                String srcRecId = formulationQueryResult.getValue(ID, ValueType.STRING);
                QueryResponse subQueryResponse = formulationQueryResult.getSubqueryResponse(Formulation.Relationship.COMPOSITIONS);
                subQueryResponse.streamResults().forEach(subQueryResult -> {
                    String childId = subQueryResult.getValue(Composition.Field.CHILD, ValueType.STRING);
                    Map<String, List<QueryResult>> childIdToCompositionsMap = childIdToCompositionsByParentMap
                        .computeIfAbsent(srcRecIdToNewRecIdMap.get(srcRecId), (key) -> VaultCollections.newMap());
                    List<QueryResult> compositionQueryResults = childIdToCompositionsMap.computeIfAbsent(childId, (key) -> VaultCollections.newList());
                    compositionQueryResults.add(subQueryResult);
                });
            });
        }

        return childIdToCompositionsByParentMap;
    }

    @Override
    public void calculateAvgPlusMinusThreeStandardDeviations(Record compositionResultRecord, List<QueryResult> fiveBatchResults) {
        if (!ValidationUtils.isNotEmpty(fiveBatchResults)) {
            return;
        }

        List<BigDecimal> targetList = fiveBatchResults.stream()
            .filter(value -> value != null)
            .map(queryResult -> queryResult.getValue(Composition.Field.TARGET, ValueType.NUMBER))
            .collect(VaultCollectors.toList());

        if (!ValidationUtils.isNotEmpty(targetList)) {
            return;
        }

        double sum = 0;
        for (BigDecimal target : targetList) {
            if (target != null) {
                sum += target.doubleValue();
            }
        }
        double mean = sum / targetList.size();

        double deviations = 0;
        for (BigDecimal target : targetList) {
            if (target != null) {
                deviations += Math.pow(target.doubleValue() - mean, 2);
            }
        }
        double variance = deviations / (targetList.size() - 1); //(n - 1) for calculating sample standard deviation

        BigDecimal standardDeviation = new BigDecimal(Math.sqrt(variance));
        BigDecimal average = new BigDecimal(mean).setScale(2, BigDecimal.ROUND_DOWN);
        BigDecimal threeStandardDeviations = new BigDecimal(3).multiply(standardDeviation);

        boolean activeIngredient = fiveBatchResults.stream()
            .findFirst()
            .map(queryResult -> queryResult.getValue(Composition.Field.PURPOSE, ValueType.PICKLIST_VALUES))
            .filter(purposePickListValues -> purposePickListValues != null)
            .flatMap(purposePickListValues -> purposePickListValues.stream().findFirst())
            .map(purposeVal -> Purpose.PicklistValue.ACTIVE_INGREDIENT.equals(purposeVal) || Purpose.PicklistValue.ACT_AGENT.equals(purposeVal))
            .orElse(false);

        //If the purpose of the composition is active ingredient then subtract otherwise add the average to 3 standard deviations
        BigDecimal avgPlusMinus3Sds = activeIngredient ? average.subtract(threeStandardDeviations) : average.add(threeStandardDeviations);

        compositionResultRecord.setValue(Composition.Field.AVERAGE_PERCENT_WEIGHT, average);
        compositionResultRecord.setValue(Composition.Field.AVERAGE_PLUS_MINUS_3_SD, avgPlusMinus3Sds.setScale(2, BigDecimal.ROUND_HALF_UP));
        setDefaultValues(compositionResultRecord, fiveBatchResults);
    }

    void setDefaultValues(Record compositionResultRecord, List<QueryResult> fiveBatchResults) {
       fiveBatchResults.stream()
            .findFirst()
            .map(queryResult -> queryResult.getValue(Composition.Field.PURPOSE, ValueType.PICKLIST_VALUES))
            .ifPresent(purposePickListValues -> {
                compositionResultRecord.setValue(Composition.Field.PURPOSE, purposePickListValues);
            });
        fiveBatchResults.stream()
            .findFirst()
            .map(queryResult -> queryResult.getValue(Composition.Field.ORDER, ValueType.NUMBER))
            .ifPresent(order -> {
                compositionResultRecord.setValue(Composition.Field.ORDER, order);
            });
    }
}