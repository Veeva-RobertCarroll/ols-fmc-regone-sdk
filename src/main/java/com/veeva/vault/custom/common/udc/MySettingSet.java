package com.veeva.vault.custom.common.udc;

import java.util.Iterator;
import java.util.Map;
import java.util.Spliterator;
import java.util.function.Consumer;

import com.veeva.vault.sdk.api.core.RequestContextValue;
import com.veeva.vault.sdk.api.core.UserDefinedClassInfo;
import com.veeva.vault.sdk.api.core.VaultCollections;

@UserDefinedClassInfo(name="vps_my_setting_set__c")
public class MySettingSet implements RequestContextValue, Iterable<Setting> {
    private Map<Setting, Setting> backingMap = VaultCollections.newMap();
    public void add(Setting value) {
        backingMap.put(value, value);
    }
    @Override
    public Iterator<Setting> iterator() {
        return backingMap.keySet().iterator();
    }
    @Override
    public void forEach(Consumer action) {
        backingMap.keySet().forEach(action);
    }
    @Override
    public Spliterator<Setting> spliterator() {
        return backingMap.keySet().spliterator();
    }
    public void addAll(MySettingSet kids) {
        this.backingMap.putAll(kids.backingMap);
    }
}