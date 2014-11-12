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
package ch.algotrader.util.diff.value;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps a string to another string. Can be used to map from one domain model to another.
 */
public class MappedStringAsserter extends AbstractValueAsserter<String> {

    private Map<String, String> map;

    public MappedStringAsserter(String... mappedStrings) {
        this(toMap(mappedStrings));
    }
    public MappedStringAsserter(Map<String, String> map) {
        super(String.class);
        this.map = map;
    }
    private static Map<String, String> toMap(String... mappedStrings) {
        final Map<String, String> map = new LinkedHashMap<String, String>();
        for (final String mappedString : mappedStrings) {
            final int colonIndex = mappedString.indexOf(':');
            if (colonIndex >= 0) {
                map.put(mappedString.substring(0, colonIndex), mappedString.substring(colonIndex + 1));
            } else {
                throw new IllegalArgumentException("colon ':' missing in mappedStrings value: " + mappedString);
            }
        }
        return map;
    }

    @Override
    public String convert(String column, String value) {
        if (value == null) {
            return null;
        }
        final String mapped = map.get(value);
        if (mapped == null) {
            throw new IllegalArgumentException("value <" + value + "> from column '" + column + "' missing in map: " + map);
        }
        return mapped;
    }
}
