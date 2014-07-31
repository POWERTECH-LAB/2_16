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
package ch.algotrader.configeditor.editingsupport;

import ch.algotrader.configeditor.IPropertySerializer;

/**
 * Serializer for double (floating-point) values.
 *
 * @author <a href="mailto:ahihlovskiy@algotrader.ch">Andrey Hihlovskiy</a>
 *
 * @version $Revision$ $Date$
 */
public class DoubleSerializer implements IPropertySerializer {

    @Override
    public Object deserialize(String propValue) {
        if (propValue.contains(","))
            propValue = propValue.replace(',', '.');
        return new Double(propValue);
    }

    @Override
    public String serialize(Object propObject) {
        return propObject.toString();
    }
}
