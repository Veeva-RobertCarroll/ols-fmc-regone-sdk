package com.veeva.vault.custom.fmc.uds;

import com.veeva.vault.sdk.api.core.UserDefinedService;
import com.veeva.vault.sdk.api.core.UserDefinedServiceInfo;
import com.veeva.vault.sdk.api.data.Record;

import java.util.List;

/**
 * Service for functionality related to Registered Specification formulation object type
 */
@UserDefinedServiceInfo
public interface RegisteredSpecificationService extends UserDefinedService {
    /**
     * Create registered specification records for each combination of plant country spec and location for a given version source formulation
     * @param updatedFormulationCountryJoinRecords list of formulation country join records for which create registered spec checkbox was updated
     */
    void createRegisteredSpecifications(List<Record> updatedFormulationCountryJoinRecords);
}