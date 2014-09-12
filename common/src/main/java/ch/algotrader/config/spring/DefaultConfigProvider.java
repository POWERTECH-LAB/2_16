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
package ch.algotrader.config.spring;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.Assert;

import ch.algotrader.config.ConfigProvider;

/**
 * Basic {@link ch.algotrader.config.ConfigProvider} implementation based on
 * Spring {@link ConversionService} and backed by a simple {@link java.util.Map}.
 *
 * @author <a href="mailto:okalnichevski@algotrader.ch">Oleg Kalnichevski</a>
 *
 * @version $Revision$ $Date$
 */
public class DefaultConfigProvider implements ConfigProvider {

    private final ConcurrentHashMap<String, ?> paramMap;
    private final ConversionService conversionService;

    static ConversionService createDefaultConversionService() {
        DefaultConversionService conversionService = new DefaultConversionService();
        conversionService.addConverter(new StringToDateConverter());
        conversionService.addConverter(Date.class, String.class, new ObjectToStringConverter());
        return conversionService;
    }

    public DefaultConfigProvider(
            final Map<String, ?> paramMap,
            final ConversionService conversionService) {
        Assert.notNull(paramMap, "ParamMap is null");
        Assert.notNull(conversionService, "ConversionService is null");
        this.paramMap = new ConcurrentHashMap<String, Object>(paramMap);
        this.conversionService = conversionService;
    }

    public DefaultConfigProvider(final Map<String, ?> paramMap) {
        this(paramMap, createDefaultConversionService());
    }

    protected Object getRawValue(final String name) {

        return this.paramMap.get(name);
    }

    public final <T> T getParameter(final String name, final Class<T> clazz) throws ClassCastException {

        Object param = getRawValue(name);
        if (param == null) {

            return null;
        }
        if (param instanceof String && StringUtils.isBlank((String) param)) {

            return null;
        }
        if (this.conversionService.canConvert(param.getClass(), clazz)) {

            return this.conversionService.convert(param, clazz);
        } else {

            // Attempt direct cast as the last resort
            return clazz.cast(param);
        }
    }

    @Override
    public Set<String> getNames() {
        return new HashSet<String>(this.paramMap.keySet());
    }

    @Override
    public String toString() {
        return paramMap.toString();
    }

}
