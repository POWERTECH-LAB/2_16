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
package ch.algotrader.util.io;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.Date;

import org.supercsv.cellprocessor.CellProcessorAdaptor;
import org.supercsv.cellprocessor.ParseBigDecimal;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.Token;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvBeanReader;
import org.supercsv.prefs.CsvPreference;
import org.supercsv.util.CsvContext;

import ch.algotrader.ServiceLocator;
import ch.algotrader.entity.marketData.Tick;
import ch.algotrader.entity.marketData.TickImpl;

/**
 * SuperCSV Reader that reads {@link Tick Ticks} from the specified CSV-File.
 *
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class CsvTickReader {

    private static String dataSet = ServiceLocator.instance().getConfiguration().getDataSet();

    //@formatter:off
    private static CellProcessor[] processor = new CellProcessor[] {
        new ParseDate(),
        new Token("", new BigDecimal(0), new ParseBigDecimal()),
        new ParseDate(),
        new ParseInt(),
        new ParseInt(),
        new ParseBigDecimal(),
        new ParseBigDecimal(),
        new ParseInt()
    };
    //@formatter:on

    private String[] header;
    private CsvBeanReader reader;

    public CsvTickReader(File file) throws IOException {

        Reader inFile = new FileReader(file);
        this.reader = new CsvBeanReader(inFile, CsvPreference.EXCEL_PREFERENCE);
        this.header = this.reader.getHeader(true);
    }

    public CsvTickReader(String fileName) throws IOException {

        this(new File("files" + File.separator + "tickdata" + File.separator + dataSet + File.separator + fileName + ".csv"));
    }

    private static class ParseDate extends CellProcessorAdaptor {

        public ParseDate() {
            super();
        }

        @Override
        public Object execute(final Object value, final CsvContext context) throws NumberFormatException {

            Date date;
            if (value == null || "".equals(value)) {
                date = null;
            } else {
                date = new Date(Long.parseLong((String) value));
            }

            return this.next.execute(date, context);
        }
    }

    public Tick readTick() throws IOException {

        Tick tick;
        if ((tick = this.reader.read(TickImpl.class, this.header, processor)) != null) {
            return tick;
        } else {
            this.reader.close();
            return null;
        }
    }
}
