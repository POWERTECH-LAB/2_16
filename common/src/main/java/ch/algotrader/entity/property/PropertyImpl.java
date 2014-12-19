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
package ch.algotrader.entity.property;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

/**
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class PropertyImpl extends Property {

    private static final long serialVersionUID = -3790829266158776151L;

    @Override
    public Object getValue() {

        if (getIntValue() != null) {
            return getIntValue();
        } else if (getDoubleValue() != null) {
            return getDoubleValue();
        } else if (getMoneyValue() != null) {
            return getMoneyValue();
        } else if (getTextValue() != null) {
            return getTextValue();
        } else if (getDateTimeValue() != null) {
            return getDateTimeValue();
        } else if (getBooleanValue() != null) {
            return getBooleanValue();
        } else {
            return null;
        }
    }

    @Override
    public void setValue(Object value) {

        // set all values to null to prevent double value settings
        setIntValue(null);
        setDoubleValue(null);
        setMoneyValue(null);
        setTextValue(null);
        setDateTimeValue(null);
        setBooleanValue(null);

        // set the value of the correct type
        if (value instanceof Integer) {
            setIntValue((Integer) value);
        } else if (value instanceof Double) {
            setDoubleValue((Double) value);
        } else if (value instanceof BigDecimal) {
            setMoneyValue((BigDecimal) value);
        } else if (value instanceof String) {
            setTextValue((String) value);
        } else if (value instanceof Date) {
            setDateTimeValue((Date) value);
        } else if (value instanceof Boolean) {
            setBooleanValue((Boolean) value);
        } else {
            throw new IllegalArgumentException("unsupport value type " + value.getClass());
        }
    }


    @Override
    public String toString() {

        return getValue().toString();
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj instanceof Property) {
            Property that = (Property) obj;
            return Objects.equals(this.getPropertyHolder(), that.getPropertyHolder()) &&
                    Objects.equals(this.getName(), that.getName());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {

        int hash = 17;
        hash = hash * 37 + Objects.hashCode(getPropertyHolder());
        hash = hash * 37 + Objects.hashCode(getName());
        return hash;
    }
}
