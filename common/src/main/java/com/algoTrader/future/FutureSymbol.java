package com.algoTrader.future;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.algoTrader.entity.security.SecurityFamily;

public class FutureSymbol {

    private static final String[] monthEnc = { "F", "G", "H", "J", "K", "M", "N", "Q", "U", "V", "X", "Z" };
    private static final String[] yearEnc = { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J" };

    public static String getSymbol(SecurityFamily family, Date expiration) {

        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(expiration);

        StringBuffer buffer = new StringBuffer();
        buffer.append(family.getBaseSymbol());
        buffer.append(" ");
        buffer.append(new SimpleDateFormat("MMM").format(cal.getTime()).toUpperCase());
        buffer.append("/");
        buffer.append(String.valueOf(cal.get(Calendar.YEAR)).substring(2));

        return buffer.toString();
    }

    public static String getIsin(SecurityFamily family, Date expiration) {

        int week = 0;

        String month;
        Calendar cal = new GregorianCalendar();
        cal.setTime(expiration);
        month = monthEnc[cal.get(Calendar.MONTH)];

        int yearIndex = cal.get(Calendar.YEAR) % 10;
        String year = yearEnc[yearIndex];

        StringBuffer buffer = new StringBuffer();
        buffer.append(week);
        buffer.append("F");
        buffer.append(family.getBaseSymbol());
        buffer.append(month);
        buffer.append(year);
        buffer.append("00000");

        return buffer.toString();
    }

    public static String getRic(SecurityFamily family, Date expiration) {

        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(expiration);

        StringBuffer buffer = new StringBuffer();
        buffer.append(family.getRicRoot());
        buffer.append(monthEnc[cal.get(Calendar.MONTH)]);
        buffer.append(String.valueOf(cal.get(Calendar.YEAR)).substring(3));
        buffer.append(":VE");

        return buffer.toString();
    }

    public static int getWeek(String symbol) {

        return Integer.parseInt(symbol.substring(0, 1));
    }

    public static String getUnderlying(String symbol) {

        return symbol.substring(2, 5);
    }

    public static int getMonth(String symbol) {

        String month = symbol.substring(5, 6);
        int index = Arrays.binarySearch(monthEnc, month);

        return index;
    }

    public static int getYear(String symbol) {

        String year = symbol.substring(6, 7);
        return Arrays.binarySearch(yearEnc, year) + 2010;
    }
}
