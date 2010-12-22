package com.algoTrader.service.ib;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.algoTrader.entity.Security;
import com.algoTrader.entity.Tick;
import com.algoTrader.entity.TickImpl;
import com.algoTrader.service.HistoricalDataServiceException;
import com.algoTrader.util.ConfigurationUtil;
import com.algoTrader.util.DateUtil;
import com.algoTrader.util.MyLogger;
import com.algoTrader.util.RoundUtil;
import com.algoTrader.util.io.CsvTickWriter;
import com.ib.client.Contract;

public class IbHistoricalDataServiceImpl extends IbHistoricalDataServiceBase {

    private static Logger logger = MyLogger.getLogger(IbHistoricalDataServiceImpl.class.getName());

    private static boolean simulation = ConfigurationUtil.getBaseConfig().getBoolean("simulation");
    private static boolean ibEnabled = "IB".equals(ConfigurationUtil.getBaseConfig().getString("marketChannel"));
    private static boolean historicalDataServiceEnabled = ConfigurationUtil.getBaseConfig().getBoolean("ib.historicalDataServiceEnabled");

    private static int historicalDataTimeout = ConfigurationUtil.getBaseConfig().getInt("ib.historicalDataTimeout");

    private static SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd  HH:mm:ss");

    private DefaultClientSocket client;
    private DefaultWrapper wrapper;
    private Lock lock = new ReentrantLock();
    private Condition condition = this.lock.newCondition();

    private Map<Integer, Boolean> requestIdBooleanMap;
    private Map<Integer, Date> requestIdDateMap;
    private Map<Integer, String> requestIdWhatToShowMap;
    private NavigableMap<Date, Tick> dateTickMap;
    private Security security;

    private CsvTickWriter writer;
    private static int clientId = 3;

    protected void handleInit() throws Exception {

        if (!ibEnabled || simulation || !historicalDataServiceEnabled)
            return;

        this.wrapper = new DefaultWrapper(clientId) {

            @Override
            public void historicalData(int requestId, String dateString, double open, double high, double low, double close, int volume, int count, double WAP, boolean hasGaps) {

                IbHistoricalDataServiceImpl.this.lock.lock();
                try {

                    if (dateString.startsWith("finished")) {

                        IbHistoricalDataServiceImpl.this.requestIdBooleanMap.put(requestId, true);
                        IbHistoricalDataServiceImpl.this.condition.signalAll();

                        return;
                    }

                    Date date = format.parse(dateString);
                    Date requestedDate = IbHistoricalDataServiceImpl.this.requestIdDateMap.get(requestId);

                    // retrieve ticks only between marketOpen & close
                    if (DateUtil.compareTime(date, IbHistoricalDataServiceImpl.this.security.getSecurityFamily().getMarketClose()) > 0
                            || DateUtil.compareTime(date, IbHistoricalDataServiceImpl.this.security.getSecurityFamily().getMarketOpen()) < 0) {

                        return;
                    }

                    // only accept ticks within the last 24h
                    if (requestedDate.getTime() - date.getTime() > 86400000) {

                        return;
                    }

                    Tick tick = IbHistoricalDataServiceImpl.this.dateTickMap.get(date);
                    if (tick == null) {

                        tick = new TickImpl();
                        tick.setLast(new BigDecimal(0));
                        tick.setDateTime(date);
                        tick.setVol(0);
                        tick.setVolBid(0);
                        tick.setVolAsk(0);
                        tick.setBid(new BigDecimal(0));
                        tick.setAsk(new BigDecimal(0));
                        tick.setOpenIntrest(0);
                        tick.setSettlement(new BigDecimal(0));

                        IbHistoricalDataServiceImpl.this.dateTickMap.put(date, tick);
                    }

                    String whatToShow = IbHistoricalDataServiceImpl.this.requestIdWhatToShowMap.get(requestId);

                    if ("TRADES".equals(whatToShow)) {
                        Entry<Date, Tick> entry = IbHistoricalDataServiceImpl.this.dateTickMap.lowerEntry(date);
                        if (entry != null) {
                            Tick lastTick = entry.getValue();
                            if (volume > 0) {
                                tick.setVol(lastTick.getVol() + volume);
                                tick.setLastDateTime(date);
                            } else {
                                tick.setVol(lastTick.getVol());
                                tick.setLastDateTime(lastTick.getLastDateTime());
                            }
                        } else {
                            if (volume > 0) {
                                tick.setVol(volume);
                                tick.setLastDateTime(date);
                            }
                        }
                        BigDecimal last = RoundUtil.getBigDecimal(close);
                        tick.setLast(last);
                    } else if ("BID".equals(whatToShow)) {
                        BigDecimal bid = RoundUtil.getBigDecimal(close);
                        tick.setBid(bid);
                    } else if ("ASK".equals(whatToShow)) {
                        BigDecimal ask = RoundUtil.getBigDecimal(close);
                        tick.setAsk(ask);
                    }

                    String message = whatToShow +
                            " " + dateString +
                            " open=" + open +
                            " high=" + high +
                            " low=" + low +
                            " close=" + close +
                            " volume=" + volume +
                            " count=" + count +
                            " WAP=" + WAP +
                            " hasGaps=" + hasGaps;

                    if (hasGaps) {
                        logger.error(message, new HistoricalDataServiceException(message));
                    } else {
                        logger.debug(message);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    IbHistoricalDataServiceImpl.this.lock.unlock();
                }
            }

            public void connectionClosed() {

                super.connectionClosed();

                connect();
            }

            public void error(int requestId, int code, String errorMsg) {

                // 165 Historical data farm is connected
                // 162 Historical market data Service error message.
                // 2105 A historical data farm is disconnected.
                // 2107 A historical data farm connection has become inactive
                // but should be available upon demand.
                if (code == 162 || code == 165 || code == 2105 || code == 2106 || code == 2107) {

                    IbHistoricalDataServiceImpl.this.lock.lock();
                    try {

                        if (code == 2105 || code == 2107) {

                            super.error(requestId, code, errorMsg);
                            IbHistoricalDataServiceImpl.this.requestIdBooleanMap.put(requestId, false);
                        }

                        if (code == 162) {

                            logger.warn("HMDS query returned no data for " + IbHistoricalDataServiceImpl.this.requestIdDateMap.get(requestId));
                            IbHistoricalDataServiceImpl.this.requestIdBooleanMap.put(requestId, true);
                        }

                        IbHistoricalDataServiceImpl.this.condition.signalAll();
                    } finally {
                        IbHistoricalDataServiceImpl.this.lock.unlock();
                    }
                } else {
                    super.error(requestId, code, errorMsg);
                }
            }
        };

        this.client = new DefaultClientSocket(this.wrapper);

        connect();
    }

    protected void handleConnect() {

        this.requestIdBooleanMap = new HashMap<Integer, Boolean>();
        this.requestIdDateMap = new HashMap<Integer, Date>();
        this.requestIdWhatToShowMap = new HashMap<Integer, String>();

        this.client.connect(clientId);
    }

    protected void handleRequestHistoricalData(int[] securityIds, String[] whatToShow, String startDateString, String endDateString) throws Exception {


        Date startDate = format.parse(startDateString + "  24:00:00");
        Date endDate = format.parse(endDateString + "  24:00:00");

        for (int securityId : securityIds) {

            Security security = getSecurityDao().load(securityId);
            this.security = security;

            this.writer = new CsvTickWriter(security.getIsin());

            Contract contract = IbUtil.getContract(security);

            requestHistoricalDataForSecurity(contract, security, startDate, endDate, whatToShow);

        }
    }

    private void requestHistoricalDataForSecurity(Contract contract, Security security, Date startDate, Date endDate, String[] whatToShow) throws Exception {

        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(startDate);

        Date date;
        while ((date = cal.getTime()).compareTo(endDate) <= 0) {

            if ((cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) && (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY)) {

                requestHistoricalDataForDate(date, contract, security, whatToShow);
            }
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    private void requestHistoricalDataForDate(Date date, Contract contract, Security security, String[] whatToShow) throws Exception {

        this.dateTickMap = new TreeMap<Date, Tick>();

        // run all whatToShows and get the ticks
        for (String whatToShowNow : whatToShow) {
            requestHistoricalDataForWhatToShow(date, contract, whatToShowNow);

        }

        // writer the ticks to the csvWriter
        for (Map.Entry<Date, Tick> entry : this.dateTickMap.entrySet()) {

            Tick tick = entry.getValue();
            tick.setSecurity(security);
            this.writer.write(tick);
        }

        logger.debug("done for: " + security.getSymbol() + " on date: " + date);
    }

    private void requestHistoricalDataForWhatToShow(Date date, Contract contract, String whatToShow) throws Exception {

        while (true) {

            this.lock.lock();

            try {

                int requestId = RequestIdManager.getInstance().getNextRequestId();
                this.requestIdDateMap.put(requestId, date);
                this.requestIdWhatToShowMap.put(requestId, whatToShow);

                this.client.reqHistoricalData(requestId, contract, format.format(date), "1 D", "1 min", whatToShow, 1, 1);

                Boolean success;
                while ((success = this.requestIdBooleanMap.get(requestId)) == null) {
                    if (!this.condition.await(historicalDataTimeout, TimeUnit.SECONDS)) {
                        this.client.cancelHistoricalData(requestId);
                        continue;
                    }
                }

                // to make sure we don't get a pacing error
                Thread.sleep(10000);

                if (success)
                    break;

            } finally {
                this.lock.unlock();
            }
        }
    }
}
