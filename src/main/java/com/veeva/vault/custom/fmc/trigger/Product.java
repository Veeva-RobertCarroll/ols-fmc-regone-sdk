package com.veeva.vault.custom.fmc.trigger;

import com.veeva.vault.custom.fmc.uds.DeepCopyService;
import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.data.RecordEvent;
import com.veeva.vault.sdk.api.data.RecordTrigger;
import com.veeva.vault.sdk.api.data.RecordTriggerContext;
import com.veeva.vault.sdk.api.data.RecordTriggerInfo;

import static com.veeva.vault.custom.fmc.Constants.Formulation.NAME;

/**
 * Trigger to deep copy related composition records and populate version_source__v when record is created during copy operation
 */
@RecordTriggerInfo(object = NAME, events = { RecordEvent.BEFORE_INSERT, RecordEvent.AFTER_INSERT })
public class Product implements RecordTrigger {

    @Override
    public void execute(RecordTriggerContext recordTriggerContext) {
        DeepCopyService deepCopyService = ServiceLocator.locate(DeepCopyService.class);
        deepCopyService.deepCopy(recordTriggerContext, NAME);
    }
}