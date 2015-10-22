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
package ch.algotrader.esper.io;

import java.io.File;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.espertech.esperio.AdapterInputSource;
import com.espertech.esperio.csv.CSVInputAdapterSpec;

/**
 * A {@link CSVInputAdapterSpec} used to input {@link ch.algotrader.entity.marketData.Tick Ticks}.
 * Will use {@code dateTime} as {@code timestampColumn}.
 *
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 */
public class CsvTickInputAdapterSpec extends CSVInputAdapterSpec {

    private final File file;

    public CsvTickInputAdapterSpec(File file) {

        super(new AdapterInputSource(file), "RawTickVO");

        this.file = file;

        //@formatter:off
        String[] tickPropertyOrder = new String[] {
                "dateTime",
                "last",
                "lastDateTime",
                "volBid",
                "volAsk",
                "bid",
                "ask",
                "vol",
                "security"};
        //@formatter:on

        setPropertyOrder(tickPropertyOrder);

        Map<String, Object> tickPropertyTypes = new HashMap<>();

        tickPropertyTypes.put("dateTime", Date.class);
        tickPropertyTypes.put("last", BigDecimal.class);
        tickPropertyTypes.put("lastDateTime", Date.class);
        tickPropertyTypes.put("volBid", int.class);
        tickPropertyTypes.put("volAsk", int.class);
        tickPropertyTypes.put("bid", BigDecimal.class);
        tickPropertyTypes.put("ask", BigDecimal.class);
        tickPropertyTypes.put("vol", int.class);
        tickPropertyTypes.put("security", String.class);

        setPropertyTypes(tickPropertyTypes);

        setTimestampColumn("dateTime");

        setUsingExternalTimer(true);
    }

    public File getFile() {

        return this.file;
    }
}
