package com.veeva.vault.custom.fmc.uds;

import com.veeva.vault.sdk.api.core.UserDefinedService;
import com.veeva.vault.sdk.api.core.UserDefinedServiceInfo;
import com.veeva.vault.sdk.api.data.Record;
import com.veeva.vault.sdk.api.query.QueryResult;

import java.util.List;
import java.util.Map;

/**
 * Service for functionality related to five batch object type of formulation object
 */
@UserDefinedServiceInfo
public interface FiveBatchService extends UserDefinedService {

    /**
     * Queries compositions related to the formulation records by parentId and builds a map of childId of compositions to respective
     * composition query results wrapped in a map keyed by the new formulationIds created from the formulation records
     * @param formulationRecords formulation records
     * @return map of newly created parentId to map of childId to composition query results
     */
    Map<String, Map<String, List<QueryResult>>> getChildIdToCompositionsByParentMap(List<Record> formulationRecords);

    /**
     * Calculates the average +/- 3 standard deviations of the five batch results belonging to five batch data
     * Additionally sets default values like purpose and order fields
     * @param compositionResultRecord composition record where the average +/- 3SD is stored
     * @param fiveBatchResults composition records representing the five batch results used for the calculation
     */
    void calculateAvgPlusMinusThreeStandardDeviations(Record compositionResultRecord, List<QueryResult> fiveBatchResults);
}