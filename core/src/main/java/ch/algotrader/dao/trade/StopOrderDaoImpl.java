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
package ch.algotrader.dao.trade;

import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;

import ch.algotrader.entity.trade.StopOrder;
import ch.algotrader.entity.trade.StopOrderImpl;
import ch.algotrader.hibernate.AbstractDao;

/**
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
@Repository // Required for exception translation
public class StopOrderDaoImpl extends AbstractDao<StopOrder> implements StopOrderDao {

    public StopOrderDaoImpl(final SessionFactory sessionFactory) {

        super(StopOrderImpl.class, sessionFactory);
    }

}
