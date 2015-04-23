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
package ch.algotrader.util;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.util.Locale;

/**
 * Date/time patterns and commonly used time zones.
 */
public final class DateTimePatterns {

    public static final ZoneId GMT = ZoneId.of("GMT");

    /**
     * The local (zone-less) date format: {@literal yyyy-MM-dd}.
     */
    public final static DateTimeFormatter LOCAL_DATE;
    static {
        LOCAL_DATE = new DateTimeFormatterBuilder()
                .append(DateTimeFormatter.ISO_LOCAL_DATE)
                .toFormatter(Locale.ROOT);
    }

    /**
     * The local (zone-less) time format: {@literal HH:mm:ss}.
     */
    public final static DateTimeFormatter LOCAL_TIME;
    static {
        LOCAL_TIME = new DateTimeFormatterBuilder()
                .appendValue(ChronoField.HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                .optionalStart()
                .appendLiteral(':')
                .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
                .toFormatter(Locale.ROOT);
    }

    /**
     * The local (zone-less) date time format: {@literal yyyy-MM-dd HH:mm:ss}.
     */
    public final static DateTimeFormatter LOCAL_DATE_TIME;
    static {
        LOCAL_DATE_TIME = new DateTimeFormatterBuilder()
                .append(LOCAL_DATE)
                .appendLiteral(' ')
                .append(LOCAL_TIME)
                .toFormatter(Locale.ROOT);
    }

    /**
     * The local (zone-less) date time format with milliseconds: {@literal yyyy-MM-dd HH:mm:ss.SSS}.
     */
    public final static DateTimeFormatter LOCAL_DATE_TIME_MILLIS;
    static {
        LOCAL_DATE_TIME_MILLIS = new DateTimeFormatterBuilder()
                .append(LOCAL_DATE_TIME)
                .appendLiteral('.')
                .appendValue(ChronoField.MILLI_OF_SECOND, 3)
                .toFormatter(Locale.ROOT);
    }

    /**
     * The date time format with an explicit zone: {@literal yyyy-MM-dd HH:mm:ss z}.
     */
    public final static DateTimeFormatter ZONED_DATE_TIME;
    static {
        ZONED_DATE_TIME = new DateTimeFormatterBuilder()
                .append(DateTimeFormatter.ISO_LOCAL_DATE)
                .appendLiteral(' ')
                .append(DateTimeFormatter.ISO_LOCAL_TIME)
                .appendLiteral(' ')
                .appendZoneText(TextStyle.SHORT)
                .toFormatter(Locale.ROOT);
    }

    /**
     * The short format used by options: {@literal MMM/yy}.
     */
    public final static DateTimeFormatter OPTION_MONTH_YEAR;
    static {
        OPTION_MONTH_YEAR = new DateTimeFormatterBuilder()
                .appendText(ChronoField.MONTH_OF_YEAR, TextStyle.SHORT)
                .appendLiteral('/')
                .appendValueReduced(ChronoField.YEAR, 2, 2, 2000)
                .toFormatter(Locale.ROOT);
    }

    /**
     * The long format used by options: {@literal dd/MMM/yy}.
     */
    public final static DateTimeFormatter OPTION_DAY_MONTH_YEAR;
    static {
        OPTION_DAY_MONTH_YEAR = new DateTimeFormatterBuilder()
                .appendValue(ChronoField.DAY_OF_MONTH, 2)
                .appendLiteral('/')
                .append(OPTION_MONTH_YEAR)
                .toFormatter(Locale.ROOT);
    }

    /**
     * The 4-digit year format: {@literal yyyy}.
     */
    public final static DateTimeFormatter YEAR_4_DIGIT;
    static {
        YEAR_4_DIGIT = new DateTimeFormatterBuilder()
                .appendValue(ChronoField.YEAR, 4)
                .toFormatter(Locale.ROOT);

    }

    /**
     * The short month text format: {@literal MMM}.
     */
    public final static DateTimeFormatter MONTH_LONG;
    static {
        MONTH_LONG = new DateTimeFormatterBuilder()
                .appendText(ChronoField.MONTH_OF_YEAR, TextStyle.SHORT)
                .toFormatter(Locale.ROOT);
    }

    /**
     * The day of month format: {@literal dd}.
     */
    public final static DateTimeFormatter DAY_OF_MONTH;
    static {
        DAY_OF_MONTH = new DateTimeFormatterBuilder()
                .appendValue(ChronoField.DAY_OF_MONTH, 2)
                .toFormatter(Locale.ROOT);

    }

    /**
     * The week of month format: {@literal W}.
     */
    public final static DateTimeFormatter WEEK_OF_MONTH;
    static {
        WEEK_OF_MONTH = new DateTimeFormatterBuilder()
                .appendValue(ChronoField.ALIGNED_WEEK_OF_MONTH)
                .toFormatter(Locale.ROOT);

    }

}
