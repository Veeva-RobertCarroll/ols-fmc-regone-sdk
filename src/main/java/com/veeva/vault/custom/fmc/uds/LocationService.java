package com.veeva.vault.custom.fmc.uds;

import com.veeva.vault.sdk.api.core.UserDefinedService;
import com.veeva.vault.sdk.api.core.UserDefinedServiceInfo;

import java.util.List;
import java.util.Map;

/**
 * Service for functionality related to Location object
 */
@UserDefinedServiceInfo
public interface LocationService extends UserDefinedService {
    /**
     * Retrieves a map of location record ids to location names
     * @param plantCountrySpecToLocationsMap map of plant country specification id to location ids
     * @return map of location record id to location name
     */
    Map<String, String> getLocationIdToNameMap(Map<String, List<String>> plantCountrySpecToLocationsMap);
}