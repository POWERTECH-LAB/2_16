package ch.algotrader.adapter.dc;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import quickfix.CompositeLogFactory;
import quickfix.DefaultSessionFactory;
import quickfix.LogFactory;
import quickfix.MemoryStoreFactory;
import quickfix.SLF4JLogFactory;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.SocketInitiator;
import quickfix.field.SubscriptionRequestType;
import quickfix.fix44.MarketDataRequest;
import ch.algotrader.adapter.fix.DefaultFixApplication;
import ch.algotrader.adapter.fix.DefaultFixSessionLifecycle;
import ch.algotrader.adapter.fix.NoopSessionStateListener;
import ch.algotrader.entity.security.Forex;
import ch.algotrader.entity.security.ForexImpl;
import ch.algotrader.entity.security.SecurityFamily;
import ch.algotrader.entity.security.SecurityFamilyImpl;
import ch.algotrader.entity.strategy.StrategyImpl;
import ch.algotrader.enumeration.Currency;
import ch.algotrader.esper.AbstractEngine;
import ch.algotrader.esper.EngineLocator;
import ch.algotrader.vo.AskVO;
import ch.algotrader.vo.BidVO;

public class DCFixFeedMessageHandlerTest {

    private LinkedBlockingQueue<Object> eventQueue;
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

        SessionSettings settings;
        ClassLoader cl = DCFixFeedMessageHandlerTest.class.getClassLoader();
        InputStream instream = cl.getResourceAsStream("dcf-test.cfg");
        Assert.assertNotNull(instream);
        try {
            settings = new SessionSettings(instream);
        } finally {
            instream.close();
        }
        SessionID sessionId = settings.sectionIterator().next();

        DCLogonMessageHandler dcLogonHandler = new DCLogonMessageHandler();
        dcLogonHandler.setSettings(settings);

        DefaultFixApplication fixApplication = new DefaultFixApplication(sessionId, new DCFixMarketDataMessageHandler(), dcLogonHandler, new DefaultFixSessionLifecycle());

//        Log4FIX log4Fix = Log4FIX.createForLiveUpdates(settings);
//        LogFactory logFactory = new CompositeLogFactory(new LogFactory[] { new SLF4JLogFactory(settings), log4Fix.getLogFactory() });
//        log4Fix.show();

        LogFactory logFactory = new CompositeLogFactory(new LogFactory[] { new SLF4JLogFactory(settings) });

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
    public void testMarketDataFeed() throws Exception {

        SecurityFamily family = new SecurityFamilyImpl();
        family.setCurrency(Currency.USD);

        Forex forex = new ForexImpl();
        forex.setSymbol("EUR.USD");
        forex.setBaseCurrency(Currency.EUR);
        forex.setSecurityFamily(family);

        DCFixMarketDataRequestFactory requestFactory = new DCFixMarketDataRequestFactory();
        MarketDataRequest request = requestFactory.create(forex, new SubscriptionRequestType(SubscriptionRequestType.SNAPSHOT_PLUS_UPDATES));

        this.session.send(request);

        String tickerId = DCUtil.getDCSymbol(forex);

        for (int i = 0; i < 10; i++) {

            Object event = this.eventQueue.poll(30, TimeUnit.SECONDS);
            if (event == null) {
                Assert.fail("No event received within specific time limit");
            }

            if (event instanceof BidVO) {
                BidVO bid = (BidVO) event;
                Assert.assertEquals(tickerId, bid.getTickerId());
            } else if (event instanceof AskVO) {
                AskVO ask = (AskVO) event;
                Assert.assertEquals(tickerId, ask.getTickerId());
            } else {
                Assert.fail("Unexpected event type: " + event.getClass());
            }
        }
    }

}
