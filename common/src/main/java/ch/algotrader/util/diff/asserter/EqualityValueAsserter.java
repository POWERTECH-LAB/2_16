/***********************************************************************************
 * AlgoTrader Enterprise Trading Framework
 *
 * Copyright (C) 2014 AlgoTrader GmbH - All rights reserved
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
 * Badenerstrasse 16
 * 8004 Zurich
 ***********************************************************************************/
package ch.algotrader.util.diff.asserter;

import java.util.Objects;

/**
 * Asserter using object {@link Object#equals(Object) equality} to compare two values.
 *
 * @param <T> the (expected) value type
 */
public class EqualityValueAsserter implements ValueAsserter {

    /**
     * Singleton instance.
     */
    public static final EqualityValueAsserter INSTANCE = new EqualityValueAsserter();

    @Override
    public boolean equalValues(Object expectedValue, Object actualValue) {
        return Objects.equals(expectedValue, actualValue);
    }

    @Override
    public void assertValue(Object expectedValue, Object actualValue) {
        Assert.assertEquals("Values are not equal", expectedValue, actualValue);
    }

}
