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
package ch.algotrader.enumeration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Direction of a {@link ch.algotrader.entity.Position Position}. Either {@code LONG}, {@code
 * SHORT} or {@code FLAT}
 */
public enum Direction {

    LONG(1), SHORT(-1), FLAT(0);

    private static final long serialVersionUID = -2003135513481914597L;

    private final int enumValue;

    /**
     * The constructor with enumeration literal value allowing
     * super classes to access it.
     */
    private Direction(int value) {

        this.enumValue = value;
    }

    /**
     * Gets the underlying value of this type safe enumeration.
     * This method is necessary to comply with DaoBase implementation.
     * @return The name of this literal.
     */
    public int getValue() {

        return this.enumValue;
    }

    /**
     * Returns an element of the enumeration by its value.
     *
     * @param value the value.
     * @return enumeration element
     */
    public static Direction fromValue(int value) {

        Direction instance = MAP_BY_VALUE.get(value);
        if (instance == null) {
            throw new IllegalArgumentException("Invalid value '" + value + "'");
        }
        return instance;
    }

    private static final Map<Integer, Direction> MAP_BY_VALUE;
    static {
        HashMap<Integer, Direction> map = new HashMap<>();
        for (Direction instance: Direction.values()) {

            map.put(instance.getValue(), instance);
        }
        MAP_BY_VALUE = new ConcurrentHashMap<>(map);
    }

}
