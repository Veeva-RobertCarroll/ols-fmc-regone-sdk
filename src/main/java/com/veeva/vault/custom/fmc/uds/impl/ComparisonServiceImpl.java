package com.veeva.vault.custom.fmc.uds.impl;

import com.veeva.vault.custom.common.utility.CustomSettings;
import com.veeva.vault.custom.common.utility.Query;
import com.veeva.vault.custom.common.utility.RecordTypeUtil;
import com.veeva.vault.custom.common.utility.RecordUtil;
import com.veeva.vault.custom.common.utility.Setting;
import com.veeva.vault.custom.fmc.Constants.Comparison;
import com.veeva.vault.custom.fmc.Constants.ComparisonResult;
import com.veeva.vault.custom.fmc.Constants.Composition;
import com.veeva.vault.custom.fmc.Constants.CustomSetting;
import com.veeva.vault.custom.fmc.Constants.Formulation;
import com.veeva.vault.custom.fmc.udc.CompositionValue;
import com.veeva.vault.custom.fmc.uds.ComparisonService;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@UserDefinedServiceInfo
public class ComparisonServiceImpl implements ComparisonService {
    static final String QUERY = "SELECT %s, child__v, min__c, max__c, target__c, average_weight__c, average_3_sd__c FROM formulation_composition__v WHERE %s CONTAINS('%s') AND status__v = 'active__v'";
    static final String IS_NULL_CLAUSE = " AND %s = NULL";

    @Override
    public void compare(List<Record> comparisons) {
        List<Record> comparisonResults = VaultCollections.newList();
        Map<String, List<String>> objTypeToFormulationMap = VaultCollections.newMap();
        if (ValidationUtils.isNotEmpty(comparisons)) {
            comparisons.forEach(record -> {
                String formulation1Id = record.getValue(Comparison.Field.FORMULATION_1, ValueType.STRING);
                String formulation2Id = record.getValue(Comparison.Field.FORMULATION_2, ValueType.STRING);
                String formulation1ObjTypeId = record.getValue(Comparison.Field.FORMULATION_1_TYPE, ValueType.STRING);
                String formulation2ObjTypeId = record.getValue(Comparison.Field.FORMULATION_2_TYPE, ValueType.STRING);
                objTypeToFormulationMap.computeIfAbsent(formulation1ObjTypeId, key -> VaultCollections.newList()).add(formulation1Id);
                objTypeToFormulationMap.computeIfAbsent(formulation2ObjTypeId, key -> VaultCollections.newList()).add(formulation2Id);
            });
            Map<String, List<QueryResult>> formulationIdToCompositions = queryCompositions(objTypeToFormulationMap);
            comparisons.forEach(record -> {
                String comparisonRecId = record.getValue("id", ValueType.STRING);
                String formulation1Id = record.getValue(Comparison.Field.FORMULATION_1, ValueType.STRING);
                String formulation2Id = record.getValue(Comparison.Field.FORMULATION_2, ValueType.STRING);
                List<QueryResult> formulation1Compositions = formulationIdToCompositions.get(formulation1Id);
                List<QueryResult> formulation2Compositions = formulationIdToCompositions.get(formulation2Id);
                Map<String, QueryResult> childToFormulation1CompositionsMap = getChildToCompositionsMap(formulation1Compositions);
                Map<String, QueryResult> childToFormulation2CompositionsMap = getChildToCompositionsMap(formulation2Compositions);
                List<Record> comparisonResultsForFormulationPair = compareFormulations(comparisonRecId, childToFormulation1CompositionsMap, childToFormulation2CompositionsMap);
                comparisonResults.addAll(comparisonResultsForFormulationPair);
            });
        }
        if (comparisonResults.size() > 0) {
            RecordUtil.save(comparisonResults);
        }
    }

    Map<String, List<QueryResult>> queryCompositions(Map<String, List<String>> objTypeToFormulationMap) {
        Setting comparisonCustomSettings = CustomSettings.getCS(CustomSetting.COMPARISON_EXTERNAL_ID, CustomSetting.OT_FIELD_MAPPINGS);
        Map<String, List<QueryResult>> formulationIdToCompositions = VaultCollections.newMap();
        if (objTypeToFormulationMap != null && objTypeToFormulationMap.size() > 0) {
            objTypeToFormulationMap.forEach((objTypeId, formulationIds) -> {
                String objectTypeApiName = RecordTypeUtil.getRTName(Formulation.NAME, objTypeId);
                if (objectTypeApiName != null) {
                    Setting objTypeSetting = comparisonCustomSettings.getChild(objectTypeApiName);
                    if (objTypeSetting != null) {
                        String compositionReferenceFieldName = objTypeSetting.getValue();
                        String query = getQuery(objTypeSetting, compositionReferenceFieldName, formulationIds);
                        Query.queryStream(query)
                            .filter(queryResult -> queryResult != null)
                            .forEach(queryResult -> {
                                String formulationId = queryResult.getValue(compositionReferenceFieldName, ValueType.STRING);
                                formulationIdToCompositions.computeIfAbsent(formulationId, key -> VaultCollections.newList()).add(queryResult);
                        });
                    }
                }
            });
        }
        return formulationIdToCompositions;
    }

    String getQuery(Setting objTypeSetting, String compositionReferenceFieldName, List<String> formulationIds) {
        List<String> isNullFields = null;
        Setting isNullFieldSetting = objTypeSetting.getChild(CustomSetting.IS_NULL);
        if (isNullFieldSetting != null) {
            isNullFields = isNullFieldSetting.getNameSet().stream().collect(VaultCollectors.toList());
        }
        String query = String.format(QUERY, compositionReferenceFieldName, compositionReferenceFieldName, String.join("','", formulationIds));
        if (ValidationUtils.isNotEmpty(isNullFields)) {
            StringBuilder nullFieldsQuery = new StringBuilder();
            isNullFields.forEach(isNullField -> nullFieldsQuery.append(String.format(IS_NULL_CLAUSE, isNullField)));
            query = query + nullFieldsQuery.toString();
        }
        return query;
    }

    Map<String, QueryResult> getChildToCompositionsMap(List<QueryResult> compositions) {
        Map<String, QueryResult> childToCompositions = VaultCollections.newMap();
        if (ValidationUtils.isNotEmpty(compositions)) {
            compositions.forEach(queryResult -> {
                String childId = queryResult.getValue(Composition.Field.CHILD, ValueType.STRING);
                childToCompositions.put(childId, queryResult);
            });
        }
        return childToCompositions;
    }

    /**
     * Match formulation pair compositions by childId to create comparison result for each composition pair (formed by picking one from each formulation)
     * If a composition doesn't have a matching pair among the list of compositions belonging to other formulation pair then still create comparison result but with match result of false
     * @param comparisonRecId comparison record id used to persist comparison result records
     * @param formulation1Compositions compositions belonging to formulation1
     * @param formulation2Compositions compositions belonging to formulation2
     * @return list of comparison result records representing the result of the match
     */
    List<Record> compareFormulations(String comparisonRecId, Map<String, QueryResult> formulation1Compositions, Map<String, QueryResult> formulation2Compositions) {
        List<Record> comparisonResults = VaultCollections.newList();
        RecordService recordService = ServiceLocator.locate(RecordService.class);
        Iterator<Map.Entry<String, QueryResult>> formulation1CompositionsIterator = formulation1Compositions.entrySet().iterator();
        while (formulation1CompositionsIterator.hasNext()) {
            Map.Entry<String, QueryResult> entry = formulation1CompositionsIterator.next();
            String childId = entry.getKey();
            QueryResult formulation1CompositionQueryResult = entry.getValue();
            QueryResult formulation2CompositionQueryResult = formulation2Compositions.get(childId);
            Record comparisonResultRecord = recordService.newRecord(ComparisonResult.NAME);
            comparisonResultRecord.setValue(ComparisonResult.Field.COMPARISON, comparisonRecId);

            if (formulation2CompositionQueryResult != null) {
                compareCompositions(comparisonResultRecord, formulation1CompositionQueryResult, formulation2CompositionQueryResult, childId);
                formulation2Compositions.remove(childId);
            } else {
                //compositions in formulation1 that don't have a matching pair in formulation2
                compareCompositions(comparisonResultRecord, formulation1CompositionQueryResult, null, childId);
            }

            comparisonResults.add(comparisonResultRecord);
        }

        //compositions in formulation2 that don't have a matching pair in formulation1
        if (formulation2Compositions != null && formulation2Compositions.size() > 0) {
            formulation2Compositions.forEach((childId, value) -> {
                Record comparisonResultRecord = recordService.newRecord(ComparisonResult.NAME);
                comparisonResultRecord.setValue(ComparisonResult.Field.COMPARISON, comparisonRecId);
                compareCompositions(comparisonResultRecord, null, value, childId);
                comparisonResults.add(comparisonResultRecord);
            });
        }
        return comparisonResults;
    }

    void compareCompositions(Record comparisonResult, QueryResult composition1, QueryResult composition2, String childId) {
        CompositionValue value1 = getValue(composition1);
        CompositionValue value2 = getValue(composition2);
        boolean match = compareValues(value1, value2);

        comparisonResult.setValue(ComparisonResult.Field.FORMULATION, childId);
        comparisonResult.setValue(ComparisonResult.Field.VALUE_1, getComparisonResultValue(value1));
        comparisonResult.setValue(ComparisonResult.Field.VALUE_2, getComparisonResultValue(value2));
        comparisonResult.setValue(ComparisonResult.Field.MATCH, match);
    }

    CompositionValue getValue(QueryResult composition) {
        CompositionValue compositionValue = null;
        if (composition != null) {
            BigDecimal min = composition.getValue(Composition.Field.MIN, ValueType.NUMBER);
            BigDecimal max = composition.getValue(Composition.Field.MAX, ValueType.NUMBER);
            BigDecimal target = composition.getValue(Composition.Field.TARGET, ValueType.NUMBER);
            BigDecimal avgPercentWeight = composition.getValue(Composition.Field.AVERAGE_PERCENT_WEIGHT, ValueType.NUMBER);
            BigDecimal avgPlusMinus3Sd = composition.getValue(Composition.Field.AVERAGE_PLUS_MINUS_3_SD, ValueType.NUMBER);
            if (avgPercentWeight != null && avgPlusMinus3Sd != null) {
                if (avgPercentWeight.compareTo(avgPlusMinus3Sd) > 0) {
                    min = avgPlusMinus3Sd;
                    max = avgPercentWeight;
                } else {
                    min = avgPercentWeight;
                    max = avgPlusMinus3Sd;
                }
            }
            compositionValue = new CompositionValue(min, max, target);
        }
        return compositionValue;
    }

    String getComparisonResultValue(CompositionValue value) {
        if (value != null) {
            if (value.isRange()) {
                return value.getRangeValue();
            } else {
                return Optional.ofNullable(value.getTarget())
                    .map(targetVal -> targetVal.toString())
                    .orElse(null);
            }
        }
        return null;
    }

    boolean compareValues(CompositionValue value1, CompositionValue value2) {
        if (value1 == null && value2 == null) {
            return true;
        } else if (value1 != null && value2 != null) {
            if (value1.isRange() && value2.isRange()) {
                return Optional.ofNullable(value1.getRangeValue())
                    .map(rangeVal1 -> rangeVal1.equals(value2.getRangeValue()))
                    .orElse(false);
            } else if (value1.isRange()) {
                return compareRangeToTarget(value1, value2);
            } else if (value2.isRange()) {
                return compareRangeToTarget(value2, value1);
            } else if (value1.getTarget() != null && value2.getTarget() != null) {
                return value1.getTarget().compareTo(value2.getTarget()) == 0;
            } else {
                return value1.getTarget() == null && value2.getTarget() == null;
            }
        }
        return false;
    }

    boolean compareRangeToTarget(CompositionValue compositionRangeValue, CompositionValue compositionTargetValue) {
        if (compositionRangeValue != null && compositionTargetValue != null
            && compositionTargetValue.getTarget() != null
            && compositionRangeValue.getMin() != null
            && compositionRangeValue.getMax() != null) {
            //Test if target is within range
            return compositionTargetValue.getTarget().compareTo(compositionRangeValue.getMin()) >= 0
                && compositionTargetValue.getTarget().compareTo(compositionRangeValue.getMax()) <= 0;
        } else {
            return false;
        }
    }
}