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
package ch.algotrader.service;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.time.DateUtils;

import ch.algotrader.entity.security.Holiday;
import ch.algotrader.entity.security.Market;

/**
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class CalendarServiceImpl extends CalendarServiceBase {

    private Map<Market, Map<Date, Holiday>> holidays = new HashMap<Market, Map<Date, Holiday>>();

    @Override
    protected boolean handleIsMarketOpen(int marketId, Date date) throws Exception {

        Market market = getMarketDao().get(marketId);

        // are we on a trading day?
        if (!isTradingDay(market, date)) {
            return false;
        }

        // market session starting today
        Date todayOpen = getOpenTime(market, date);
        Date todayClose = getCloseTime(market, date);

        // is close is on the next
        if (market.getOpen().compareTo(market.getClose()) >= 0) {
            todayClose = DateUtils.addDays(todayClose, 1);
        }

        // are we during todays session?
        if (isTradingDay(market, todayOpen) && date.compareTo(todayOpen) >= 0 && date.compareTo(todayClose) <= 0) {
            return true;
        }

        // market session starting yesterday
        Date yesterdayOpen = DateUtils.addDays(todayOpen, -1);
        Date yesterdayClose = DateUtils.addDays(todayClose, -1);

        // are we during yesterdays session
        if (isTradingDay(market, yesterdayOpen) && date.compareTo(yesterdayOpen) >= 0 && date.compareTo(yesterdayClose) <= 0) {
            return true;
        }

        return false;
    }

    @Override
    protected boolean handleIsTradingDay(int marketId, Date date) throws Exception {

        Market market = getMarketDao().get(marketId);
        return isTradingDay(market, date);
    }

    private boolean isTradingDay(Market market, Date date) {

        Holiday holiday = getHoliday(market, date);
        if (holiday != null && !holiday.isPartialOpen()) {
            return false;
        } else {
            int dayOfWeek = toDayOfWeek(date.getTime());
            int openDay = market.getOpenDay().getValue();
            return dayOfWeek >= openDay && dayOfWeek <= openDay + 4;
        }
    }

    @Override
    protected Date handleGetOpenTime(int marketId, Date date) throws Exception {

        Market market = getMarketDao().get(marketId);
        return getOpenTime(market, date);
    }

    private Date getOpenTime(Market market, Date date) {

        if (!isTradingDay(market, date)) {
            return null;
        }

        Holiday holiday = getHoliday(market, date);
        if (holiday != null && holiday.getLateOpen() != null) {
            return setTime(date, holiday.getLateOpen());
        } else {
            return setTime(date, market.getOpen());
        }
    }

    @Override
    protected Date handleGetCloseTime(int marketId, Date date) throws Exception {

        Market market = getMarketDao().get(marketId);
        return getCloseTime(market, date);
    }

    private Date getCloseTime(Market market, Date date) {

        if (!isTradingDay(market, date)) {
            return null;
        }

        Holiday holiday = getHoliday(market, date);
        if (holiday != null && holiday.getEarlyClose() != null) {
            return setTime(date, holiday.getEarlyClose());
        } else {
            return setTime(date, market.getClose());
        }
    }

    private Holiday getHoliday(Market market, Date date) {

        // load all holidays for this market
        if (!this.holidays.containsKey(market)) {
            Map<Date, Holiday> map = new HashMap<Date, Holiday>();
            for (Holiday holiday : market.getHolidays()) {
                map.put(holiday.getDate(), holiday);
            }
            this.holidays.put(market, map);
        }

        return this.holidays.get(market).get(DateUtils.truncate(date, Calendar.DATE));
    }

    private int toDayOfWeek(long time) {

        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(time);
        return cal.get(Calendar.DAY_OF_WEEK);
    }

    private Date setTime(Date date, Date time) {

        Calendar timeCal = Calendar.getInstance();
        timeCal.setTime(time);

        Calendar dateCal = Calendar.getInstance();
        dateCal.setTime(date);
        dateCal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
        dateCal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
        dateCal.set(Calendar.SECOND, timeCal.get(Calendar.SECOND));
        dateCal.set(Calendar.MILLISECOND, timeCal.get(Calendar.MILLISECOND));

        return dateCal.getTime();
    }

}
