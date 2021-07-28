package com.veeva.vault.custom.fmc.uds;

import com.veeva.vault.sdk.api.core.UserDefinedService;
import com.veeva.vault.sdk.api.core.UserDefinedServiceInfo;
import com.veeva.vault.sdk.api.data.RecordTriggerContext;

/**
 * Service to copy related records based on Custom Settings configuration object named "Deep Copy"
 */
@UserDefinedServiceInfo
public interface DeepCopyService extends UserDefinedService {
    /**
     * Copy related records of an object in the context of trigger execution based on custom settings
     * @param recordTriggerContext context information for the trigger
     * @param objectName name of the object that's being copied
     */
    void deepCopy(RecordTriggerContext recordTriggerContext, String objectName);
}