package com.veeva.vault.custom.common.udc;

import java.util.Iterator;
import java.util.Map;
import java.util.Spliterator;
import java.util.function.Consumer;

import com.veeva.vault.sdk.api.core.RequestContextValue;
import com.veeva.vault.sdk.api.core.UserDefinedClassInfo;
import com.veeva.vault.sdk.api.core.VaultCollections;

@UserDefinedClassInfo(name="vps_my_string_set__c")
public class MyStringSet implements RequestContextValue, Iterable<String> {
        private Map<String, String> backingMap = VaultCollections.newMap();
        public void add(String value) {
            backingMap.put(value, value);
        }
        @Override
        public Iterator<String> iterator() {
            return backingMap.keySet().iterator();
        }
        @Override
        public void forEach(Consumer action) {
            backingMap.keySet().forEach(action);
        }
        @Override
        public Spliterator<String> spliterator() {
            return backingMap.keySet().spliterator();
        }
        public void addAll(MyStringSet kids) {
            this.backingMap.putAll(kids.backingMap);
        }
    }