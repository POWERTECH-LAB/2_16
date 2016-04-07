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
package ch.algotrader.event;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import ch.algotrader.entity.BaseEntityI;
import ch.algotrader.entity.security.ExpirableI;
import ch.algotrader.entity.security.Forex;
import ch.algotrader.entity.security.ForexImpl;
import ch.algotrader.entity.security.Future;
import ch.algotrader.entity.security.FutureI;
import ch.algotrader.entity.security.FutureImpl;
import ch.algotrader.entity.security.Option;
import ch.algotrader.entity.security.OptionImpl;
import ch.algotrader.entity.security.Security;
import ch.algotrader.entity.security.SecurityI;
import ch.algotrader.entity.security.SecurityImpl;
import ch.algotrader.entity.security.Stock;
import ch.algotrader.entity.security.StockImpl;

/**
 * @author <a href="mailto:okalnichevski@algotrader.ch">Oleg Kalnichevski</a>
 */
@RunWith(MockitoJUnitRunner.class)
public class EventListenerRegistryImplTest {

    @Mock
    private EventListener<Security> listener1;
    @Mock
    private EventListener<Forex> listener2;
    @Mock
    private EventListener<Future> listener3;
    @Mock
    private EventListener<ExpirableI> listener4;

    @Test
    public void testRegisterUnregister() throws Exception {

        EventListenerRegistryImpl registry = new EventListenerRegistryImpl();

        Set<EventListener<?>> listeners0 = registry.getListeners(Security.class);
        Assert.assertNotNull(listeners0);
        Assert.assertTrue(listeners0.isEmpty());

        registry.register(this.listener1, Security.class);
        Set<EventListener<?>> listeners1 = registry.getListeners(Security.class);
        Assert.assertEquals(1, listeners1.size());

        registry.register(this.listener1, Security.class);
        registry.register(this.listener1, Security.class);

        Set<EventListener<?>> listeners2 = registry.getListeners(Security.class);
        Assert.assertEquals(1, listeners2.size());

        registry.unregister(this.listener1, Security.class);

        Set<EventListener<?>> listeners3 = registry.getListeners(Security.class);
        Assert.assertEquals(0, listeners3.size());
    }

    @Test
    public void testCalculateTypeHierarchy() throws Exception {

        Class<?>[] allTypes = EventTypeCache.calculateTypeHierarchy(FutureImpl.class);
        Assert.assertNotNull(allTypes);
        Assert.assertArrayEquals(new Class<?>[] {
                FutureImpl.class,
                Future.class,
                FutureI.class,
                ExpirableI.class,
                BaseEntityI.class,
                SecurityImpl.class,
                Security.class,
                SecurityI.class,
                Object.class
        } , allTypes);
    }

    @Test
    public void testSimpleBroadcastAsExactType() throws Exception {

        EventListenerRegistryImpl registry = new EventListenerRegistryImpl();

        registry.register(this.listener1, Security.class);
        registry.register(this.listener2, Forex.class);
        registry.register(this.listener3, Future.class);

        Forex event = new ForexImpl();

        registry.broadcast(event);

        Mockito.verify(this.listener2, Mockito.times(1)).onEvent(Mockito.<Forex>any());
        Mockito.verify(this.listener1, Mockito.times(1)).onEvent(Mockito.<Security>any());
        Mockito.verify(this.listener3, Mockito.never()).onEvent(Mockito.<Future>any());
    }

    @Test
    public void testSimpleBroadcastAsBaseType() throws Exception {

        EventListenerRegistryImpl registry = new EventListenerRegistryImpl();

        registry.register(this.listener1, Security.class);
        registry.register(this.listener2, Forex.class);
        registry.register(this.listener3, Future.class);

        Stock event = new StockImpl();

        registry.broadcast(event);

        Mockito.verify(this.listener1, Mockito.times(1)).onEvent(Mockito.<Security>any());
        Mockito.verify(this.listener2, Mockito.never()).onEvent(Mockito.<Forex>any());
        Mockito.verify(this.listener3, Mockito.never()).onEvent(Mockito.<Future>any());
    }

    @Test
    public void testSimpleBroadcastAsInterface() throws Exception {

        EventListenerRegistryImpl registry = new EventListenerRegistryImpl();

        registry.register(this.listener1, Security.class);
        registry.register(this.listener2, Forex.class);
        registry.register(this.listener3, Future.class);
        registry.register(this.listener4, ExpirableI.class);

        Option event = new OptionImpl();

        registry.broadcast(event);

        Mockito.verify(this.listener4, Mockito.times(1)).onEvent(Mockito.<ExpirableI>any());
        Mockito.verify(this.listener1, Mockito.times(1)).onEvent(Mockito.<Security>any());
        Mockito.verify(this.listener2, Mockito.never()).onEvent(Mockito.<Forex>any());
        Mockito.verify(this.listener3, Mockito.never()).onEvent(Mockito.<Future>any());
    }

}
