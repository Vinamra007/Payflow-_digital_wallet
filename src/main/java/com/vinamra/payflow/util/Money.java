package com.vinamra.payflow.util;

import com.vinamra.payflow.exception.InvalidAmountException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Money is stored internally as a long number of minor units (paise).
 * Using long (never double) avoids floating point rounding errors.
 */
public final class Money {
    private Money() {
    }

    /** Converts a decimal amount such as 250.50 into minor units (25050). */
    public static long toMinor(BigDecimal amount) {
        if (amount == null) {
            throw new InvalidAmountException("amount is required");
        }
        try {
            return amount.setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact();
        } catch (ArithmeticException e) {
            throw new InvalidAmountException("amount must have at most 2 decimal places and be within range");
        }
    }

    public static BigDecimal toDecimal(long minor) {
        return BigDecimal.valueOf(minor, 2);
    }
}
