package com.veeva.vault.custom.fmc.trigger;

import com.veeva.vault.custom.fmc.uds.ComparisonService;
import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.VaultCollectors;
import com.veeva.vault.sdk.api.data.Record;
import com.veeva.vault.sdk.api.data.RecordEvent;
import com.veeva.vault.sdk.api.data.RecordTrigger;
import com.veeva.vault.sdk.api.data.RecordTriggerContext;
import com.veeva.vault.sdk.api.data.RecordTriggerInfo;

import java.util.List;

import static com.veeva.vault.custom.fmc.Constants.Comparison.NAME;

@RecordTriggerInfo(object = NAME, events = { RecordEvent.AFTER_INSERT })
public class Comparison implements RecordTrigger {

    @Override
    public void execute(RecordTriggerContext recordTriggerContext) {
        List<Record> comparisons = recordTriggerContext.getRecordChanges().stream()
            .map(recordChange -> recordChange.getNew())
            .collect(VaultCollectors.toList());
        ComparisonService comparisonService = ServiceLocator.locate(ComparisonService.class);
        comparisonService.compare(comparisons);
    }
}