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
package ch.algotrader.ehcache;

import ch.algotrader.cache.QueryCacheEvictionEventVO;
import ch.algotrader.event.dispatch.EventDispatcher;
import ch.algotrader.event.dispatch.EventRecipient;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListenerAdapter;

/**
 * EhCache CacheEventListener which notifies when the UpdateTimestampsCache is being updated in the 2nd level cache.
 *
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 */
public class QueryCacheEventListener extends CacheEventListenerAdapter {

    private EventDispatcher eventDispatcher;

    public QueryCacheEventListener(EventDispatcher eventDispatcher) {
        this.eventDispatcher = eventDispatcher;
    }

    @Override
    public void notifyElementUpdated(Ehcache cache, Element element) throws CacheException {

        String spaceName = (String) element.getObjectKey();

        QueryCacheEvictionEventVO event = new QueryCacheEvictionEventVO(spaceName);
        this.eventDispatcher.broadcast(event, EventRecipient.ALL_LISTENERS);
    }

    @Override
    public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {

        // do nothing
    }

    @Override
    public void notifyElementExpired(Ehcache cache, Element element) {

        // do nothing
    }

    @Override
    public void notifyElementEvicted(Ehcache cache, Element element) {

        // do nothing
    }
}