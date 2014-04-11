package ch.algotrader.adapter.lmax;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ch.algotrader.adapter.fix.DefaultFixApplication;
import ch.algotrader.adapter.fix.DefaultFixSessionLifecycle;
import ch.algotrader.adapter.fix.FixConfigUtils;
import ch.algotrader.adapter.fix.NoopSessionStateListener;
import ch.algotrader.entity.security.Forex;
import ch.algotrader.entity.security.ForexImpl;
import ch.algotrader.entity.security.SecurityFamily;
import ch.algotrader.entity.security.SecurityFamilyImpl;
import ch.algotrader.entity.strategy.StrategyImpl;
import ch.algotrader.entity.trade.Fill;
import ch.algotrader.entity.trade.MarketOrder;
import ch.algotrader.entity.trade.MarketOrderImpl;
import ch.algotrader.entity.trade.OrderStatus;
import ch.algotrader.enumeration.Currency;
import ch.algotrader.enumeration.Status;
import ch.algotrader.esper.AbstractEngine;
import ch.algotrader.esper.EngineLocator;
import ch.algotrader.service.LookupService;
import quickfix.DefaultSessionFactory;
import quickfix.LogFactory;
import quickfix.MemoryStoreFactory;
import quickfix.ScreenLogFactory;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.SocketInitiator;
import quickfix.field.ClOrdID;
import quickfix.field.OrdType;
import quickfix.field.OrderQty;
import quickfix.field.OrigClOrdID;
import quickfix.field.SecurityID;
import quickfix.field.SecurityIDSource;
import quickfix.field.Side;
import quickfix.field.StopPx;
import quickfix.field.TransactTime;
import quickfix.fix44.NewOrderSingle;
import quickfix.fix44.OrderCancelRequest;

public class LMAXFixOrderMessageHandlerTest {

    private LinkedBlockingQueue<Object> eventQueue;
    private LookupService lookupService;
    private LMAXFixOrderMessageHandler messageHandler;
    private Session session;
    private SocketInitiator socketInitiator;

    @Before
    public void setup() throws Exception {

        final LinkedBlockingQueue<Object> queue = new LinkedBlockingQueue<Object>();
        this.eventQueue = queue;

        EngineLocator.instance().setEngine(StrategyImpl.BASE, new AbstractEngine() {

            @Override
            public void sendEvent(Object obj) {
                try {
                    queue.put(obj);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            @Override
            public List executeQuery(String query) {
                return null;
            }
        });


        SessionSettings settings = FixConfigUtils.loadSettings();
        SessionID sessionId = FixConfigUtils.getSessionID(settings, "LMAXT");

        LMAXLogonMessageHandler logonHandler = new LMAXLogonMessageHandler();
        logonHandler.setSettings(settings);

        this.lookupService = Mockito.mock(LookupService.class);
        LMAXFixOrderMessageHandler messageHandlerImpl = new LMAXFixOrderMessageHandler();
        messageHandlerImpl.setLookupService(lookupService);
        this.messageHandler = Mockito.spy(messageHandlerImpl);

        DefaultFixApplication fixApplication = new DefaultFixApplication(sessionId, messageHandler, logonHandler, new DefaultFixSessionLifecycle());

        LogFactory logFactory = new ScreenLogFactory(true, true, true);

        DefaultSessionFactory sessionFactory = new DefaultSessionFactory(fixApplication, new MemoryStoreFactory(), logFactory);

        SocketInitiator socketInitiator = new SocketInitiator(sessionFactory, settings);
        socketInitiator.start();

        socketInitiator.createDynamicSession(sessionId);

        this.session = Session.lookupSession(sessionId);

        final CountDownLatch latch = new CountDownLatch(1);

        this.session.addStateListener(new NoopSessionStateListener() {

            @Override
            public void onDisconnect() {
                latch.countDown();
            }

            @Override
            public void onLogon() {
                latch.countDown();
            }

        });

        if (!this.session.isLoggedOn()) {
            latch.await(30, TimeUnit.SECONDS);
        }

        if (!this.session.isLoggedOn()) {
            Assert.fail("Session logon failed");
        }
    }

    @After
    public void shutDown() throws Exception {

        if (this.session != null) {
            if (this.session.isLoggedOn()) {
                this.session.logout("Testing");
            }
            this.session.close();
            this.session = null;
        }
        if (this.socketInitiator != null) {
            this.socketInitiator.stop();
            this.socketInitiator = null;
        }
    }

    @Test
    public void testMarketOrder() throws Exception {

        String orderId = Long.toHexString(System.currentTimeMillis());

        NewOrderSingle orderSingle = new NewOrderSingle();
        orderSingle.set(new ClOrdID(orderId));
        orderSingle.set(new SecurityID("4001"));
        orderSingle.set(new SecurityIDSource("8"));
        orderSingle.set(new Side(Side.BUY));
        orderSingle.set(new TransactTime(new Date()));
        orderSingle.set(new OrderQty(10.0d));
        orderSingle.set(new OrdType(OrdType.MARKET));

        SecurityFamily family = new SecurityFamilyImpl();
        family.setCurrency(Currency.USD);
        family.setScale(3);

        Forex forex = new ForexImpl();
        forex.setSymbol("EUR.USD");
        forex.setBaseCurrency(Currency.EUR);
        forex.setSecurityFamily(family);

        MarketOrder order = new MarketOrderImpl();
        order.setSecurity(forex);

        Mockito.when(lookupService.getOpenOrderByRootIntId(orderId)).thenReturn(order);

        session.send(orderSingle);

        Object event1 = eventQueue.poll(20, TimeUnit.SECONDS);
        Assert.assertTrue(event1 instanceof OrderStatus);
        OrderStatus orderStatus1 = (OrderStatus) event1;
        Assert.assertEquals(orderId, orderStatus1.getIntId());
        Assert.assertNotNull(orderStatus1.getExtId());
        Assert.assertEquals(Status.SUBMITTED, orderStatus1.getStatus());
        Assert.assertSame(order, orderStatus1.getOrder());
        Assert.assertEquals(0, orderStatus1.getFilledQuantity());

        Object event2 = eventQueue.poll(20, TimeUnit.SECONDS);
        Assert.assertTrue(event2 instanceof OrderStatus);
        OrderStatus orderStatus2 = (OrderStatus) event2;
        Assert.assertEquals(orderId, orderStatus2.getIntId());
        Assert.assertNotNull(orderStatus2.getExtId());
        Assert.assertEquals(Status.EXECUTED, orderStatus2.getStatus());
        Assert.assertSame(order, orderStatus2.getOrder());
        Assert.assertEquals(100000, orderStatus2.getFilledQuantity());

        Object event3 = eventQueue.poll(20, TimeUnit.SECONDS);
        Assert.assertTrue(event3 instanceof Fill);
        Fill fill1 = (Fill) event3;
        Assert.assertEquals(orderStatus2.getExtId(), fill1.getExtId());
        Assert.assertSame(order, fill1.getOrder());
        Assert.assertNotNull(fill1.getExtDateTime());
        Assert.assertEquals(ch.algotrader.enumeration.Side.BUY, fill1.getSide());
        Assert.assertEquals(100000L, fill1.getQuantity());
        Assert.assertNotNull(fill1.getPrice());

        Object event4 = eventQueue.poll(5, TimeUnit.SECONDS);
        Assert.assertNull(event4);
    }

    @Test
    public void testInvalidOrder() throws Exception {

        String orderId = Long.toHexString(System.currentTimeMillis());

        NewOrderSingle orderSingle = new NewOrderSingle();
        orderSingle.set(new ClOrdID(orderId));
        orderSingle.set(new SecurityID("99999"));
        orderSingle.set(new SecurityIDSource("8"));
        orderSingle.set(new Side(Side.BUY));
        orderSingle.set(new TransactTime(new Date()));
        orderSingle.set(new OrderQty(10.0d));
        orderSingle.set(new OrdType(OrdType.MARKET));

        session.send(orderSingle);

        Object event4 = eventQueue.poll(5, TimeUnit.SECONDS);
        Assert.assertNull(event4);
    }

    @Test
    public void testStopOrderCancel() throws Exception {

        String orderId1 = Long.toHexString(System.currentTimeMillis());

        NewOrderSingle orderSingle = new NewOrderSingle();
        orderSingle.set(new ClOrdID(orderId1));
        orderSingle.set(new SecurityID("4001"));
        orderSingle.set(new SecurityIDSource("8"));
        orderSingle.set(new Side(Side.SELL));
        orderSingle.set(new TransactTime(new Date()));
        orderSingle.set(new OrderQty(10.0d));
        orderSingle.set(new OrdType(OrdType.STOP));
        orderSingle.set(new StopPx(0.5d));

        SecurityFamily family = new SecurityFamilyImpl();
        family.setCurrency(Currency.USD);
        family.setScale(3);

        Forex forex = new ForexImpl();
        forex.setSymbol("EUR.USD");
        forex.setBaseCurrency(Currency.EUR);
        forex.setSecurityFamily(family);

        MarketOrder order = new MarketOrderImpl();
        order.setSecurity(forex);

        Mockito.when(lookupService.getOpenOrderByRootIntId(orderId1)).thenReturn(order);

        session.send(orderSingle);

        Object event1 = eventQueue.poll(20, TimeUnit.SECONDS);
        Assert.assertTrue(event1 instanceof OrderStatus);
        OrderStatus orderStatus1 = (OrderStatus) event1;
        Assert.assertEquals(orderId1, orderStatus1.getIntId());
        Assert.assertNotNull(orderStatus1.getExtId());
        Assert.assertEquals(Status.SUBMITTED, orderStatus1.getStatus());
        Assert.assertSame(order, orderStatus1.getOrder());
        Assert.assertEquals(0, orderStatus1.getFilledQuantity());

        String orderId2 = Long.toHexString(System.currentTimeMillis());

        OrderCancelRequest cancelRequest = new OrderCancelRequest();
        cancelRequest.set(new OrigClOrdID(orderId1));
        cancelRequest.set(new ClOrdID(orderId2));
        cancelRequest.set(new SecurityID("4001"));
        cancelRequest.set(new SecurityIDSource("8"));
        cancelRequest.set(new Side(Side.SELL));
        cancelRequest.set(new TransactTime(new Date()));
        cancelRequest.set(new OrderQty(10.0d));

        Mockito.when(lookupService.getOpenOrderByRootIntId(orderId2)).thenReturn(order);

        session.send(cancelRequest);

        Object event2 = eventQueue.poll(20, TimeUnit.SECONDS);

        Assert.assertTrue(event2 instanceof OrderStatus);
        OrderStatus orderStatus2 = (OrderStatus) event2;
        Assert.assertEquals(orderId2, orderStatus2.getIntId());
        Assert.assertNotNull(orderStatus2.getExtId());
        Assert.assertEquals(Status.CANCELED, orderStatus2.getStatus());
        Assert.assertSame(order, orderStatus2.getOrder());
        Assert.assertEquals(0, orderStatus2.getFilledQuantity());
        Assert.assertEquals(100000, orderStatus2.getRemainingQuantity());

        Object event3 = eventQueue.poll(5, TimeUnit.SECONDS);
        Assert.assertNull(event3);
    }

}
