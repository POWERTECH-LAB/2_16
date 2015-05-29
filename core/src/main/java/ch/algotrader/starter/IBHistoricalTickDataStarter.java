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
package ch.algotrader.starter;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.algotrader.ServiceLocator;
import ch.algotrader.entity.exchange.Exchange;
import ch.algotrader.entity.marketData.Bar;
import ch.algotrader.entity.marketData.Tick;
import ch.algotrader.entity.security.Security;
import ch.algotrader.enumeration.BarType;
import ch.algotrader.enumeration.Duration;
import ch.algotrader.enumeration.TimePeriod;
import ch.algotrader.service.CalendarService;
import ch.algotrader.service.HistoricalDataService;
import ch.algotrader.util.DateTimeLegacy;
import ch.algotrader.util.io.CsvTickWriter;

/**
 * Starter Class to download historical 1min tick data and save them to CSV files
 * <p>
 * Usage: {@code IBHistoricalTickDataStarter fromDate toDate barType(s) securityId(s)}
 * <p>
 * Examle: {@code IBHistoricalTickDataStarter 20120115 20120216 BID:ASK 29:103850:104586:104587:104588}
 *
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class IBHistoricalTickDataStarter {

    private static final Logger logger = LogManager.getLogger(IBHistoricalTickDataStarter.class.getName());

    public static void main(String[] args) throws Exception {

        Date startDate = DateTimeLegacy.parseAsLocalDate(args[0]);
        Date endDate = DateTimeLegacy.parseAsLocalDate(args[1]);

        String[] barTypesString = args[2].split(":");
        Set<BarType> barTypes = new HashSet<>();
        for (String element : barTypesString) {
            barTypes.add(BarType.valueOf(element));
        }

        String[] securityIdStrings = args[3].split(":");
        long[] securityIds = new long[securityIdStrings.length];
        for (int i = 0; i < securityIdStrings.length; i++) {
            securityIds[i] = Long.parseLong(securityIdStrings[i]);
        }

        ServiceLocator.instance().runServices();

        download1MinTicks(securityIds, barTypes, startDate, endDate);

        ServiceLocator.instance().shutdown();
    }

    private static void download1MinTicks(long[] securityIds, Set<BarType> barTypes, Date startDate, Date endDate) throws Exception {

        for (long securityId : securityIds) {

            Security security = ServiceLocator.instance().getLookupService().getSecurityInclFamilyAndUnderlying(securityId);

            String fileName = security.getIsin() != null ? security.getIsin() : security.getSymbol() != null ? security.getSymbol() : String.valueOf(security.getId());
            CsvTickWriter writer = new CsvTickWriter(fileName);

            download1MinTicksForSecurity(security, barTypes, startDate, endDate, writer);

            writer.close();
        }
    }

    private static void download1MinTicksForSecurity(Security security, Set<BarType> barTypes, Date startDate, Date endDate, CsvTickWriter writer) throws Exception {

        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(startDate);

        Date date;
        while ((date = cal.getTime()).compareTo(endDate) <= 0) {

            if ((cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) && (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY)) {

                download1MinTicksForDate(security, date, barTypes, writer);
            }
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    private static void download1MinTicksForDate(Security security, Date date, Set<BarType> barTypes, CsvTickWriter writer) throws Exception {

        TreeMap<Date, Tick> timeTickMap = new TreeMap<>();

        HistoricalDataService historicalDataService = ServiceLocator.instance().getHistoricalDataService();
        CalendarService calendarService = ServiceLocator.instance().getCalendarService();
        Exchange exchange = security.getSecurityFamily().getExchange();

        // run all barTypes and get the ticks
        for (BarType barType : barTypes) {

            List<Bar> bars = historicalDataService.getHistoricalBars(security.getId(), date, 1, TimePeriod.DAY, Duration.MIN_1, barType, new HashMap<>());

            // filter Bars by date
            for (Iterator<Bar> it = bars.iterator(); it.hasNext();) {

                Bar bar = it.next();

                // discard bars outside trading hours
                if (!calendarService.isOpen(exchange.getId(), bar.getDateTime())) {
                    it.remove();
                    continue;
                }

                // only accept bars within the last 24h
                if (date.getTime() - bar.getDateTime().getTime() > 86400000) {

                    it.remove();
                    continue;
                }
            }

            // to make sure we don't get a pacing error
            Thread.sleep(11000);

            for (Bar bar : bars) {

                Tick tick = timeTickMap.get(bar.getDateTime());

                if (tick == null) {

                    tick = Tick.Factory.newInstance();
                    tick.setSecurity(security);
                    tick.setLast(new BigDecimal(0));
                    tick.setDateTime(bar.getDateTime());
                    tick.setVol(0);
                    tick.setVolBid(0);
                    tick.setVolAsk(0);
                    tick.setBid(new BigDecimal(0));
                    tick.setAsk(new BigDecimal(0));

                    timeTickMap.put(bar.getDateTime(), tick);
                }

                if (BarType.TRADES.equals(barType)) {
                    Entry<Date, Tick> entry = timeTickMap.lowerEntry(bar.getDateTime());
                    if (entry != null) {
                        Tick lastTick = entry.getValue();
                        if (bar.getVol() > 0) {
                            tick.setVol(lastTick.getVol() + bar.getVol());
                            tick.setLastDateTime(bar.getDateTime());
                        } else {
                            tick.setVol(lastTick.getVol());
                            tick.setLastDateTime(lastTick.getLastDateTime());
                        }
                    } else {
                        if (bar.getVol() > 0) {
                            tick.setVol(bar.getVol());
                            tick.setLastDateTime(bar.getDateTime());
                        }
                    }
                    BigDecimal last = bar.getClose();
                    tick.setLast(last);
                } else if (BarType.BID.equals(barType)) {
                    BigDecimal bid = bar.getClose();
                    tick.setBid(bid);
                } else if (BarType.ASK.equals(barType)) {
                    BigDecimal ask = bar.getClose();
                    tick.setAsk(ask);
                } else if (BarType.BID_ASK.equals(barType)) {
                    BigDecimal bid = bar.getOpen();
                    tick.setBid(bid);
                    BigDecimal ask = bar.getClose();
                    tick.setAsk(ask);
                }
            }
        }

        // write the ticks to the csvWriter
        for (Map.Entry<Date, Tick> entry : timeTickMap.entrySet()) {

            Tick tick = entry.getValue();
            writer.write(tick);
        }

        logger.debug("wrote " + timeTickMap.entrySet().size() + " ticks for: " + security + " on date: " + date);
    }
}
