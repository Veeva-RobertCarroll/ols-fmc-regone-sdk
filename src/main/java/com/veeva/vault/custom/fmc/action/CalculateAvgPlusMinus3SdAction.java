package com.veeva.vault.custom.fmc.action;

import com.veeva.vault.custom.common.utility.RecordTypeUtil;
import com.veeva.vault.custom.common.utility.RecordUtil;
import com.veeva.vault.custom.fmc.Constants.Composition;
import com.veeva.vault.custom.fmc.Constants.Formulation;
import com.veeva.vault.custom.fmc.uds.FiveBatchService;
import com.veeva.vault.custom.fmc.util.ValidationUtils;
import com.veeva.vault.sdk.api.action.RecordAction;
import com.veeva.vault.sdk.api.action.RecordActionContext;
import com.veeva.vault.sdk.api.action.RecordActionInfo;
import com.veeva.vault.sdk.api.action.Usage;
import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.data.Record;
import com.veeva.vault.sdk.api.data.RecordService;
import com.veeva.vault.sdk.api.query.QueryResult;

import java.util.List;
import java.util.Map;

import static com.veeva.vault.custom.fmc.Constants.Record.OBJECT_TYPE;

/**
 * Action to calculate average +/- 3 Standard Deviations of 5 Batch Data
 */
@RecordActionInfo(label = "Calculate Avg +/- 3SD for 5 Batch Data", object = Formulation.NAME, usages = { Usage.USER_ACTION, Usage.LIFECYCLE_ENTRY_ACTION })
public class CalculateAvgPlusMinus3SdAction implements RecordAction {

    @Override
    public boolean isExecutable(RecordActionContext context) {
        return true;
    }

    @Override
    public void execute(RecordActionContext context) {
        List<Record> fiveBatchRecords = context.getRecords();
        if (ValidationUtils.isNotEmpty(fiveBatchRecords)) {
            FiveBatchService fiveBatchService = ServiceLocator.locate(FiveBatchService.class);
            Map<String, Map<String, List<QueryResult>>> childIdToCompositionsByParentMap = fiveBatchService.getChildIdToCompositionsByParentMap(fiveBatchRecords);

            List<Record> compositionResults = VaultCollections.newList();
            RecordService recordService = ServiceLocator.locate(RecordService.class);
            String fiveBatchResultObjTypeId = RecordTypeUtil.getRTId(Composition.NAME, Composition.ObjectType.FIVE_BATCH);

            childIdToCompositionsByParentMap.forEach((fiveBatchId, childIdToCompositionsMap) -> {
                childIdToCompositionsMap.forEach((childId, fiveBatchResults) -> {
                    Record compositionResultRecord = recordService.newRecord(Composition.NAME);
                    fiveBatchService.calculateAvgPlusMinusThreeStandardDeviations(compositionResultRecord, fiveBatchResults);
                    compositionResultRecord.setValue(Composition.Field.CHILD, childId);
                    compositionResultRecord.setValue(Composition.Field.PARENT, fiveBatchId);
                    compositionResultRecord.setValue(OBJECT_TYPE, fiveBatchResultObjTypeId);
                    compositionResults.add(compositionResultRecord);
                });
            });

            RecordUtil.save(compositionResults);
        }
    }
}