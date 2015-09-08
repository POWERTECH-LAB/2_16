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

package ch.algotrader.adapter.ib;

import java.util.concurrent.atomic.AtomicLong;

import ch.algotrader.entity.trade.Order;
import ch.algotrader.enumeration.Status;

/**
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class IBOrderStatus {

    private final static AtomicLong COUNT = new AtomicLong(0);

    private final Status status;
    private final long filledQuantity;
    private final long remainingQuantity;
    private final double avgFillPrice;
    private final double lastFillPrice;
    private final String extId;
    private final Order order;
    private final String reason;
    private final long sequenceNumber;

    public IBOrderStatus(Status status, long filledQuantity, long remainingQuantity, String extId, Order order) {
        this.status = status;
        this.filledQuantity = filledQuantity;
        this.remainingQuantity = remainingQuantity;
        this.avgFillPrice = 0.0;
        this.lastFillPrice = 0.0;
        this.extId = extId;
        this.order = order;
        this.reason = null;
        this.sequenceNumber = COUNT.getAndIncrement();
    }

    public IBOrderStatus(Status status, long filledQuantity, long remainingQuantity, String extId, Order order, String reason) {
        this.status = status;
        this.filledQuantity = filledQuantity;
        this.remainingQuantity = remainingQuantity;
        this.avgFillPrice = 0.0;
        this.lastFillPrice = 0.0;
        this.extId = extId;
        this.order = order;
        this.reason = reason;
        this.sequenceNumber = COUNT.getAndIncrement();
    }

    public IBOrderStatus(Status status, long filledQuantity, long remainingQuantity, double avgFillPrice, double lastFillPrice, String extId, Order order) {
        this.status = status;
        this.filledQuantity = filledQuantity;
        this.remainingQuantity = remainingQuantity;
        this.avgFillPrice = avgFillPrice;
        this.lastFillPrice = lastFillPrice;
        this.extId = extId;
        this.order = order;
        this.reason = null;
        this.sequenceNumber = COUNT.getAndIncrement();
    }

    public Status getStatus() {
        return this.status;
    }

    public long getFilledQuantity() {
        return this.filledQuantity;
    }

    public long getRemainingQuantity() {
        return this.remainingQuantity;
    }

    public double getAvgFillPrice() {
        return this.avgFillPrice;
    }

    public double getLastFillPrice() {
        return this.lastFillPrice;
    }

    public String getExtId() {
        return this.extId;
    }

    public Order getOrder() {
        return this.order;
    }

    public String getReason() {
        return this.reason;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    @Override
    public String toString() {

        StringBuilder buffer = new StringBuilder();
        buffer.append(getStatus());
        if (getOrder() != null) {
            buffer.append(",");
            buffer.append(getOrder().getDescription());
        }
        buffer.append(",filledQuantity=");
        buffer.append(getFilledQuantity());
        buffer.append(",remainingQuantity=");
        buffer.append(getRemainingQuantity());
        buffer.append(",avgFillPrice=");
        buffer.append(getAvgFillPrice());
        buffer.append(",lastFillPrice=");
        buffer.append(getLastFillPrice());
        if (getReason() != null) {
            buffer.append(",reason=");
            buffer.append(getReason());
        }

        return buffer.toString();
    }

}
