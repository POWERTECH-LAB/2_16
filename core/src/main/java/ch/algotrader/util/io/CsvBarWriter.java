/***********************************************************************************
 * AlgoTrader Enterprise Trading Framework
 *
 * Copyright (C) 2013 Flury Trading - All rights reserved
 *
 * All information contained herein is, and remains the property of Flury Trading.
 * The intellectual and technical concepts contained herein are proprietary to
 * Flury Trading. Modification, translation, reverse engineering, decompilation,
 * disassembly or reproduction of this material is strictly forbidden unless prior
 * written permission is obtained from Flury Trading
 *
 * Fur detailed terms and conditions consult the file LICENSE.txt or contact
 *
 * Flury Trading
 * Badenerstrasse 16
 * 8004 Zurich
 ***********************************************************************************/
package ch.algotrader.util.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import org.supercsv.cellprocessor.CellProcessorAdaptor;
import org.supercsv.cellprocessor.ConvertNullTo;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.exception.SuperCSVException;
import org.supercsv.exception.SuperCSVReflectionException;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.prefs.CsvPreference;
import org.supercsv.util.CSVContext;

import ch.algotrader.ServiceLocator;
import ch.algotrader.entity.marketData.Bar;

/**
 * SuperCSV Writer that writes {@link Bar Bars} to the specified CSV-File.
 *
 * @author <a href="mailto:andyflury@gmail.com">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class CsvBarWriter {

    //@formatter:off
    private static String[] header = new String[] {
        "dateTime",
        "open",
        "high",
        "low",
        "close",
        "vol",
        "openIntrest",
        "settlement"
    };

    private static CellProcessor[] processor = new CellProcessor[] {
        new DateConverter(), //dateTime
        null, //open
        null, //high
        null, //low
        null, //close
        null, //vol
        null, //openIntrest
        new ConvertNullTo("") //settlement
    };
    //@formatter:on

    private static String dataSet = ServiceLocator.instance().getConfiguration().getDataSet();

    private CsvBeanWriter writer;

    public CsvBarWriter(File file) throws SuperCSVException, IOException {

        boolean exists = file.exists();

        this.writer = new CsvBeanWriter(new FileWriter(file, true), CsvPreference.EXCEL_PREFERENCE);

        if (!exists) {
            this.writer.writeHeader(header);
        }
    }

    public CsvBarWriter(String fileName) throws SuperCSVException, IOException {

        this(new File("files" + File.separator + "bardata" + File.separator + dataSet + File.separator + fileName + ".csv"));
    }

    private static class DateConverter extends CellProcessorAdaptor {

        public DateConverter() {
            super();
        }

        @Override
        public Object execute(final Object value, final CSVContext context) throws NumberFormatException {
            if (value == null) {
                return "";
            }
            final Date date = (Date) value;
            Long result = Long.valueOf(date.getTime());
            return this.next.execute(result, context);
        }
    }

    public void write(Bar bar) throws SuperCSVReflectionException, IOException {

        this.writer.write(bar, header, processor);
    }

    public void close() throws IOException {

        this.writer.close();
    }
}
