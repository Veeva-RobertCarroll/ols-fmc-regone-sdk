package com.veeva.vault.custom.fmc.trigger;

import java.util.List;
import java.util.Map;

import com.veeva.vault.custom.common.utility.Log;
import com.veeva.vault.custom.common.utility.RecordUtil;
import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.ValueType;
import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.data.RecordEvent;
import com.veeva.vault.sdk.api.data.RecordService;
import com.veeva.vault.sdk.api.data.RecordTrigger;
import com.veeva.vault.sdk.api.data.RecordTriggerContext;
import com.veeva.vault.sdk.api.data.RecordTriggerInfo;
import com.veeva.vault.sdk.api.data.Record;
import com.veeva.vault.sdk.api.data.RecordChange;

@RecordTriggerInfo(object = "property__c", events = { RecordEvent.AFTER_UPDATE, RecordEvent.AFTER_INSERT })
public class Property implements RecordTrigger {

    @SuppressWarnings("unchecked")
    @Override
    public void execute(RecordTriggerContext rtc) {
        Map<String, Record> recordMap = VaultCollections.newMap();
        RecordService rs = ServiceLocator.locate(RecordService.class);
        for (RecordChange rc : rtc.getRecordChanges()) {
            Record r = rc.getNew();
            String prodFld = r.getValue("product_field__c", ValueType.STRING);
            Log.debug("prodFld:" + prodFld);
            if (prodFld != null && !prodFld.equals("")) {
                String prodId = r.getValue("formulation__c", ValueType.STRING);
                Record update = recordMap.computeIfAbsent(prodId, k -> rs.newRecordWithId("formulation__v", prodId));
                update.setValue(prodFld, r.getValue("value__c", ValueType.STRING));
            }
        }

        List<Record> saveList = VaultCollections.newList();
        saveList.addAll(recordMap.values());
        RecordUtil.save(saveList);
    }

}