package com.algoTrader.service.rbs;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ParseBigDecimal;
import org.supercsv.cellprocessor.ParseDate;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.ParseLong;
import org.supercsv.cellprocessor.constraint.StrNotNullOrEmpty;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.exception.SuperCSVReflectionException;
import org.supercsv.io.CsvMapReader;
import org.supercsv.prefs.CsvPreference;

public class CsvRBSTradeReader {

    //@formatter:off
    private static CellProcessor[] processor = new CellProcessor[] {
        new ParseDate("dd/MM/yyyy"),
        new ParseInt(),
        new Optional(),
        new Optional(),
        new StrNotNullOrEmpty(),
        new ParseDate("dd/MM/yyyy"),
        new StrNotNullOrEmpty(),
        new StrNotNullOrEmpty(),
        new ParseInt(),
        new ParseInt(),
        new ParseLong(),
        new ParseBigDecimal(),
        new Optional(),
        new StrNotNullOrEmpty(),
        new ParseDate("dd/MM/yyyy"),
        new StrNotNullOrEmpty(),
        new ParseBigDecimal(),
        new ParseBigDecimal(),
        new StrNotNullOrEmpty(),
        new StrNotNullOrEmpty(),
        new StrNotNullOrEmpty(),
        new Optional(),
        new Optional(),
        new ParseDate("dd/MM/yyyy"),
        new StrNotNullOrEmpty(),
        new StrNotNullOrEmpty(),
    };
    //@formatter:on

    public static void main(String[] args) throws SuperCSVReflectionException, IOException {
        System.out.println(CsvRBSTradeReader.readPositions("files/rbs/RBS_Trades_Linard.csv"));
    }

    public static List<Map<String, ? super Object>> readPositions(String fileName) throws SuperCSVReflectionException, IOException {

        File file = new File(fileName);

        CsvMapReader reader = new CsvMapReader(new FileReader(file), CsvPreference.EXCEL_PREFERENCE);
        String[] header = reader.getCSVHeader(true);

        List<Map<String, ? super Object>> list = new ArrayList<Map<String, ? super Object>>();

        Map<String, ? super Object> position;
        while ((position = reader.read(header, processor)) != null) {
            list.add(position);
        }

        reader.close();

        return list;
    }
}
