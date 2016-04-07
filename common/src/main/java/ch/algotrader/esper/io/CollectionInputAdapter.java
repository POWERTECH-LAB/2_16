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
package ch.algotrader.esper.io;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.beanutils.PropertyUtils;

import com.espertech.esper.adapter.AdapterState;
import com.espertech.esper.client.EPException;
import com.espertech.esperio.AbstractCoordinatedAdapter;
import com.espertech.esperio.SendableEvent;

/**
 * A {@link com.espertech.esperio.CoordinatedAdapter} used to input arbitraty Collections.
 * The specified {@code timeStampColumn} is used.
 *
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 */
public class CollectionInputAdapter extends AbstractCoordinatedAdapter {

    private final Iterator<?> iterator;
    private final String timeStampColumn;

    public CollectionInputAdapter(Collection<?> baseObjects, String timeStampColumn) {

        super(null, true, true, true);

        this.iterator = baseObjects.iterator();
        this.timeStampColumn = timeStampColumn;
    }

    @Override
    protected void close() {
        //do nothing
    }

    @Override
    protected void replaceFirstEventToSend() {
        this.eventsToSend.remove(this.eventsToSend.first());
        SendableEvent event = read();
        if (event != null) {
            this.eventsToSend.add(event);
        }
    }

    @Override
    protected void reset() {
        // do nothing
    }

    @Override
    public SendableEvent read() throws EPException {
        if (this.stateManager.getState() == AdapterState.DESTROYED) {
            return null;
        }

        if (this.eventsToSend.isEmpty()) {

            if (this.iterator.hasNext()) {

                try {
                    Object baseObject = this.iterator.next();
                    Date date = (Date) PropertyUtils.getProperty(baseObject, this.timeStampColumn);

                    if (date == null) {
                        throw new IllegalStateException("missing time stamp");
                    }

                    return new SendableBaseObjectEvent(baseObject, date.getTime(), this.scheduleSlot);
                } catch (Exception e) {
                    throw new EPException("problem getting timestamp column", e);
                }

            } else {
                if (this.stateManager.getState() == AdapterState.STARTED) {
                    stop();
                } else {
                    destroy();
                }
                return null;
            }
        } else {
            SendableEvent event = this.eventsToSend.first();
            this.eventsToSend.remove(event);
            return event;
        }
    }
}
