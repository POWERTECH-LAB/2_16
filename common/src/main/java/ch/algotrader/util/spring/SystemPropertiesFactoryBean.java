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
package ch.algotrader.util.spring;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.core.Constants;

/**
 * Like the parent <code>PropertiesFactoryBean</code>, but overrides or augments the resulting property set with values
 * from VM system properties. As with the Spring {@link PropertyPlaceholderConfigurer} the following modes are
 * supported:
 * <ul>
 * <li>SYSTEM_PROPERTIES_MODE_NEVER: Don't use system properties at all.</li>
 * <li>SYSTEM_PROPERTIES_MODE_FALLBACK: Fallback to a system property only for undefined properties.</li>
 * <li>SYSTEM_PROPERTIES_MODE_OVERRIDE: (DEFAULT) Use a system property if it is available.</li>
 * </ul>
 * Note that system properties will only be included in the property set if defaults for the property have already been
 * defined using {@link #setProperties(Properties)} or {@link #setLocations(org.springframework.core.io.Resource[])} or
 * their names have been included explicitly in the set passed to {@link #setSystemProperties(Set)}.
 *
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class SystemPropertiesFactoryBean extends PropertiesFactoryBean {
    private static final Constants constants = new Constants(PropertyPlaceholderConfigurer.class);

    private int systemPropertiesMode = PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_OVERRIDE;
    private Set<String> systemProperties = Collections.emptySet();

    /**
     * Set the system property mode by the name of the corresponding constant, e.g. "SYSTEM_PROPERTIES_MODE_OVERRIDE".
     *
     * @param constantName
     *            name of the constant
     * @throws java.lang.IllegalArgumentException
     *             if an invalid constant was specified
     * @see #setSystemPropertiesMode
     */
    public void setSystemPropertiesModeName(String constantName) throws IllegalArgumentException {
        this.systemPropertiesMode = SystemPropertiesFactoryBean.constants.asNumber(constantName).intValue();
    }

    /**
     * Set how to check system properties.
     *
     * @see PropertyPlaceholderConfigurer#setSystemPropertiesMode(int)
     */
    public void setSystemPropertiesMode(int systemPropertiesMode) {
        this.systemPropertiesMode = systemPropertiesMode;
    }

    /**
     * Set the names of the properties that can be considered for overriding.
     *
     * @param systemProperties
     *            a set of properties that can be fetched from the system properties
     */
    public void setSystemProperties(Set<String> systemProperties) {
        this.systemProperties = systemProperties;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected Properties mergeProperties() throws IOException {
        // First do the default merge
        Properties props = super.mergeProperties();

        // Now resolve all the merged properties
        if (this.systemPropertiesMode == PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_NEVER) {
            // If we are in never mode, we don't refer to system properties at all
            for (String systemProperty : (Set<String>) (Set) props.keySet()) {
                resolveMergedProperty(systemProperty, props);
            }
        } else {
            // Otherwise, we allow unset properties to drift through from the systemProperties set and potentially set
            // ones to be overriden by system properties
            Set<String> propNames = new HashSet<>((Set) props.keySet());
            propNames.addAll(this.systemProperties);
            for (String systemProperty : propNames) {
                resolveMergedProperty(systemProperty, props);
                if (this.systemPropertiesMode == PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_FALLBACK && props.containsKey(systemProperty)) {
                    // It's already there
                    continue;
                }
                // Get the system value and assign if present
                String systemPropertyValue = System.getProperty(systemProperty);
                if (systemPropertyValue != null) {
                    props.put(systemProperty, systemPropertyValue);
                }
            }
        }
        return props;
    }

    /**
     * Override hook. Allows subclasses to resolve a merged property from an alternative source, whilst still respecting
     * the chosen system property fallback path.
     *
     * @param systemProperty
     * @param props
     */
    protected void resolveMergedProperty(String systemProperty, Properties props) {
    }
}
