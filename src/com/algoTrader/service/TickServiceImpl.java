package com.algoTrader.service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.supercsv.exception.SuperCSVException;

import com.algoTrader.entity.Security;
import com.algoTrader.entity.StockOption;
import com.algoTrader.entity.Tick;
import com.algoTrader.util.DateUtil;
import com.algoTrader.util.EsperService;
import com.algoTrader.util.MyLogger;
import com.algoTrader.util.PropertiesUtil;
import com.algoTrader.util.csv.CsvTickReader;
import com.algoTrader.util.csv.CsvTickWriter;

public abstract class TickServiceImpl extends TickServiceBase {

    private static Logger logger = MyLogger.getLogger(TickServiceImpl.class.getName());
    private static String dataSet = PropertiesUtil.getProperty("strategie.dataSet");

    private Map<Security, CsvTickWriter> csvWriters = new HashMap<Security, CsvTickWriter>();

    @SuppressWarnings("unchecked")
    protected void handleProcessSecuritiesOnWatchlist() throws SuperCSVException, IOException  {

        List<Security> securities = getSecurityDao().findSecuritiesOnWatchlist();
        for (Security security : securities) {

            // retrieve ticks only between marketOpen & close
            if (DateUtil.compareToTime(security.getMarketOpen()) >= 0 &&
                DateUtil.compareToTime(security.getMarketClose()) <= 0) {

                Tick tick = retrieveTick(security);

                // if we hit a timeout, we get null
                if (tick != null) {

                    try {
                        tick.validate();
                        EsperService.sendEvent(tick);
                    } catch (Exception e) {
                        // do nothing, just ignore invalideTicks
                    }

                    // write the tick to file (even if not valid)
                    CsvTickWriter csvWriter;
                    if (this.csvWriters.containsKey(security)) {
                        csvWriter = this.csvWriters.get(security);
                    } else {
                        csvWriter = new CsvTickWriter(security.getIsin());
                        this.csvWriters.put(security, csvWriter);
                    }
                    csvWriter.write(tick);
                }
            }
        }
    }

    protected void handlePutOnWatchlist(int stockOptionId) throws Exception {

        StockOption stockOption = (StockOption)getStockOptionDao().load(stockOptionId);
        putOnWatchlist(stockOption);
    }

    @SuppressWarnings("unchecked")
    protected void handlePutOnWatchlist(StockOption stockOption) throws Exception {

        if (!stockOption.isOnWatchlist()) {

            putOnExternalWatchlist(stockOption);

            stockOption.setOnWatchlist(true);
            getStockOptionDao().update(stockOption);
            getStockOptionDao().getStockOptionsOnWatchlist(false).add(stockOption);

            logger.info("put stockOption on watchlist " + stockOption.getSymbol());
        }
    }

    protected void handleRemoveFromWatchlist(int stockOptionId) throws Exception {

        StockOption stockOption = (StockOption)getStockOptionDao().load(stockOptionId);
        removeFromWatchlist(stockOption);
    }

    protected void handleRemoveFromWatchlist(StockOption stockOption) throws Exception {

        if (stockOption.isOnWatchlist()) {

            removeFromExternalWatchlist(stockOption);

            stockOption.setOnWatchlist(false);
            getStockOptionDao().update(stockOption);
            getStockOptionDao().getStockOptionsOnWatchlist(false).remove(stockOption);

            logger.info("removed stockOption from watchlist " + stockOption.getSymbol());
        }
    }

    /**
     * must be run with simulation=false (to get correct values for bid, ask and settlement)
     * also recommended to turn of ehache on commandline (to avoid out of memory error)
     */
    protected void handleImportTicks() throws Exception {

        File directory = new File("results/tickdata/" + dataSet);
        for (File file : directory.listFiles()) {

            String isin = file.getName().split("\\.")[0];

            Security security = getSecurityDao().findByISIN(isin);
            CsvTickReader reader = new CsvTickReader(isin);

            Tick tick;
            List<Tick> ticks = new ArrayList<Tick>();
            while ((tick = reader.readTick()) != null) {

                if (tick.getLast().equals(new BigDecimal(0)))
                    tick.setLast(null);

                tick.setSecurity(security);
                ticks.add(tick);

            }
            getTickDao().create(ticks);
            System.out.println("imported ticks for: " + isin);
            System.gc();
        }
    }
}
