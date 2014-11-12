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
package ch.algotrader.util.diff.reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ch.algotrader.util.diff.define.CsvColumn;
import ch.algotrader.util.diff.define.CsvDefinition;

/**
 * Reads directly from a file using a {@link BufferedReader}. This is usually
 * the inner-most reader in a nested reader structure.
 */
public class DefaultCsvReader implements CsvReader {

    private final CsvDefinition csvDefinition;
    private final File file;
    private final BufferedReader bufferedReader;
    private int lineIndex;

    public DefaultCsvReader(CsvDefinition csvDefinition, File file) throws FileNotFoundException {
        this.csvDefinition = Objects.requireNonNull(csvDefinition, "csvDefinition cannot be null");
        this.file = Objects.requireNonNull(file, "file cannot be null");
        this.bufferedReader = new BufferedReader(new FileReader(file));
        this.lineIndex = 0;
    }

    public CsvDefinition getCsvDefinition() {
        return csvDefinition;
    }
    public File getFile() {
        return file;
    }
    public BufferedReader getReader() {
        return bufferedReader;
    }
    public int getLine() {
        return lineIndex;
    }

    public CsvLine readLine() throws IOException {
        if (lineIndex == 0 && csvDefinition.isSkipHeaderLine()) {
            bufferedReader.readLine();
            lineIndex++;
        }
        final String rawLine = bufferedReader.readLine();
        final Map<CsvColumn, Object> values = parseLine(rawLine);
        final CsvLine line = new CsvLine(rawLine, values, lineIndex);
        lineIndex++;
        return line;
    }

    private Map<CsvColumn, Object> parseLine(String line) {
        if (line == null) {
            return null;
        }
        final String[] values = line.split(",");
        final List<CsvColumn> cols = csvDefinition.getColumns();
        final Map<CsvColumn, Object> result = new LinkedHashMap<CsvColumn, Object>();
        for (final CsvColumn col : cols) {
            final String value = col.index() < values.length ? values[col.index()] : null;
            final Object converted;
            try {
                converted = col.convert(value);
            } catch (Exception e) {
                throw new RuntimeException("conversion of value <" + value + "> failed for column " + col + " " + CsvReaderUtil.getFileLocation(this), e);
            }
            result.put(col, converted);
        }
        return result;
    }

}
