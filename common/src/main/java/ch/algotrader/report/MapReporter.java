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

package ch.algotrader.report;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvMapWriter;
import org.supercsv.prefs.CsvPreference;

/**
 * SuperCSV Writer that writes Maps to the specified CSV-File.
 *
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 */
public class MapReporter extends Report {

    private final String[] header;
    private final CellProcessor[] processor;
    private final CsvMapWriter writer;

    public MapReporter(File file, String[] header) {

        this(file, header, null);
    }

    protected MapReporter(File file, String[] header, CellProcessor[] processor) {

        try {
            File parent = file.getParentFile();
            if (!parent.exists()) {
                FileUtils.forceMkdir(parent);
            }

            this.header = header;
            this.processor = processor;

            this.writer = new CsvMapWriter(new FileWriter(file, false), CsvPreference.EXCEL_PREFERENCE);

            this.writer.writeHeader(header);

            ReportManager.registerReport(this);
        } catch (IOException e) {
            throw new ReportException(e);
        }
    }

    public void write(Map<String, ?> row) {

        try {
            if (this.processor != null) {
                this.writer.write(row, this.header, this.processor);
            } else {
                this.writer.write(row, this.header);
            }
        } catch (IOException e) {
            throw new ReportException(e);
        }
    }

    public void writeAndFlush(Map<String, ?> row) {

        try {
            if (this.processor != null) {
                this.writer.write(row, this.header, this.processor);
            } else {
                this.writer.write(row, this.header);
            }
            this.writer.flush();
        } catch (IOException e) {
            throw new ReportException(e);
        }
    }

    @Override
    public void close() {

        try {
            this.writer.close();
        } catch (IOException e) {
            throw new ReportException(e);
        }
    }
}
