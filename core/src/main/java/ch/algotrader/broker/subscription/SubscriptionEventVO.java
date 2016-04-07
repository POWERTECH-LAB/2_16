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

package ch.algotrader.broker.subscription;

import java.io.Serializable;

/**
 * @author <a href="mailto:vgolding@algotrader.ch">Vince Golding</a>
 */
public class SubscriptionEventVO implements Serializable {

    private static final long serialVersionUID = -4304145393532328472L;

    private final String baseTopic;
    private final String key;
    private final boolean subscribe;

    public SubscriptionEventVO(final String baseTopic, final String key, boolean subscribe) {
        this.baseTopic = baseTopic;
        this.key = key;
        this.subscribe = subscribe;
    }

    public boolean isSubscribe() {
        return subscribe;
    }

    public String getBaseTopic() {
        return baseTopic;
    }

    public String getSubscriptionKey() {
        return this.key;
    }

    @Override
    public String toString() {
        return this.baseTopic + "." + this.key + "(" + this.subscribe + ")";
    }

}
