package com.veeva.vault.custom.fmc.util;

import com.veeva.vault.sdk.api.core.UserDefinedClassInfo;

import java.util.Collection;

/**
 * Utility methods for performing validations.
 */
@UserDefinedClassInfo
public final class ValidationUtils {
    private ValidationUtils() {}

    /**
     * Checks whether specified string is null or empty.
     *
     * @param string string to be checked
     * @return true if collection is empty or null; false otherwise
     */
    public static boolean isNullOrBlank(final String string) {
        return string == null || string.trim().isEmpty();
    }

    /**
     * Checks whether specified collection is not empty.
     *
     * @param collection collection to be checked
     * @return true if collection is not empty; false otherwise
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return collection != null && !collection.isEmpty();
    }
}