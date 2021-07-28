package com.veeva.vault.custom.fmc.action;

import com.veeva.vault.sdk.api.action.RecordAction;
import com.veeva.vault.sdk.api.action.RecordActionContext;
import com.veeva.vault.sdk.api.action.RecordActionInfo;
import com.veeva.vault.sdk.api.action.Usage;

/**
 * Action on custom product has been disabled
 */
@RecordActionInfo(label = "Calculate Avg +/- 3SD", object = "product__c", usages = { Usage.USER_ACTION, Usage.LIFECYCLE_ENTRY_ACTION })
public class Product implements RecordAction {

    @Override
    public boolean isExecutable(RecordActionContext context) {
        return false;
    }

    @Override
    public void execute(RecordActionContext context) {
        //Disabled action
    }
}