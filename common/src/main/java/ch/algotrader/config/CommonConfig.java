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
package ch.algotrader.config;

import java.io.File;
import java.math.BigDecimal;

import ch.algotrader.entity.strategy.StrategyImpl;
import ch.algotrader.enumeration.Currency;
import ch.algotrader.enumeration.Duration;
import ch.algotrader.enumeration.MarketDataType;

/**
 * Algotrader common configuration object.
 *
 * @author <a href="mailto:okalnichevski@algotrader.ch">Oleg Kalnichevski</a>
 *
 * @version $Revision$ $Date$
 */
public final class CommonConfig {

    private final String strategyName;
    private final String dataSet;
    private final MarketDataType dataSetType;
    private final File dataSetLocation;
    private final Duration barSize;
    private final boolean feedCSV;
    private final boolean feedDB;
    private final boolean feedGenericEvents;
    private final boolean feedAllMarketDataFiles;
    private final int feedBatchSize;
    private final boolean simulation;
    private final BigDecimal simulationInitialBalance;
    private final boolean simulationLogTransactions;
    private final boolean singleVM;
    private final int portfolioDigits;
    private final Currency portfolioBaseCurrency;
    private final BigDecimal initialMarginMarkup;
    private final boolean validateCrossedSpread;
    private final boolean displayClosedPositions;

    public CommonConfig(
            @ConfigName("strategyName") final String strategyName,
            @ConfigName("dataSource.dataSet") final String dataSet,
            @ConfigName("dataSource.dataSetType") final MarketDataType dataSetType,
            @ConfigName(value = "dataSource.dataSetLocation", optional = true) final File dataSetLocation,
            @ConfigName("dataSource.barSize") final Duration barSize,
            @ConfigName("dataSource.feedCSV") final boolean feedCSV,
            @ConfigName("dataSource.feedDB") final boolean feedDB,
            @ConfigName("dataSource.feedGenericEvents") final boolean feedGenericEvents,
            @ConfigName("dataSource.feedAllMarketDataFiles") final boolean feedAllMarketDataFiles,
            @ConfigName("dataSource.feedBatchSize") final int feedBatchSize,
            @ConfigName("simulation") final boolean simulation,
            @ConfigName("simulation.initialBalance") final BigDecimal simulationInitialBalance,
            @ConfigName("simulation.logTransactions") final boolean simulationLogTransactions,
            @ConfigName("misc.singleVM") final boolean singleVM,
            @ConfigName("misc.portfolioDigits") final int portfolioDigits,
            @ConfigName("misc.portfolioBaseCurrency") final Currency portfolioBaseCurrency,
            @ConfigName("misc.initialMarginMarkup") final BigDecimal initialMarginMarkup,
            @ConfigName("misc.validateCrossedSpread") final boolean validateCrossedSpread,
            @ConfigName("misc.displayClosedPositions") final boolean displayClosedPositions) {
        this.strategyName = strategyName;
        this.dataSet = dataSet;
        this.dataSetType = dataSetType;
        this.dataSetLocation = dataSetLocation;
        this.barSize = barSize;
        this.feedCSV = feedCSV;
        this.feedDB = feedDB;
        this.feedGenericEvents = feedGenericEvents;
        this.feedAllMarketDataFiles = feedAllMarketDataFiles;
        this.feedBatchSize = feedBatchSize;
        this.simulation = simulation;
        this.simulationInitialBalance = simulationInitialBalance;
        this.simulationLogTransactions = simulationLogTransactions;
        this.singleVM = singleVM;
        this.portfolioDigits = portfolioDigits;
        this.portfolioBaseCurrency = portfolioBaseCurrency;
        this.initialMarginMarkup = initialMarginMarkup;
        this.validateCrossedSpread = validateCrossedSpread;
        this.displayClosedPositions = displayClosedPositions;
    }

    public String getStrategyName() {
        return this.strategyName;
    }

    public String getDataSet() {
        return this.dataSet;
    }

    public MarketDataType getDataSetType() {
        return this.dataSetType;
    }

    public File getDataSetLocation() {
        return this.dataSetLocation;
    }

    public Duration getBarSize() {
        return this.barSize;
    }

    public boolean isFeedCSV() {
        return this.feedCSV;
    }

    public boolean isFeedDB() {
        return this.feedDB;
    }

    public boolean isFeedGenericEvents() {
        return this.feedGenericEvents;
    }

    public boolean isFeedAllMarketDataFiles() {
        return this.feedAllMarketDataFiles;
    }

    public int getFeedBatchSize() {
        return this.feedBatchSize;
    }

    public boolean isSimulation() {
        return this.simulation;
    }

    public BigDecimal getSimulationInitialBalance() {
        return this.simulationInitialBalance;
    }

    public boolean isSimulationLogTransactions() {
        return this.simulationLogTransactions;
    }

    public boolean isSingleVM() {
        return this.singleVM;
    }

    public int getPortfolioDigits() {
        return this.portfolioDigits;
    }

    public Currency getPortfolioBaseCurrency() {
        return this.portfolioBaseCurrency;
    }

    public BigDecimal getInitialMarginMarkup() {
        return this.initialMarginMarkup;
    }

    public boolean isValidateCrossedSpread() {
        return this.validateCrossedSpread;
    }

    public boolean isDisplayClosedPositions() {
        return this.displayClosedPositions;
    }

    public String getStartedStrategyName() {
        if (this.simulation) {
            return StrategyImpl.SERVER;
        } else {
            return this.strategyName;
        }
    }

    public boolean isStartedStrategySERVER() {
        return StrategyImpl.SERVER.equals(getStartedStrategyName());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("[");
        sb.append("strategyName='").append(this.strategyName).append('\'');
        sb.append(", dataSet='").append(this.dataSet).append('\'');
        sb.append(", dataSetType=").append(this.dataSetType);
        sb.append(", dataSetLocation=").append(this.dataSetLocation);
        sb.append(", barSize=").append(this.barSize);
        sb.append(", feedCSV=").append(this.feedCSV);
        sb.append(", feedDB=").append(this.feedDB);
        sb.append(", feedGenericEvents=").append(this.feedGenericEvents);
        sb.append(", feedAllMarketDataFiles=").append(this.feedAllMarketDataFiles);
        sb.append(", feedBatchSize=").append(this.feedBatchSize);
        sb.append(", simulation=").append(this.simulation);
        sb.append(", simulationInitialBalance=").append(this.simulationInitialBalance);
        sb.append(", simulationLogTransactions=").append(this.simulationLogTransactions);
        sb.append(", singleVM=").append(this.singleVM);
        sb.append(", portfolioDigits=").append(this.portfolioDigits);
        sb.append(", portfolioBaseCurrency=").append(this.portfolioBaseCurrency);
        sb.append(", initialMarginMarkup=").append(this.initialMarginMarkup);
        sb.append(", validateCrossedSpread=").append(this.validateCrossedSpread);
        sb.append(", displayClosedPositions=").append(this.displayClosedPositions);
        sb.append(']');
        return sb.toString();
    }

}
