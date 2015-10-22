/***********************************************************************************
 * AlgoTrader Enterprise Trading Framework
 *
 * Copyright (C) 2015 AlgoTrader GmbH - All rights reserved
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
 * Aeschstrasse 6
 * 8834 Schindellegi
 ***********************************************************************************/
package ch.algotrader.entity.property;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 */
public abstract class PropertyHolderImpl extends PropertyHolder {

    private static final long serialVersionUID = 2154726089257967279L;

    @Override
    public int getIntProperty(String name) {

        Property property = getProps().get(name);
        if (property != null) {
            if (property.getIntValue() != null) {
                return property.getIntValue();
            } else {
                throw new IllegalArgumentException("property " + name + " does not have a int value");
            }
        } else {
            throw new IllegalArgumentException("property " + name + " is not defined");
        }
    }

    @Override
    public double getDoubleProperty(String name) {

        Property property = getProps().get(name);
        if (property != null) {
            if (property.getDoubleValue() != null) {
                return property.getDoubleValue();
            } else {
                throw new IllegalArgumentException("property " + name + " does not have a double value");
            }
        } else {
            throw new IllegalArgumentException("property " + name + " is not defined");
        }
    }

    @Override
    public BigDecimal getMoneyProperty(String name) {

        Property property = getProps().get(name);
        if (property != null) {
            if (property.getMoneyValue() != null) {
                return property.getMoneyValue();
            } else {
                throw new IllegalArgumentException("property " + name + " does not have a Money value");
            }
        } else {
            throw new IllegalArgumentException("property " + name + " is not defined");
        }
    }

    @Override
    public String getTextProperty(String name) {

        Property property = getProps().get(name);
        if (property != null) {
            if (property.getTextValue() != null) {
                return property.getTextValue();
            } else {
                throw new IllegalArgumentException("property " + name + " does not have a Text value");
            }
        } else {
            throw new IllegalArgumentException("property " + name + " is not defined");
        }
    }

    @Override
    public Date getDateProperty(String name) {

        Property property = getProps().get(name);
        if (property != null) {
            if (property.getDateTimeValue() != null) {
                return property.getDateTimeValue();
            } else {
                throw new IllegalArgumentException("property " + name + " does not have a Date value");
            }
        } else {
            throw new IllegalArgumentException("property " + name + " is not defined");
        }
    }

    @Override
    public boolean getBooleanProperty(String name) {

        Property property = getProps().get(name);
        if (property != null) {
            if (property.getBooleanValue() != null) {
                return property.getBooleanValue();
            } else {
                throw new IllegalArgumentException("property " + name + " does not have a Boolean value");
            }
        } else {
            throw new IllegalArgumentException("property " + name + " is not defined");
        }
    }

    @Override
    public Boolean hasProperty(String name) {

        return getProps().containsKey(name);
    }

    @Override
    public Map<String, Object> getPropertyNameValueMap() {

        Map<String, Object> nameValuePairs = new HashMap<>();
        for (Property property : getProps().values()) {
            nameValuePairs.put(property.getName(), property.getValue());
        }
        return nameValuePairs;
    }
}
