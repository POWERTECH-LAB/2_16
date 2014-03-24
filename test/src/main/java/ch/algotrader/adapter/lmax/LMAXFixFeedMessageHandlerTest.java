package ch.algotrader.adapter.lmax;

import java.io.InputStream;
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
import quickfix.DefaultSessionFactory;
import quickfix.LogFactory;
import quickfix.MemoryStoreFactory;
import quickfix.ScreenLogFactory;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.SocketInitiator;
import quickfix.field.MDEntryType;
import quickfix.field.MDReqID;
import quickfix.field.MDUpdateType;
import quickfix.field.MarketDepth;
import quickfix.field.SecurityID;
import quickfix.field.SecurityIDSource;
import quickfix.field.SubscriptionRequestType;
import quickfix.fix44.MarketDataRequest;
import quickfix.fix44.MarketDataSnapshotFullRefresh;

public class LMAXFixFeedMessageHandlerTest {

    private LinkedBlockingQueue<Object> eventQueue;
    private LMAXInstrumentCodeMapper mapper;
    private LMAXFixMarketDataMessageHandler messageHandler;
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
        ClassLoader cl = LMAXFixFeedMessageHandlerTest.class.getClassLoader();
        InputStream instream = cl.getResourceAsStream("lmf-test.cfg");
        Assert.assertNotNull(instream);
        try {
            settings = new SessionSettings(instream);
        } finally {
            instream.close();
        }
        SessionID sessionId = settings.sectionIterator().next();

        LMAXLogonMessageHandler logonHandler = new LMAXLogonMessageHandler();
        logonHandler.setSettings(settings);

        this.mapper = LMAXInstrumentCodeMapper.load();
        this.messageHandler = Mockito.spy(new LMAXFixMarketDataMessageHandler(mapper));

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
    public void testMarketDataFeed() throws Exception {

        SecurityFamily family = new SecurityFamilyImpl();
        family.setCurrency(Currency.USD);

        Forex forex = new ForexImpl();
        forex.setSymbol("EUR.USD");
        forex.setBaseCurrency(Currency.EUR);
        forex.setSecurityFamily(family);

        LMAXFixMarketDataRequestFactory requestFactory = new LMAXFixMarketDataRequestFactory(mapper);
        MarketDataRequest request = requestFactory.create(forex, new SubscriptionRequestType(SubscriptionRequestType.SNAPSHOT_PLUS_UPDATES));

        session.send(request);

        String symbol = LMAXUtil.createSymbol(forex);
        int tickerId = LMAXUtil.createTickerId(symbol);

        for (int i = 0; i < 10; i++) {

            Object event = eventQueue.poll(30, TimeUnit.SECONDS);
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

        Mockito.verify(messageHandler, Mockito.times(5)).onMessage(Mockito.<MarketDataSnapshotFullRefresh>any(), Mockito.eq(session.getSessionID()));
    }

    @Test
    public void testMarketDataInvalidRequest() throws Exception {

        MarketDataRequest request = new MarketDataRequest();
        request.set(new MDReqID("stuff"));
        request.set(new SubscriptionRequestType(SubscriptionRequestType.SNAPSHOT_PLUS_UPDATES));
        request.set(new MarketDepth(1));
        request.set(new MDUpdateType(MDUpdateType.FULL_REFRESH));

        MarketDataRequest.NoMDEntryTypes bid = new MarketDataRequest.NoMDEntryTypes();
        bid.set(new MDEntryType(MDEntryType.BID));
        request.addGroup(bid);

        MarketDataRequest.NoMDEntryTypes offer = new MarketDataRequest.NoMDEntryTypes();
        offer.set(new MDEntryType(MDEntryType.OFFER));
        request.addGroup(offer);

        MarketDataRequest.NoRelatedSym symGroup = new MarketDataRequest.NoRelatedSym();
        symGroup.set(new SecurityID("1234567890"));
        symGroup.set(new SecurityIDSource("8"));
        request.addGroup(symGroup);

        session.send(request);

        Object event = eventQueue.poll(5, TimeUnit.SECONDS);
        Assert.assertNull(event);

        Mockito.verify(messageHandler, Mockito.never()).onMessage(Mockito.<MarketDataSnapshotFullRefresh>any(), Mockito.eq(session.getSessionID()));
    }

}
