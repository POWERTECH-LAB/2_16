package com.algoTrader.util;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

import org.apache.commons.lang.builder.StandardToStringStyle;

import com.algoTrader.BaseObject;

public class CustomToStringStyle extends StandardToStringStyle {

    private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss,SSS");

    private static CustomToStringStyle style;

    public static CustomToStringStyle getInstance() {

        if (style == null) {
            style = new CustomToStringStyle();
            style.setUseClassName(false);
            style.setUseIdentityHashCode(false);
        }
        return style;
    }

    @SuppressWarnings("unchecked")
    protected void appendDetail(StringBuffer buffer, String fieldName, Collection col) {

        buffer.append(col.size());
    }

    protected void appendDetail(StringBuffer buffer, String fieldName, Object value) {

        if ( value instanceof BaseObject ) {
            return;
        } else if (value instanceof Date) {
            String out = format.format(value);
            buffer.append(out);
        } else {
            super.appendDetail(buffer, fieldName, value);
        }
    }

    public void append(StringBuffer buffer, String fieldName, Object value, Boolean fullDetail) {

        if ( value instanceof BaseObject ) {
            return;
        } else {
            super.append(buffer, fieldName, value, fullDetail);
        }
    }
}
