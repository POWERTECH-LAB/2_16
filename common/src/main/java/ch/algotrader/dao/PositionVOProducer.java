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
package ch.algotrader.dao;

import java.math.BigDecimal;
import java.util.Map;

import org.apache.commons.lang.Validate;

import ch.algotrader.entity.Position;
import ch.algotrader.entity.marketData.MarketDataEventVO;
import ch.algotrader.entity.property.Property;
import ch.algotrader.service.LocalLookupService;
import ch.algotrader.util.RoundUtil;
import ch.algotrader.vo.client.PositionVO;

/**
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class PositionVOProducer implements EntityConverter<Position, PositionVO> {

    private final LocalLookupService localLookupService;

    public PositionVOProducer(final LocalLookupService localLookupService) {
        this.localLookupService = localLookupService;
    }

    @Override
    public PositionVO convert(final Position entity) {

        Validate.notNull(entity, "Position is null");

        MarketDataEventVO marketDataEvent = this.localLookupService.getCurrentMarketDataEvent(entity.getSecurity().getId());

        PositionVO vo = new PositionVO();

        vo.setId(entity.getId());
        vo.setQuantity(entity.getQuantity());
        // No conversion for target.strategy (can't convert source.getStrategy():Strategy to String)
        vo.setCost(new BigDecimal(entity.getCost()));
        vo.setRealizedPL(new BigDecimal(entity.getRealizedPL()));

        int scale = entity.getSecurity().getSecurityFamily().getScale();

        vo.setSecurityId(entity.getSecurity().getId());
        vo.setName(entity.getSecurity().toString());
        vo.setStrategy(entity.getStrategy().toString());
        vo.setCurrency(entity.getSecurity().getSecurityFamily().getCurrency());
        vo.setMarketPrice(RoundUtil.getBigDecimal(entity.getMarketPrice(marketDataEvent), scale));
        vo.setMarketValue(RoundUtil.getBigDecimal(entity.getMarketValue(marketDataEvent)));
        vo.setAveragePrice(RoundUtil.getBigDecimal(entity.getAveragePrice(), scale));
        vo.setCost(RoundUtil.getBigDecimal(entity.getCost()));
        vo.setUnrealizedPL(RoundUtil.getBigDecimal(entity.getUnrealizedPL(marketDataEvent)));
        vo.setRealizedPL(RoundUtil.getBigDecimal(entity.getRealizedPL()));

        // add properties if any
        Map<String, Property> properties = entity.getProps();
        if (!properties.isEmpty()) {
            vo.setProperties(properties);
        }

        return vo;
    }

}
