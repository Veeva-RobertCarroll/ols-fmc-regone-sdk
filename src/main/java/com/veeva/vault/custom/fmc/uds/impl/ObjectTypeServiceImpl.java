package com.veeva.vault.custom.fmc.uds.impl;

import com.veeva.vault.custom.common.utility.Query;
import com.veeva.vault.custom.fmc.uds.ObjectTypeService;
import com.veeva.vault.sdk.api.core.UserDefinedServiceInfo;
import com.veeva.vault.sdk.api.core.ValueType;

import static com.veeva.vault.custom.fmc.Constants.Record.ID;

/**
 * Implementation of Helper Service class for object type query
 */
@UserDefinedServiceInfo
public class ObjectTypeServiceImpl implements ObjectTypeService {

    static final String OBJECT_TYPE_QUERY = "SELECT id from object_type__v WHERE object_name__v = '%s' and api_name__v = '%s'";

    @Override
    public String getObjectTypeIdFromApiName(String objectName, String objectTypeApiName) {
        String query = String.format(OBJECT_TYPE_QUERY, objectName, objectTypeApiName);
        return Query.queryStream(query).findFirst().map(queryResult -> queryResult.getValue(ID, ValueType.STRING)).orElse(null);
    }
}