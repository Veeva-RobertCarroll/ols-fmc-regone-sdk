package com.veeva.vault.custom.fmc.uds;

import com.veeva.vault.sdk.api.core.UserDefinedService;
import com.veeva.vault.sdk.api.core.UserDefinedServiceInfo;
import com.veeva.vault.sdk.api.data.Record;
import com.veeva.vault.sdk.api.query.QueryResult;

import java.util.List;
import java.util.Map;

/**
 * Service for functionality related to Composition object
 */
@UserDefinedServiceInfo
public interface CompositionService extends UserDefinedService {
    /**
     * Copies plant country spec compositions to registered specs
     * @param registeredSpecIdToRecordMap map of registered spec record id to record
     * @param versionSourceIdToCompositionsMap map of plant country spec version source id to composition query results
     * @param groupingSrcRecIdToNewRecIdMap map of plant country spec (source) grouping record id to copied registered spec grouping id
     */
    void copyRegisteredSpecCompositions(Map<String, Record> registeredSpecIdToRecordMap, Map<String, List<QueryResult>> versionSourceIdToCompositionsMap, Map<String, String> groupingSrcRecIdToNewRecIdMap);
}