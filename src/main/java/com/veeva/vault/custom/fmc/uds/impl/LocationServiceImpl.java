package com.veeva.vault.custom.fmc.uds.impl;

import com.veeva.vault.custom.common.utility.Query;
import com.veeva.vault.custom.fmc.uds.LocationService;
import com.veeva.vault.sdk.api.core.UserDefinedServiceInfo;
import com.veeva.vault.sdk.api.core.ValueType;
import com.veeva.vault.sdk.api.core.VaultCollectors;

import java.util.List;
import java.util.Map;

import static com.veeva.vault.custom.fmc.Constants.Record.ID;
import static com.veeva.vault.custom.fmc.Constants.Record.NAME;

/**
 * Implementation of the service containing functionality related to Location object
 */
@UserDefinedServiceInfo
public class LocationServiceImpl implements LocationService {

    static final String LOCATION_QUERY = "SELECT id, name__v from country__v WHERE id CONTAINS('%s')";

    @Override
    public Map<String, String> getLocationIdToNameMap(Map<String, List<String>> plantCountrySpecToLocationsMap) {
        List<String> locationIds = plantCountrySpecToLocationsMap.values().stream()
            .flatMap(locations -> locations.stream())
            .collect(VaultCollectors.toList());
        String locationQuery = String.format(LOCATION_QUERY, String.join("','", locationIds));
        Map<String, String> locationIdToNameMap = Query.queryStream(locationQuery)
            .collect(VaultCollectors.toMap(
                locationQueryResult -> locationQueryResult.getValue(ID, ValueType.STRING),
                locationQueryResult -> locationQueryResult.getValue(NAME, ValueType.STRING)
            ));
        return locationIdToNameMap;
    }
}