package com.veeva.vault.custom.fmc.uds;

import com.veeva.vault.sdk.api.core.UserDefinedService;
import com.veeva.vault.sdk.api.core.UserDefinedServiceInfo;

/**
 * Helper Service class for object type query
 */
@UserDefinedServiceInfo
public interface ObjectTypeService extends UserDefinedService {
    /**
     * Gets the object type id for the provided object name and object type name.
     *
     * @param objectName the object name
     * @param objectTypeApiName the api name of the object type
     * @return the type id
     */
    String getObjectTypeIdFromApiName(String objectName, String objectTypeApiName);
}