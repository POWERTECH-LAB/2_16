package com.algoTrader.esper.io;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.collections15.CollectionUtils;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.supercsv.exception.SuperCSVException;

import com.algoTrader.entity.marketData.Tick;

public class CsvMerger {

    public static void main(String[] args) throws SuperCSVException, IOException {

        File aDir = new File("results/tickdata/" + args[0] + "/");
        File bDir = new File("results/tickdata/" + args[1] + "/");
        File cDir = new File("results/tickdata/" + args[2] + "/");

        if (!cDir.exists()) {
            cDir.mkdir();
        }

        Collection<String> aNames = CollectionUtils.collect(Arrays.asList(aDir.listFiles()), new Transformer<File, String>() {
            @Override
            public String transform(File file) {
                return FilenameUtils.getName(file.getName());
            }});

        Collection<String> bNames = CollectionUtils.collect(Arrays.asList(bDir.listFiles()), new Transformer<File, String>() {
            @Override
            public String transform(File file) {
                return FilenameUtils.getName(file.getName());
            }});

        for (String fileName : new HashSet<String>(CollectionUtils.union(aNames, bNames))) {
            if (aNames.contains(fileName) && bNames.contains(fileName)) {

                CsvTickReader aReader = new CsvTickReader(new File(aDir.getPath() + "/" + fileName));
                CsvTickReader bReader = new CsvTickReader(new File(bDir.getPath() + "/" + fileName));
                CsvTickWriter csvWriter = new CsvTickWriter(new File(cDir.getPath() + "/" + fileName));

                Tick aTick = aReader.readTick();
                Tick bTick = bReader.readTick();
                Tick lastTick = null;
                do {

                    Tick newTick = null;
                    if (aTick != null & bTick == null) {
                        newTick = aTick;
                        aTick = aReader.readTick();
                    } else if (bTick != null & aTick == null) {
                        newTick = bTick;
                        bTick = bReader.readTick();
                    } else if (aTick.getDateTime().getTime() < bTick.getDateTime().getTime()) {
                        newTick = aTick;
                        aTick = aReader.readTick();
                    } else if (bTick.getDateTime().getTime() < aTick.getDateTime().getTime()) {
                        newTick = bTick;
                        bTick = bReader.readTick();
                    } else {
                        newTick = aTick;
                        aTick = aReader.readTick();
                        bTick = bReader.readTick();
                    }

                    if (newTick.getLast().equals(null)) {
                        newTick.setLast(new BigDecimal(0));
                    }

                    if (newTick.getLastDateTime().equals(null)) {
                        newTick.setLastDateTime(newTick.getDateTime());
                    }

                    csvWriter.write(newTick);

                    if (lastTick != null) {
                        double daysDiff = (double) (newTick.getDateTime().getTime() - lastTick.getDateTime().getTime()) / 86400000;
                        if (daysDiff > 4.0) {
                            System.out.println(fileName + " at " + newTick.getDateTime() + " gap of " + daysDiff);
                        }
                    }
                    lastTick = newTick;

                } while (aTick != null || bTick != null);

                csvWriter.close();

            } else if (aNames.contains(fileName)) {
                FileUtils.copyFileToDirectory(new File(aDir.getPath() + "/" + fileName), cDir);
            } else if (bNames.contains(fileName)) {
                FileUtils.copyFileToDirectory(new File(bDir.getPath() + "/" + fileName), cDir);
            }
        }
    }
}
