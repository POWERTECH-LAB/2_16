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
package ch.algotrader.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * A ValueObject representing a {@link ch.algotrader.entity.strategy.PortfolioValue PortfolioValue}.
 */
public class PortfolioValueVO implements Serializable {

    private static final long serialVersionUID = 7104071653742687817L;

    /**
     * The dateTime of this PortfolioValueVO
     */
    private Date dateTime;

    /**
     * Current market value of all Assets.
     */
    private BigDecimal netLiqValue;

    /**
     * Current market value of all positions.
     */
    private BigDecimal securitiesCurrentValue;

    /**
     * Total cash
     */
    private BigDecimal cashBalance;

    /**
     * Current Maintenance Margin of all marginable positions
     */
    private BigDecimal maintenanceMargin;

    /**
     * Current (delta-adjusted) Notional Exposure
     */
    private double leverage;

    /**
     * boolean setter for primitive attribute, so we can tell if it's initialized
     */
    private boolean setLeverage = false;

    /**
     * Allocation assigned to the AlgoTrader Server / Strategy.
     */
    private double allocation;

    /**
     * boolean setter for primitive attribute, so we can tell if it's initialized
     */
    private boolean setAllocation = false;

    /**
     * The Performance at the specified {@code dateTime} since the beginning of the evaluation time series.
     */
    private double performance;

    /**
     * boolean setter for primitive attribute, so we can tell if it's initialized
     */
    private boolean setPerformance = false;

    /**
     * CashFlow value occurred at the specified time.
     */
    private BigDecimal cashFlow;

    /**
     * Default Constructor
     */
    public PortfolioValueVO() {

        // Documented empty block - avoid compiler warning - no super constructor
    }

    /**
     * Constructor taking only required properties
     * @param dateTimeIn Date The dateTime of this PortfolioValueVO
     * @param netLiqValueIn BigDecimal Current market value of all Assets.
     * @param securitiesCurrentValueIn BigDecimal Current market value of all positions.
     * @param cashBalanceIn BigDecimal Total cash
     * @param maintenanceMarginIn BigDecimal Current Maintenance Margin of all marginable positions
     * @param leverageIn double Current (delta-adjusted) Notional Exposure
     * @param allocationIn double Allocation assigned to the AlgoTrader Server / Strategy.
     * @param performanceIn double The Performance at the specified {@code dateTime} since the beginning of the evaluation time series.
     */
    public PortfolioValueVO(final Date dateTimeIn, final BigDecimal netLiqValueIn, final BigDecimal securitiesCurrentValueIn, final BigDecimal cashBalanceIn, final BigDecimal maintenanceMarginIn,
            final double leverageIn, final double allocationIn, final double performanceIn) {

        this.dateTime = dateTimeIn;
        this.netLiqValue = netLiqValueIn;
        this.securitiesCurrentValue = securitiesCurrentValueIn;
        this.cashBalance = cashBalanceIn;
        this.maintenanceMargin = maintenanceMarginIn;
        this.leverage = leverageIn;
        this.setLeverage = true;
        this.allocation = allocationIn;
        this.setAllocation = true;
        this.performance = performanceIn;
        this.setPerformance = true;
    }

    /**
     * Constructor with all properties
     * @param dateTimeIn Date
     * @param netLiqValueIn BigDecimal
     * @param securitiesCurrentValueIn BigDecimal
     * @param cashBalanceIn BigDecimal
     * @param maintenanceMarginIn BigDecimal
     * @param leverageIn double
     * @param allocationIn double
     * @param performanceIn double
     * @param cashFlowIn BigDecimal
     */
    public PortfolioValueVO(final Date dateTimeIn, final BigDecimal netLiqValueIn, final BigDecimal securitiesCurrentValueIn, final BigDecimal cashBalanceIn, final BigDecimal maintenanceMarginIn,
            final double leverageIn, final double allocationIn, final double performanceIn, final BigDecimal cashFlowIn) {

        this.dateTime = dateTimeIn;
        this.netLiqValue = netLiqValueIn;
        this.securitiesCurrentValue = securitiesCurrentValueIn;
        this.cashBalance = cashBalanceIn;
        this.maintenanceMargin = maintenanceMarginIn;
        this.leverage = leverageIn;
        this.setLeverage = true;
        this.allocation = allocationIn;
        this.setAllocation = true;
        this.performance = performanceIn;
        this.setPerformance = true;
        this.cashFlow = cashFlowIn;
    }

    /**
     * Copies constructor from other PortfolioValueVO
     *
     * @param otherBean Cannot be <code>null</code>
     * @throws NullPointerException if the argument is <code>null</code>
     */
    public PortfolioValueVO(final PortfolioValueVO otherBean) {

        this.dateTime = otherBean.getDateTime();
        this.netLiqValue = otherBean.getNetLiqValue();
        this.securitiesCurrentValue = otherBean.getSecuritiesCurrentValue();
        this.cashBalance = otherBean.getCashBalance();
        this.maintenanceMargin = otherBean.getMaintenanceMargin();
        this.leverage = otherBean.getLeverage();
        this.setLeverage = true;
        this.allocation = otherBean.getAllocation();
        this.setAllocation = true;
        this.performance = otherBean.getPerformance();
        this.setPerformance = true;
        this.cashFlow = otherBean.getCashFlow();
    }

    /**
     * The dateTime of this PortfolioValueVO
     * @return dateTime Date
     */
    public Date getDateTime() {

        return this.dateTime;
    }

    /**
     * The dateTime of this PortfolioValueVO
     * @param value Date
     */
    public void setDateTime(final Date value) {

        this.dateTime = value;
    }

    /**
     * Current market value of all Assets.
     * @return netLiqValue BigDecimal
     */
    public BigDecimal getNetLiqValue() {

        return this.netLiqValue;
    }

    /**
     * Current market value of all Assets.
     * @param value BigDecimal
     */
    public void setNetLiqValue(final BigDecimal value) {

        this.netLiqValue = value;
    }

    /**
     * Current market value of all positions.
     * @return securitiesCurrentValue BigDecimal
     */
    public BigDecimal getSecuritiesCurrentValue() {

        return this.securitiesCurrentValue;
    }

    /**
     * Current market value of all positions.
     * @param value BigDecimal
     */
    public void setSecuritiesCurrentValue(final BigDecimal value) {

        this.securitiesCurrentValue = value;
    }

    public BigDecimal getCashBalance() {

        return this.cashBalance;
    }

    public void setCashBalance(final BigDecimal value) {

        this.cashBalance = value;
    }

    /**
     * Current Maintenance Margin of all marginable positions
     * @return maintenanceMargin BigDecimal
     */
    public BigDecimal getMaintenanceMargin() {

        return this.maintenanceMargin;
    }

    /**
     * Current Maintenance Margin of all marginable positions
     * @param value BigDecimal
     */
    public void setMaintenanceMargin(final BigDecimal value) {

        this.maintenanceMargin = value;
    }

    /**
     * Current (delta-adjusted) Notional Exposure
     * @return leverage double
     */
    public double getLeverage() {

        return this.leverage;
    }

    /**
     * Current (delta-adjusted) Notional Exposure
     * @param value double
     */
    public void setLeverage(final double value) {

        this.leverage = value;
        this.setLeverage = true;
    }

    /**
     * Return true if the primitive attribute leverage is set, through the setter or constructor
     * @return true if the attribute value has been set
     */
    public boolean isSetLeverage() {

        return this.setLeverage;
    }

    /**
     * Allocation assigned to the AlgoTrader Server / Strategy.
     * @return allocation double
     */
    public double getAllocation() {

        return this.allocation;
    }

    /**
     * Allocation assigned to the AlgoTrader Server / Strategy.
     * @param value double
     */
    public void setAllocation(final double value) {

        this.allocation = value;
        this.setAllocation = true;
    }

    /**
     * Return true if the primitive attribute allocation is set, through the setter or constructor
     * @return true if the attribute value has been set
     */
    public boolean isSetAllocation() {

        return this.setAllocation;
    }

    /**
     * The Performance at the specified {@code dateTime} since the beginning of the evaluation time series.
     * @return performance double
     */
    public double getPerformance() {

        return this.performance;
    }

    /**
     * The Performance at the specified {@code dateTime} since the beginning of the evaluation time series.
     * @param value double
     */
    public void setPerformance(final double value) {

        this.performance = value;
        this.setPerformance = true;
    }

    /**
     * Return true if the primitive attribute performance is set, through the setter or constructor
     * @return true if the attribute value has been set
     */
    public boolean isSetPerformance() {

        return this.setPerformance;
    }

    /**
     * CashFlow value occurred at the specified time.
     * @return cashFlow BigDecimal
     */
    public BigDecimal getCashFlow() {

        return this.cashFlow;
    }

    /**
     * CashFlow value occurred at the specified time.
     * @param value BigDecimal
     */
    public void setCashFlow(final BigDecimal value) {

        this.cashFlow = value;
    }

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();
        builder.append("PortfolioValueVO [dateTime=");
        builder.append(this.dateTime);
        builder.append(", netLiqValue=");
        builder.append(this.netLiqValue);
        builder.append(", securitiesCurrentValue=");
        builder.append(this.securitiesCurrentValue);
        builder.append(", cashBalance=");
        builder.append(this.cashBalance);
        builder.append(", maintenanceMargin=");
        builder.append(this.maintenanceMargin);
        builder.append(", leverage=");
        builder.append(this.leverage);
        builder.append(", setLeverage=");
        builder.append(this.setLeverage);
        builder.append(", allocation=");
        builder.append(this.allocation);
        builder.append(", setAllocation=");
        builder.append(this.setAllocation);
        builder.append(", performance=");
        builder.append(this.performance);
        builder.append(", setPerformance=");
        builder.append(this.setPerformance);
        builder.append(", cashFlow=");
        builder.append(this.cashFlow);
        builder.append("]");

        return builder.toString();
    }

}
