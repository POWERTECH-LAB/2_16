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
package com.algoTrader.util.io;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.apache.commons.lang.time.DateUtils;
import org.supercsv.exception.SuperCSVException;

import com.algoTrader.entity.marketData.Bar;
import com.algoTrader.util.DateUtil;

/**
 * @author <a href="mailto:andyflury@gmail.com">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class CsvBarGapFiller {

    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy kk:mm:ss");
    private static double weekendDiff = 2800;
    private static String endTime = "23:00";

    public static void main(String[] args) throws SuperCSVException, IOException, ParseException {

        File readerFile = new File("files" + File.separator + "bardata" + File.separator + args[0]);
        File writerFile = new File("files" + File.separator + "bardata" + File.separator + args[1]);

        CsvBarReader reader = new CsvBarReader(readerFile);
        CsvBarWriter writer = new CsvBarWriter(writerFile);

        Bar lastBar = null;
        Bar newBar = null;
        while ((newBar = reader.readBar()) != null) {

            // check gaps
            if (lastBar != null) {

                // check day gap
                long timeDiff = (newBar.getDateTime().getTime() - lastBar.getDateTime().getTime()) / 60000;
                if (timeDiff > weekendDiff) {

                    // weekend gap
                    System.out.println(readerFile.getName() + " " + dateFormat.format(newBar.getDateTime()) + " gap of " + timeDiff + " minutes");

                    writer.write(lastBar);

                    // fill gap until endTime
                    Bar bar = cloneBar(lastBar, lastBar.getClose());
                    bar.setDateTime(lastBar.getDateTime());

                    do {
                        bar.setDateTime(DateUtils.addMinutes(bar.getDateTime(), 1));
                        writer.write(bar);
                    } while (DateUtil.compareTime(bar.getDateTime(), timeFormat.parse(endTime)) < 0);

                } else if (timeDiff == 0) {

                    System.out.println(readerFile.getName() + " " + dateFormat.format(newBar.getDateTime()) + " no gap");

                } else if (timeDiff == 1) {

                    // no gap
                    writer.write(lastBar);

                } else {

                    writer.write(lastBar);

                    // fill small gaps
                    Bar bar = cloneBar(lastBar, lastBar.getClose());

                    for (int i = 1; i < timeDiff; i++) {
                        bar.setDateTime(DateUtils.addMinutes(lastBar.getDateTime(), i));
                        writer.write(bar);
                    }
                }
            }

            lastBar = newBar;
        }

        writer.write(lastBar);

        writer.close();
    }

    private static Bar cloneBar(Bar origBar, BigDecimal price) {

        Bar bar = Bar.Factory.newInstance();
        bar.setOpen(price);
        bar.setHigh(price);
        bar.setLow(price);
        bar.setClose(price);
        bar.setVol(origBar.getVol());
        bar.setOpenIntrest(origBar.getOpenIntrest());
        bar.setSettlement(origBar.getSettlement());
        return bar;
    }
}
