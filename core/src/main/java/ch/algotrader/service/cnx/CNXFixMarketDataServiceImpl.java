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
package ch.algotrader.service.cnx;

import org.apache.commons.lang.Validate;

import quickfix.field.SubscriptionRequestType;
import quickfix.fix44.MarketDataRequest;
import ch.algotrader.adapter.cnx.CNXFixMarketDataRequestFactory;
import ch.algotrader.adapter.cnx.CNXUtil;
import ch.algotrader.adapter.fix.FixAdapter;
import ch.algotrader.adapter.fix.FixApplicationException;
import ch.algotrader.adapter.fix.FixSessionLifecycle;
import ch.algotrader.entity.security.Forex;
import ch.algotrader.entity.security.Security;
import ch.algotrader.entity.security.SecurityDao;
import ch.algotrader.enumeration.FeedType;
import ch.algotrader.service.fix.fix44.Fix44MarketDataServiceImpl;

/**
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class CNXFixMarketDataServiceImpl extends Fix44MarketDataServiceImpl implements CNXFixMarketDataService {

    private static final long serialVersionUID = 2946126163433296876L;

    private final CNXFixMarketDataRequestFactory requestFactory;

    public CNXFixMarketDataServiceImpl(final FixSessionLifecycle lifeCycle,
            final FixAdapter fixAdapter,
            final SecurityDao securityDao) {

        super(lifeCycle, fixAdapter, securityDao);

        this.requestFactory = new CNXFixMarketDataRequestFactory();
    }

    @Override
    public void sendSubscribeRequest(Security security) {

        Validate.notNull(security, "Security is null");

        try {
            MarketDataRequest request = this.requestFactory.create(security, new SubscriptionRequestType(SubscriptionRequestType.SNAPSHOT_PLUS_UPDATES));

            getFixAdapter().sendMessage(request, getSessionQualifier());
        } catch (Exception ex) {
            throw new CNXFixMarketDataServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public String getTickerId(Security security) {

        Validate.notNull(security, "Security is null");

        try {
            if (!(security instanceof Forex)) {
                throw new FixApplicationException("Currenex supports forex orders only");
            }
            Forex forex = (Forex) security;
            return CNXUtil.getCNXSymbol(forex);
        } catch (Exception ex) {
            throw new CNXFixMarketDataServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public void sendUnsubscribeRequest(Security security) {

        Validate.notNull(security, "Security is null");

        try {
            MarketDataRequest request = this.requestFactory.create(security, new SubscriptionRequestType(SubscriptionRequestType.DISABLE_PREVIOUS_SNAPSHOT_PLUS_UPDATE_REQUEST));

            getFixAdapter().sendMessage(request, getSessionQualifier());
        } catch (Exception ex) {
            throw new CNXFixMarketDataServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public String getSessionQualifier() {

        return "CNXMD";
    }

    @Override
    public FeedType getFeedType() {

        return FeedType.CNX;
    }
}
