/***********************************************************************************
 * AlgoTrader Enterprise Trading Framework
 *
 * Copyright (C) 2015 AlgoTrader GmbH - All rights reserved
 *
 * All information contained herein is, and remains the property of AlgoTrader GmbH.
 * The intellectual and technical concepts contained herein are proprietary to
 * AlgoTrader GmbH. Modification, translation, reverse engineering, decompilation,
 * disassembly or reproduction of this material is strictly forbidden unless prior
 * written permission is obtained from AlgoTrader GmbH
 *
 * Fur detailed terms and conditions consult the file LICENSE.txt or contact
 *
 * AlgoTrader GmbH
 * Aeschstrasse 6
 * 8834 Schindellegi
 ***********************************************************************************/
package ch.algotrader.util;

/**
 * Provides conversion methods for Base-10 numbers to and from other Bases.
 *
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 */
public class BaseConverterUtil {

    private static final String baseDigits = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    public static String toBase36(int decimalNumber) {
        return fromDecimalToOtherBase(36, decimalNumber);
    }

    public static int fromBase36(String base36Number) {
        return fromOtherBaseToDecimal(36, base36Number);
    }

    private static String fromDecimalToOtherBase(int base, int decimalNumber) {
        String tempVal = decimalNumber == 0 ? "0" : "";
        int mod = 0;

        while (decimalNumber != 0) {
            mod = decimalNumber % base;
            tempVal = baseDigits.substring(mod, mod + 1) + tempVal;
            decimalNumber = decimalNumber / base;
        }

        return tempVal;
    }

    private static int fromOtherBaseToDecimal(int base, String number) {
        int iterator = number.length();
        int returnValue = 0;
        int multiplier = 1;

        while (iterator > 0) {
            returnValue = returnValue + (baseDigits.indexOf(number.substring(iterator - 1, iterator)) * multiplier);
            multiplier = multiplier * base;
            --iterator;
        }
        return returnValue;
    }
}
