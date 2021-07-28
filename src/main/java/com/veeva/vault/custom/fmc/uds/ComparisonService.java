package com.veeva.vault.custom.fmc.uds;

import com.veeva.vault.sdk.api.core.UserDefinedService;
import com.veeva.vault.sdk.api.core.UserDefinedServiceInfo;
import com.veeva.vault.sdk.api.data.Record;

import java.util.List;

/**
 * Service that compares formulations by their composition and stores the results in comparison result objects
 */
@UserDefinedServiceInfo
public interface ComparisonService extends UserDefinedService {
    /**
     * Compares the pairs of formulations contained in each comparison record to each other
     * Persists comparison result records containing the results of the match related to each comparison record
     * @param comparisons comparison records
     */
    void compare(List<Record> comparisons);
}