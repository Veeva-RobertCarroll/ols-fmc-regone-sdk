package com.veeva.vault.custom.fmc.util;

import com.veeva.vault.sdk.api.core.RollbackException;
import com.veeva.vault.sdk.api.core.UserDefinedClassInfo;

import java.util.Collection;

/**
 * Helper methods for validating arguments. Methods will check a condition and throw if not met.
 */
@UserDefinedClassInfo
public final class ArgumentUtils {

    /**
     * Validate the given Object value.
     * @param value Object value to validate. Must be non-null.
     * @param errMsg Message to throw in IllegalArgumentException if conditions aren't met.
     */
    public static void validateNotNull(final Object value, final String errMsg) {
        checkArgument(value != null, errMsg);
    }

    /**
     * Validate the given collection value to not be null and empty.
     * @param value Object value to validate. Must be non-null and non-empty.
     * @param errMsg Message to throw in IllegalArgumentException if conditions aren't met.
     */
    public static void validateNotNullAndEmptyCollection(final Collection value, final String errMsg) {
        checkArgument(value != null && !value.isEmpty(), errMsg);
    }

    /**
     * Throw exception if not valid.
     * @param valid a boolean value to determine whether to throw an exception or not.
     * @param errMsg the error message to throw when not valid.
     */
    public static void checkArgument(boolean valid, String errMsg) {
        if (!valid) {
            throw new RollbackException("Argument not valid", errMsg);
        }
    }

    private ArgumentUtils() {}
}