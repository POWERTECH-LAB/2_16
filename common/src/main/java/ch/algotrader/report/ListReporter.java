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
package ch.algotrader.report;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;

/**
 * SuperCSV Writer that writes Maps to the specified CSV-File.
 *
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class ListReporter implements Report {

    private CellProcessor[] processor;
    private CsvListWriter writer;

    public ListReporter(File file, String[] header) throws IOException {

        this(file, header, null);
    }

    public ListReporter(File file, String[] header, CellProcessor[] processor) throws IOException {

        File parent = file.getParentFile();
        if (!parent.exists()) {
            FileUtils.forceMkdir(parent);
        }

        this.processor = processor;

        this.writer = new CsvListWriter(new FileWriter(file, false), CsvPreference.EXCEL_PREFERENCE);

        this.writer.writeHeader(header);

        ReportManager.registerReport(this);
    }

    public ListReporter(String fileName, String[] header) throws IOException {

        this(fileName, header, null);
    }

    public ListReporter(String fileName, String[] header, CellProcessor[] processor) throws IOException {

        this(new File("files" + File.separator + "report" + File.separator + fileName + ".csv"), header, processor);
    }

    public void write(List<?> row) throws IOException {

        if (this.processor != null) {
            this.writer.write(row, this.processor);
        } else {
            this.writer.write(row);
        }
    }

    public void write(Object... row) throws IOException {

        this.writer.write(row);
    }

    @Override
    public void close() throws IOException {

        this.writer.close();
    }
}
