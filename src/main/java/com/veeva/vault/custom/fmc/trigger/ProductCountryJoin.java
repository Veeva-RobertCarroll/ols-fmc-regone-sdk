package com.veeva.vault.custom.fmc.trigger;

import com.veeva.vault.custom.fmc.uds.RegisteredSpecificationService;
import com.veeva.vault.custom.fmc.util.ValidationUtils;
import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.ValueType;
import com.veeva.vault.sdk.api.core.VaultCollectors;
import com.veeva.vault.sdk.api.data.Record;
import com.veeva.vault.sdk.api.data.RecordEvent;
import com.veeva.vault.sdk.api.data.RecordTrigger;
import com.veeva.vault.sdk.api.data.RecordTriggerContext;
import com.veeva.vault.sdk.api.data.RecordTriggerInfo;

import java.util.List;

import static com.veeva.vault.custom.fmc.Constants.FormulationCountryJoin.Field.CREATE_COUNTRY_REGISTRATION;
import static com.veeva.vault.custom.fmc.Constants.FormulationCountryJoin.NAME;

/**
 * Trigger to create registered specs for each plant country join record related to a plant country spec
 */
@RecordTriggerInfo(object = NAME, events = { RecordEvent.AFTER_UPDATE })
public class ProductCountryJoin implements RecordTrigger {
    @Override
    public void execute(RecordTriggerContext recordTriggerContext) {

        List<Record> updatedFormulationCountryJoinRecords = recordTriggerContext.getRecordChanges().stream()
            .filter(recordChange -> recordChange != null)
            .filter(recordChange -> recordChange.getNew().getValue(CREATE_COUNTRY_REGISTRATION, ValueType.BOOLEAN))
            .filter(recordChange -> !recordChange.getOld().getValue(CREATE_COUNTRY_REGISTRATION, ValueType.BOOLEAN))
            .map(recordChange -> recordChange.getNew())
            .collect(VaultCollectors.toList());

        if (ValidationUtils.isNotEmpty(updatedFormulationCountryJoinRecords)) {
            RegisteredSpecificationService registeredSpecificationService = ServiceLocator.locate(RegisteredSpecificationService.class);
            registeredSpecificationService.createRegisteredSpecifications(updatedFormulationCountryJoinRecords);
        }

    }
}