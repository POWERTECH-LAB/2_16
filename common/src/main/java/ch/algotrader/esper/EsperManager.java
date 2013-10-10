/***********************************************************************************
 * AlgoTrader Enterprise Trading Framework
 *
 * Copyright (C) 2013 Flury Trading - All rights reserved
 *
 * All information contained herein is, and remains the property of Flury Trading.
 * The intellectual and technical concepts contained herein are proprietary to
 * Flury Trading. Modification, translation, reverse engineering, decompilation,
 * disassembly or reproduction of this material is strictly forbidden unless prior
 * written permission is obtained from Flury Trading
 *
 * Fur detailed terms and conditions consult the file LICENSE.txt or contact
 *
 * Flury Trading
 * Badenerstrasse 16
 * 8004 Zurich
 ***********************************************************************************/
package ch.algotrader.esper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.jms.JMSException;
import javax.jms.Message;

import org.apache.commons.collections15.CollectionUtils;
import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;

import ch.algotrader.ServiceLocator;
import ch.algotrader.entity.Subscription;
import ch.algotrader.entity.marketData.MarketDataEvent;
import ch.algotrader.entity.security.Security;
import ch.algotrader.entity.strategy.Strategy;
import ch.algotrader.entity.strategy.StrategyImpl;
import ch.algotrader.entity.trade.Order;
import ch.algotrader.esper.annotation.Condition;
import ch.algotrader.esper.annotation.Listeners;
import ch.algotrader.esper.annotation.RunTimeOnly;
import ch.algotrader.esper.annotation.SimulationOnly;
import ch.algotrader.esper.annotation.Subscriber;
import ch.algotrader.esper.callback.ClosePositionCallback;
import ch.algotrader.esper.callback.OpenPositionCallback;
import ch.algotrader.esper.callback.TickCallback;
import ch.algotrader.esper.callback.TradeCallback;
import ch.algotrader.esper.io.BatchDBTickInputAdapter;
import ch.algotrader.esper.io.CollectionInputAdapter;
import ch.algotrader.esper.io.CsvBarInputAdapter;
import ch.algotrader.esper.io.CsvBarInputAdapterSpec;
import ch.algotrader.esper.io.CsvTickInputAdapter;
import ch.algotrader.esper.io.CsvTickInputAdapterSpec;
import ch.algotrader.esper.subscriber.SubscriberCreator;
import ch.algotrader.util.MyLogger;
import ch.algotrader.util.collection.CollectionUtil;
import ch.algotrader.util.metric.MetricsUtil;
import ch.algotrader.vo.GenericEventVO;
import ch.algotrader.vo.StatementMetricVO;

import com.espertech.esper.adapter.InputAdapter;
import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.ConfigurationEngineDefaults.Threading;
import com.espertech.esper.client.ConfigurationOperations;
import com.espertech.esper.client.ConfigurationVariable;
import com.espertech.esper.client.EPAdministrator;
import com.espertech.esper.client.EPOnDemandQueryResult;
import com.espertech.esper.client.EPPreparedStatement;
import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.SafeIterator;
import com.espertech.esper.client.StatementAwareUpdateListener;
import com.espertech.esper.client.UpdateListener;
import com.espertech.esper.client.annotation.Name;
import com.espertech.esper.client.deploy.DeploymentInformation;
import com.espertech.esper.client.deploy.EPDeploymentAdmin;
import com.espertech.esper.client.deploy.Module;
import com.espertech.esper.client.deploy.ModuleItem;
import com.espertech.esper.client.soda.AnnotationPart;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.time.TimerControlEvent;
import com.espertech.esper.core.deploy.EPLModuleUtil;
import com.espertech.esper.core.service.EPServiceProviderImpl;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.epl.annotation.AnnotationUtil;
import com.espertech.esper.epl.core.EngineImportService;
import com.espertech.esper.epl.spec.AnnotationDesc;
import com.espertech.esper.epl.spec.StatementSpecMapper;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esperio.AdapterCoordinator;
import com.espertech.esperio.AdapterCoordinatorImpl;
import com.espertech.esperio.csv.CSVInputAdapter;
import com.espertech.esperio.csv.CSVInputAdapterSpec;

/**
 * Main class for management of Esper Engine Instances.
 *
 * @author <a href="mailto:andyflury@gmail.com">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class EsperManager {

    private static Logger logger = MyLogger.getLogger(EsperManager.class.getName());
    private static String newline = System.getProperty("line.separator");

    private static boolean simulation = ServiceLocator.instance().getConfiguration().getSimulation();
    private static boolean singleVM = ServiceLocator.instance().getConfiguration().getBoolean("misc.singleVM");
    private static List<String> moduleDeployExcludeStatements = Arrays.asList((ServiceLocator.instance().getConfiguration().getString("misc.moduleDeployExcludeStatements")).split(","));
    private static int outboundThreads = ServiceLocator.instance().getConfiguration().getInt("misc.outboundThreads");

    private static Map<String, AdapterCoordinator> coordinators = new HashMap<String, AdapterCoordinator>();
    private static Map<String, Boolean> internalClock = new HashMap<String, Boolean>();
    private static Map<String, EPServiceProvider> serviceProviders = new HashMap<String, EPServiceProvider>();
    private static ThreadLocal<Set<String>> processing = new ThreadLocal<Set<String>>() {
        @Override
        protected Set<String> initialValue() {
            return new HashSet<String>();
        }
    };

    private static Map<String, JmsTemplate> templates = new HashMap<String, JmsTemplate>();

    /**
     * Initializes a new Esper Engine by the given {@code strategyName}.
     * The following steps are exectued:
     * <ul>
     * <li>{@code corresponding esper-xxx.cfg.xml} files are loaded from the classpath</li>
     * <li>Esper variables are initilized</li>
     * <li>Esper threading is configured</li>
     * <li>The {@link Strategy} itself is configured as an Esper variable {@code engineStrategy}</li>
     * <li>Esper Time is set to zero</li>
     * </ul>
     */
    public static void initServiceProvider(String strategyName) {

        String providerURI = getProviderURI(strategyName);

        Configuration configuration = new Configuration();

        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath*:/META-INF/esper-**.cfg.xml");
            for (Resource resource : resources) {
                if (StrategyImpl.BASE.equals(strategyName)) {

                    // for Base only load esper-common.cfg.xml and esper-core.cfg.xml
                    if (resource.toString().contains("esper-common.cfg.xml") || resource.toString().contains("esper-core.cfg.xml")) {
                        configuration.configure(resource.getURL());
                    }
                } else {

                    // for Strategies to not load esper-core.cfg.xml
                    if (!resource.toString().contains("esper-core.cfg.xml")) {
                        configuration.configure(resource.getURL());
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("problem loading esper config", e);
        }

        initVariables(configuration);

        // outbound threading for BASE
        if (StrategyImpl.BASE.equals(strategyName) && !simulation) {

            Threading threading = configuration.getEngineDefaults().getThreading();

            threading.setThreadPoolOutbound(true);
            threading.setThreadPoolOutboundNumThreads(outboundThreads);
        }

        Strategy strategy = ServiceLocator.instance().getLookupService().getStrategyByName(strategyName);
        configuration.getVariables().get("engineStrategy").setInitializationValue(strategy);

        EPServiceProvider serviceProvider = EPServiceProviderManager.getProvider(providerURI, configuration);

        // must send time event before first schedule pattern
        serviceProvider.getEPRuntime().sendEvent(new CurrentTimeEvent(0));

        serviceProviders.put(providerURI, serviceProvider);

        internalClock.put(strategyName, false);

        logger.debug("initialized service provider: " + strategyName);
    }

    /**
     * Returns true if the specified Esper Engine is initialized.
     */
    public static boolean isInitialized(String strategyName) {

        return serviceProviders.containsKey(getProviderURI(strategyName));
    }

    /**
     * Destroys the specified Esper Engine.
     */
    public static void destroyServiceProvider(String strategyName) {

        getServiceProvider(strategyName).destroy();
        serviceProviders.remove(getProviderURI(strategyName));
        internalClock.remove(strategyName);

        logger.debug("destroyed service provider: " + strategyName);
    }

    /**
     * Deploys the statement from the given {@code moduleName} with the given {@code statementName} into the specified Esper Engine.
     */
    public static void deployStatement(String strategyName, String moduleName, String statementName) {

        deployStatement(strategyName, moduleName, statementName, null, new Object[] {}, null);
    }

    /**
     * Deploys the statement from the given {@code moduleName} with the given {@code statementName} into the specified Esper Engine.
     * In addition the given {@code alias} and Prepared Statement {@code params} are set on the statement
     */
    public static void deployStatement(String strategyName, String moduleName, String statementName, String alias, Object[] params) {

        deployStatement(strategyName, moduleName, statementName, alias, params, null);
    }

    /**
     * Deploys the statement from the given {@code moduleName} with the given {@code statementName} into the specified Esper Engine.
     * In addition the given {@code alias}, Prepared Statement {@code params} and {@code callback} are set on the statement.
     */
    public static void deployStatement(String strategyName, String moduleName, String statementName, String alias, Object[] params, Object callback) {

        EPServiceProvider serviceProvider = getServiceProvider(strategyName);

        // do nothing if the statement already exists
        EPStatement oldStatement = serviceProvider.getEPAdministrator().getStatement(statementName);
        if (oldStatement != null && oldStatement.isStarted()) {
            logger.warn(statementName + " is already deployed and started");
            return;
        }

        Module module = getModule(moduleName);

        // go through all statements in the module
        EPStatement newStatement = null;
        for (ModuleItem moduleItem : module.getItems()) {

            if (isEligibleStatement(serviceProvider, moduleItem, statementName)) {

                newStatement = startStatement(moduleItem, serviceProvider, alias, params, callback);

                // break iterating over the statements
                break;

            } else {
                continue;
            }
        }

        if (newStatement == null) {
            logger.warn("statement " + statementName + " was not found");
        } else {
            logger.debug("deployed statement " + newStatement.getName() + " on service provider: " + strategyName);
        }
    }

    /**
     * Deploys all modules defined for given Strategy
     */
    public static void deployAllModules(String strategyName) {

        deployInitModules(strategyName);
        deployRunModules(strategyName);
    }

    /**
     * Deploys all init-modules defined for given Strategy
     */
    public static void deployInitModules(String strategyName) {

        Strategy strategy = ServiceLocator.instance().getLookupService().getStrategyByName(strategyName);
        String initModules = strategy.getInitModules();
        if (initModules != null) {
            String[] modules = initModules.split(",");
            for (String module : modules) {
                deployModule(strategyName, module.trim());
            }
        }
    }

    /**
     * Deploys all run-modules defined for given Strategy
     */
    public static void deployRunModules(String strategyName) {

        Strategy strategy = ServiceLocator.instance().getLookupService().getStrategyByName(strategyName);
        String runModules = strategy.getRunModules();
        if (runModules != null) {
            String[] modules = runModules.split(",");
            for (String module : modules) {
                deployModule(strategyName, module.trim());
            }
        }
    }

    /**
     * Deploys the specified module into the specified Esper Engine.
     */
    public static void deployModule(String strategyName, String moduleName) {

        EPServiceProvider serviceProvider = getServiceProvider(strategyName);

        Module module = getModule(moduleName);

        for (ModuleItem moduleItem : module.getItems()) {

            if (isEligibleStatement(serviceProvider, moduleItem, null)) {

                startStatement(moduleItem, serviceProvider, null, new Object[] {}, null);

            } else {
                continue;
            }
        }

        logger.debug("deployed module " + moduleName + " on service provider: " + strategyName);
    }

    /**
     * Returns true if the statement by the given {@code statementNameRegex} is deployed.
     *
     * @param statementNameRegex statement name regular expression
     */
    public static boolean isDeployed(String strategyName, final String statementNameRegex) {

        // find the first statement that matches the given statementName regex
        String[] statementNames = findStatementNames(strategyName, statementNameRegex);

        if (statementNames.length == 0) {
            return false;
        } else if (statementNames.length > 1) {
            logger.error("more than one statement matches: " + statementNameRegex);
        }

        // get the statement
        EPAdministrator administrator = getServiceProvider(strategyName).getEPAdministrator();
        EPStatement statement = administrator.getStatement(statementNames[0]);

        if (statement != null && statement.isStarted()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Undeploys the specified statement from the specified Esper Engine.
     */
    public static void undeployStatement(String strategyName, String statementName) {

        // destroy the statement
        EPStatement statement = getServiceProvider(strategyName).getEPAdministrator().getStatement(statementName);

        if (statement != null && statement.isStarted()) {
            statement.destroy();
            logger.debug("undeployed statement " + statementName);
        }
    }

    /**
     * Restarts the given Statement
     */
    public static void restartStatement(String strategyName, String statementName) {

        // destroy the statement
        EPStatement statement = getServiceProvider(strategyName).getEPAdministrator().getStatement(statementName);

        if (statement != null && statement.isStarted()) {
            statement.stop();
            statement.start();
            logger.debug("restarted statement " + statementName);
        }
    }

    /**
     * Undeploys the specified module from the specified Esper Engine.
     */
    public static void undeployModule(String strategyName, String moduleName) {

        EPAdministrator administrator = getServiceProvider(strategyName).getEPAdministrator();
        EPDeploymentAdmin deployAdmin = administrator.getDeploymentAdmin();
        for (DeploymentInformation deploymentInformation : deployAdmin.getDeploymentInformation()) {
            if (deploymentInformation.getModule().getName().equals(moduleName)) {
                try {
                    deployAdmin.undeploy(deploymentInformation.getDeploymentId());
                } catch (Exception e) {
                    throw new RuntimeException("module " + moduleName + " could no be undeployed", e);
                }
            }
        }

        logger.debug("undeployed module " + moduleName);
    }

    /**
     * Adds the given Event Type to the specified Esper Engine.
     * this method can be used if an Event Type is not known at compile time and can therefore not be configured
     * inside an {@code esper-xxx.cfg.xml} file
     */
    public static void addEventType(String strategyName, String eventTypeName, String eventClassName) {

        ConfigurationOperations configuration = getServiceProvider(strategyName).getEPAdministrator().getConfiguration();
        if (configuration.getEventType(eventTypeName) == null) {
            configuration.addEventType(eventTypeName, eventClassName);
        }
    }

    /**
     * Sends an Event into the corresponding Esper Engine.
     */
    public static void sendEvent(String strategyName, Object obj) {

        if (simulation || singleVM) {
            if (isInitialized(strategyName)) {

                long startTime = System.nanoTime();

                internalSendEvent(strategyName, obj);

                MetricsUtil.accountEnd("EsperManager." + strategyName, obj.getClass(), startTime);
            }
        } else {

            // check if it is the localStrategy
            if (ServiceLocator.instance().getConfiguration().getStartedStrategyName().equals(strategyName)) {
                internalSendEvent(strategyName, obj);
            } else {
                externalSendEvent(strategyName, obj);
            }
        }
    }

    /**
     * Sends a MarketDataEvent into the corresponding Esper Engine.
     * In Live-Trading the {@code marketDataTemplate} will be used.
     */
    public static void sendMarketDataEvent(final MarketDataEvent marketDataEvent) {

        if (simulation || singleVM) {
            for (Subscription subscription : marketDataEvent.getSecurity().getSubscriptions()) {
                if (!subscription.getStrategyInitialized().getName().equals(StrategyImpl.BASE)) {
                    sendEvent(subscription.getStrategy().getName(), marketDataEvent);
                }
            }

        } else {

            // send using the jms template
            getTemplate("marketDataTemplate").convertAndSend(marketDataEvent, new MessagePostProcessor() {

                @Override
                public Message postProcessMessage(Message message) throws JMSException {

                    // add securityId Property
                    message.setIntProperty("securityId", marketDataEvent.getSecurity().getId());
                    return message;
                }
            });
        }
    }

    /**
     * Sends a MarketDataEvent into the corresponding Esper Engine.
     * In Live-Trading the {@code genericTemplate} will be used.
     */
    public static void sendGenericEvent(final GenericEventVO event) {

        if (simulation || singleVM) {
            for (String strategyName : serviceProviders.keySet()) {
                if (!strategyName.equals(StrategyImpl.BASE) && !strategyName.equals(event.getStrategyName())) {
                    sendEvent(strategyName, event);
                }
            }

        } else {

            // send using the jms template
            getTemplate("genericTemplate").convertAndSend(event, new MessagePostProcessor() {

                @Override
                public Message postProcessMessage(Message message) throws JMSException {

                    // add class Property
                    message.setStringProperty("clazz", event.getClass().getName());
                    return message;
                }
            });
        }
    }

    /**
     * Executes an arbitrary EPL query on the Esper Engine.
     */
    @SuppressWarnings("rawtypes")
    public static List executeQuery(String strategyName, String query) {

        List<Object> objects = new ArrayList<Object>();
        EPOnDemandQueryResult result = getServiceProvider(strategyName).getEPRuntime().executeQuery(query);
        for (EventBean row : result.getArray()) {
            Object object = row.getUnderlying();
            objects.add(object);
        }
        return objects;
    }

    /**
     * Executes an arbitrary EPL query that is supposed to return one single object on the Esper Engine.
     */
    @SuppressWarnings("unchecked")
    public static Object executeSingelObjectQuery(String strategyName, String query) {

        List<Object> events = executeQuery(StrategyImpl.BASE, query);
        if (events.size() == 0) {
            return null;
        } else if (events.size() == 1) {
            return events.get(0);
        } else {
            throw new IllegalArgumentException("query returned more than one object");
        }
    }

    /**
     * Retrieves the last event currently held by the given statement.
     */
    public static Object getLastEvent(String strategyName, String statementName) {

        EPStatement statement = getServiceProvider(strategyName).getEPAdministrator().getStatement(statementName);
        if (statement == null) {
            throw new IllegalStateException("statement " + statementName + " does not exist");
        } else if (!statement.isStarted()) {
            throw new IllegalStateException("statement " + statementName + " is not started");
        } else {
            SafeIterator<EventBean> it = statement.safeIterator();
            try {
                if (it.hasNext()) {
                    return it.next().getUnderlying();
                } else {
                    return null;
                }
            } finally {
                it.close();
            }
        }
    }

    /**
     * Retrieves the last event currently held by the given statement and returns the property by the {@code propertyName}
     */
    public static Object getLastEventProperty(String strategyName, String statementName, String propertyName) {

        EPStatement statement = getServiceProvider(strategyName).getEPAdministrator().getStatement(statementName);
        if (statement == null) {
            throw new IllegalStateException("statement " + statementName + " does not exist");
        } else if (!statement.isStarted()) {
            throw new IllegalStateException("statement " + statementName + " is not started");
        } else {
            SafeIterator<EventBean> it = statement.safeIterator();
            try {
                if (it.hasNext()) {
                    return it.next().get(propertyName);
                } else {
                    return null;
                }
            } finally {
                it.close();
            }
        }
    }

    /**
     * Retrieves all events currently held by the given statement.
     */
    @SuppressWarnings("rawtypes")
    public static List getAllEvents(String strategyName, String statementName) {

        EPStatement statement = getServiceProvider(strategyName).getEPAdministrator().getStatement(statementName);
        if (statement == null) {
            throw new IllegalStateException("statement " + statementName + " does not exist");
        } else if (!statement.isStarted()) {
            throw new IllegalStateException("statement " + statementName + " is not started");
        } else {
            List<Object> list = new ArrayList<Object>();
            SafeIterator<EventBean> it = statement.safeIterator();
            try {
                while (it.hasNext()) {
                    EventBean bean = it.next();
                    Object underlying = bean.getUnderlying();
                    list.add(underlying);
                }
                return list;
            } finally {
                it.close();
            }
        }
    }

    /**
     * Retrieves all events currently held by the given statement and returns the property by the {@code propertyName} for all events.
     */
    @SuppressWarnings("rawtypes")
    public static List getAllEventsProperty(String strategyName, String statementName, String property) {

        EPStatement statement = getServiceProvider(strategyName).getEPAdministrator().getStatement(statementName);
        if (statement == null) {
            throw new IllegalStateException("statement " + statementName + " does not exist");
        } else if (!statement.isStarted()) {
            throw new IllegalStateException("statement " + statementName + " is not started");
        } else {
            List<Object> list = new ArrayList<Object>();
            SafeIterator<EventBean> it = statement.safeIterator();
            try {
                while (it.hasNext()) {
                    EventBean bean = it.next();
                    Object underlying = bean.get(property);
                    list.add(underlying);
                }
                return list;
            } finally {
                it.close();
            }
        }
    }

    /**
     * Sets the internal clock of the specified Esper Engine
     */
    public static void setInternalClock(String strategyName, boolean internal) {

        internalClock.put(strategyName, internal);

        if (internal) {
            sendEvent(strategyName, new TimerControlEvent(TimerControlEvent.ClockType.CLOCK_INTERNAL));
            EPServiceProviderImpl provider = (EPServiceProviderImpl) getServiceProvider(strategyName);
            provider.getTimerService().enableStats();
        } else {
            sendEvent(strategyName, new TimerControlEvent(TimerControlEvent.ClockType.CLOCK_EXTERNAL));
            EPServiceProviderImpl provider = (EPServiceProviderImpl) getServiceProvider(strategyName);
            provider.getTimerService().disableStats();
        }

        setVariableValue(strategyName, "internal_clock", internal);

        logger.debug("set internal clock to: " + internal + " for strategy: " + strategyName);
    }

    /**
     * Returns true if the specified Esper Engine uses internal clock
     */
    public static boolean isInternalClock(String strategyName) {

        return internalClock.get(strategyName);
    }

    /**
     * Sends a {@link com.espertech.esper.client.time.CurrentTimeEvent} to all local Esper Engines
     */
    public static void setCurrentTime(CurrentTimeEvent currentTimeEvent) {

        // sent currentTime to all local engines
        for (String providerURI : EPServiceProviderManager.getProviderURIs()) {
            sendEvent(providerURI, currentTimeEvent);
        }
    }

    /**
     * Retruns the current time of the given Esper Engine
     */
    public static long getCurrentTime(String strategyName) {

        return getServiceProvider(strategyName).getEPRuntime().getCurrentTime();
    }

    /**
     * Prepares the given Esper Engine for coordinated input of CSV-Files
     */
    public static void initCoordination(String strategyName) {

        coordinators.put(strategyName, new AdapterCoordinatorImpl(getServiceProvider(strategyName), true, true));
    }

    /**
     * Queues the specified {@code csvInputAdapterSpec} for coordination with the given Esper Engine.
     */
    public static void coordinate(String strategyName, CSVInputAdapterSpec csvInputAdapterSpec) {

        InputAdapter inputAdapter;
        if (csvInputAdapterSpec instanceof CsvTickInputAdapterSpec) {
            inputAdapter = new CsvTickInputAdapter(getServiceProvider(strategyName), (CsvTickInputAdapterSpec) csvInputAdapterSpec);
        } else if (csvInputAdapterSpec instanceof CsvBarInputAdapterSpec) {
            inputAdapter = new CsvBarInputAdapter(getServiceProvider(strategyName), (CsvBarInputAdapterSpec) csvInputAdapterSpec);
        } else {
            inputAdapter = new CSVInputAdapter(getServiceProvider(strategyName), csvInputAdapterSpec);
        }
        coordinators.get(strategyName).coordinate(inputAdapter);
    }

    /**
     * Queues the specified {@code collection} for coordination with the given Esper Engine.
     * The property by the name of {@code timeStampProperty} is used to identify the current time.
     */
    @SuppressWarnings({ "rawtypes" })
    public static void coordinate(String strategyName, Collection collection, String timeStampProperty) {

        InputAdapter inputAdapter = new CollectionInputAdapter(getServiceProvider(strategyName), collection, timeStampProperty);
        coordinators.get(strategyName).coordinate(inputAdapter);
    }

    /**
     * Queues subscribed Ticks for coordination with the given Esper Engine.
     */
    public static void coordinateTicks(String strategyName, Date startDate) {

        InputAdapter inputAdapter = new BatchDBTickInputAdapter(getServiceProvider(strategyName), startDate);
        coordinators.get(strategyName).coordinate(inputAdapter);
    }

    /**
     * Starts coordination for the given Esper Engine.
     */
    public static void startCoordination(String strategyName) {

        coordinators.get(strategyName).start();
    }

    /**
     * sets the value of the specified Esper Variable
     */
    public static void setVariableValue(String strategyName, String variableName, Object value) {

        variableName = variableName.replace(".", "_");
        EPRuntime runtime = getServiceProvider(strategyName).getEPRuntime();
        if (runtime.getVariableValueAll().containsKey(variableName)) {
            runtime.setVariableValue(variableName, value);
            logger.debug("set variable " + variableName + " to value " + value);
        }
    }

    /**
     * sets the value of the specified Esper Variable by parsing the given String value.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void setVariableValueFromString(String strategyName, String variableName, String value) {

        variableName = variableName.replace(".", "_");
        EPRuntime runtime = getServiceProvider(strategyName).getEPRuntime();
        if (runtime.getVariableValueAll().containsKey(variableName)) {
            Class clazz = runtime.getVariableValue(variableName).getClass();

            Object castedObj = null;
            if (clazz.isEnum()) {
                castedObj = Enum.valueOf(clazz, value);
            } else {
                castedObj = JavaClassHelper.parse(clazz, value);
            }
            runtime.setVariableValue(variableName, castedObj);
            logger.debug("set variable " + variableName + " to value " + value);
        }
    }

    /**
     * Returns the value of the specified Esper Variable
     */
    public static Object getVariableValue(String strategyName, String variableName) {

        variableName = variableName.replace(".", "_");
        EPRuntime runtime = getServiceProvider(strategyName).getEPRuntime();
        return runtime.getVariableValue(variableName);
    }

    /**
     * Adds a {@link TradeCallback} to the given Esper Engine that will be invoked as soon as all {@code orders} have been
     * fully exectured or cancelled.
     */
    public static void addTradeCallback(String strategyName, Collection<Order> orders, TradeCallback callback) {

        // get the securityIds sorted asscending and check that all orders are from the same strategy
        final Order firstOrder = CollectionUtil.getFirstElement(orders);
        Set<Integer> sortedSecurityIds = new TreeSet<Integer>(CollectionUtils.collect(orders, new Transformer<Order, Integer>() {

            @Override
            public Integer transform(Order order) {
                if (!order.getStrategy().equals(firstOrder.getStrategy())) {
                    throw new IllegalArgumentException("cannot addTradeCallback for orders of different strategies");
                }
                return order.getSecurity().getId();
            }
        }));

        if (sortedSecurityIds.size() < orders.size()) {
            throw new IllegalArgumentException("cannot addTradeCallback for multiple orders on the same security");
        }

        // get the statement alias based on all security ids
        String alias = "ON_TRADE_COMPLETED_" + StringUtils.join(sortedSecurityIds, "_") + "_" + firstOrder.getStrategy().getName();

        if (isDeployed(strategyName, alias)) {

            logger.warn(alias + " is already deployed");
        } else {

            Object[] params = new Object[] { sortedSecurityIds.size(), sortedSecurityIds, firstOrder.getStrategy().getName(), firstOrder.isAlgoOrder() };
            deployStatement(strategyName, "prepared", "ON_TRADE_COMPLETED", alias, params, callback);
        }
    }

    /**
     * Adds a {@link TickCallback} to the given Esper Engine that will be invoked as soon as at least one Tick has arrived
     * for each of the specified {@code securities}
     */
    public static void addFirstTickCallback(String strategyName, Collection<Security> securities, TickCallback callback) {

        // create a list of unique security ids
        Set<Integer> securityIds = new TreeSet<Integer>();
        securityIds.addAll(CollectionUtils.collect(securities, new Transformer<Security, Integer>() {
            @Override
            public Integer transform(Security security) {
                return security.getId();
            }
        }));

        String alias = "ON_FIRST_TICK_" + StringUtils.join(securityIds, "_");

        if (isDeployed(strategyName, alias)) {

            logger.warn(alias + " is already deployed");
        } else {

            int[] securityIdsArray = ArrayUtils.toPrimitive(securityIds.toArray(new Integer[0]));
            deployStatement(strategyName, "prepared", "ON_FIRST_TICK", alias, new Object[] { securityIds.size(), securityIdsArray }, callback);
        }
    }

    /**
     * Adds a {@link OpenPositionCallback} to the given Esper Engine that will be invoked as soon as a new Position
     * on the given Security has been opened.
     */
    public static void addOpenPositionCallback(String strategyName, int securityId, OpenPositionCallback callback) {

        String alias = "ON_OPEN_POSITION_" + securityId;

        if (isDeployed(strategyName, alias)) {

            logger.warn(alias + " is already deployed");
        } else {

            deployStatement(strategyName, "prepared", "ON_OPEN_POSITION", alias, new Object[] { securityId }, callback);
        }
    }

    /**
     * Adds a {@link OpenPositionCallback} to the given Esper Engine that will be invoked as soon as a Position
     * on the given Security has been closed.
     */
    public static void addClosePositionCallback(String strategyName, int securityId, ClosePositionCallback callback) {

        String alias = "ON_CLOSE_POSITION_" + securityId;

        if (isDeployed(strategyName, alias)) {

            logger.warn(alias + " is already deployed");
        } else {

            deployStatement(strategyName, "prepared", "ON_CLOSE_POSITION", alias, new Object[] { securityId }, callback);
        }
    }

    /**
     * Prints all statement metrics.
     */
    @SuppressWarnings("unchecked")
    public static void logStatementMetrics() {

        for (String strategyName : serviceProviders.keySet()) {

            List<StatementMetricVO> metrics = getAllEvents(strategyName, "METRICS");

            // consolidate ON_TRADE_COMPLETED and ON_FIRST_TICK
            for (final String statementName :  new String[] {"ON_TRADE_COMPLETED", "ON_FIRST_TICK"}) {

                // select metrics where the statementName startsWith
                Collection<StatementMetricVO> selectedMetrics = CollectionUtils.select(metrics, new Predicate<StatementMetricVO>() {
                    @Override
                    public boolean evaluate(StatementMetricVO metric) {
                        return metric.getStatementName() != null && metric.getStatementName().startsWith(statementName);
                    }});

                // add cpuTime, wallTime and numInput
                if (selectedMetrics.size() > 0) {

                    long cpuTime = 0;
                    long wallTime = 0;
                    long numInput = 0;
                    for (StatementMetricVO metric : selectedMetrics) {

                        cpuTime += metric.getCpuTime();
                        wallTime += metric.getWallTime();
                        numInput += metric.getNumInput();

                        // remove the original metric
                        metrics.remove(metric);
                    };

                    // add a consolidated metric
                    metrics.add(new StatementMetricVO(strategyName, statementName, cpuTime, wallTime, numInput));
                }
            }

            for (StatementMetricVO metric : metrics) {
                logger.info(metric.getEngineURI() + "." + metric.getStatementName() + ": " + metric.getWallTime() + " millis " +  metric.getNumInput() + " events");
            }
        }
    }

    /**
     * Resets all statement metrics.
     */
    public static void resetStatementMetrics() {

        for (String strategyName : serviceProviders.keySet()) {
            restartStatement(strategyName, "METRICS");
        }
    }

    private static String getProviderURI(String strategyName) {

        return (strategyName == null || "".equals(strategyName)) ? StrategyImpl.BASE : strategyName.toUpperCase();
    }

    private static EPServiceProvider getServiceProvider(String strategyName) {

        String providerURI = getProviderURI(strategyName);

        EPServiceProvider serviceProvider = serviceProviders.get(providerURI);
        if (serviceProvider == null) {
            throw new IllegalStateException("strategy " + providerURI + " is not initialized yet!");
        }

        return serviceProvider;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void initVariables(Configuration configuration) {

        try {
            Map<String, ConfigurationVariable> variables = configuration.getVariables();
            for (Map.Entry<String, ConfigurationVariable> entry : variables.entrySet()) {
                String variableName = entry.getKey().replace("_", ".");
                String value = ServiceLocator.instance().getConfiguration().getString(variableName);
                if (value != null) {
                    Class clazz = Class.forName(entry.getValue().getType());
                    Object castedObj = null;
                    if (clazz.isEnum()) {
                        castedObj = Enum.valueOf(clazz, value);
                    } else if (clazz == BigDecimal.class) {
                        castedObj = new BigDecimal(value);
                    } else {
                        castedObj = JavaClassHelper.parse(clazz, value);
                    }
                    entry.getValue().setInitializationValue(castedObj);
                }
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static Module getModule(String moduleName) {

        String fileName = "module-" + moduleName + ".epl";
        InputStream stream = EsperManager.class.getResourceAsStream("/" + fileName);
        if (stream == null) {
            throw new IllegalArgumentException(fileName + " does not exist");
        }

        // process loads
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            StringWriter buffer = new StringWriter();
            String strLine;
            while ((strLine = reader.readLine()) != null) {
                if (!strLine.startsWith("load")) {
                    buffer.append(strLine);
                    buffer.append(newline);
                } else {
                    String argument = StringUtils.substringAfter(strLine, "load").trim();
                    String moduleBaseName = argument.split("\\.")[0];
                    String statementName = argument.split("\\.")[1].split(";")[0];
                    Module module = EPLModuleUtil.readResource("module-" + moduleBaseName + ".epl");
                    for (ModuleItem item : module.getItems()) {
                        if (item.getExpression().contains("@Name('" + statementName + "')")) {
                            buffer.append(item.getExpression());
                            buffer.append(";");
                            buffer.append(newline);
                            break;
                        }
                    }
                }
            }
            reader.close();
            stream.close();

            return EPLModuleUtil.parseInternal(buffer.toString(), fileName);

        } catch (Exception e) {
            throw new RuntimeException(moduleName + " could not be deployed", e);
        }
    }

    private static boolean isEligibleStatement(EPServiceProvider serviceProvider, ModuleItem item, String statementName) {

        if (item.isCommentOnly()) {
            return false;
        }

        String expression = item.getExpression().replace("?", "1"); // replace ? to prevent error during compile
        EPStatementObjectModel objectModel = serviceProvider.getEPAdministrator().compileEPL(expression);
        List<AnnotationPart> annotationParts = objectModel.getAnnotations();
        List<AnnotationDesc> annotationDescs = StatementSpecMapper.mapAnnotations(annotationParts);

        EngineImportService engineImportService = ((EPServiceProviderSPI) serviceProvider).getEngineImportService();
        Annotation[] annotations = AnnotationUtil.compileAnnotations(annotationDescs, engineImportService, item.getExpression());

        for (Annotation annotation : annotations) {
            if (annotation instanceof Name) {

                Name name = (Name) annotation;
                if (statementName != null && !statementName.equals(name.value())) {
                    return false;
                } else if (moduleDeployExcludeStatements.contains(name.value())) {
                    return false;
                }

            } else if (annotation instanceof RunTimeOnly && simulation) {

                return false;

            } else if (annotation instanceof SimulationOnly && !simulation) {

                return false;

            } else if (annotation instanceof Condition) {

                Condition condition = (Condition) annotation;
                String key = condition.key();
                if (!ServiceLocator.instance().getConfiguration().getBoolean(key)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static EPStatement startStatement(ModuleItem moduleItem, EPServiceProvider serviceProvider, String alias, Object[] params, Object callback) {

        // create the statement and set the prepared statement params if a prepared statement
        EPStatement statement;
        String expression = moduleItem.getExpression();
        EPAdministrator administrator = serviceProvider.getEPAdministrator();
        if (expression.contains("?")) {
            EPPreparedStatement prepared = administrator.prepareEPL(expression);
            for (int i = 0; i < params.length; i++) {
                prepared.setObject(i + 1, params[i]);
            }
            if (alias != null) {
                statement = administrator.create(prepared, alias);
            } else {
                statement = administrator.create(prepared);
            }
        } else {
            if (alias != null) {
                statement = administrator.createEPL(expression, alias);
            } else {
                statement = administrator.createEPL(expression);
            }
        }

        // process annotations
        Annotation[] annotations = statement.getAnnotations();
        for (Annotation annotation : annotations) {
            if (annotation instanceof Subscriber) {

                Subscriber subscriber = (Subscriber) annotation;
                try {
                    Object obj = getSubscriber(subscriber.className());
                    statement.setSubscriber(obj);
                } catch (Exception e) {
                    throw new RuntimeException("subscriber " + subscriber.className() + " could not be created for statement " + statement.getName(), e);
                }

            } else if (annotation instanceof Listeners) {

                Listeners listeners = (Listeners) annotation;
                for (String className : listeners.classNames()) {
                    try {
                        Class<?> cl = Class.forName(className);
                        Object obj = cl.newInstance();
                        if (obj instanceof StatementAwareUpdateListener) {
                            statement.addListener((StatementAwareUpdateListener) obj);
                        } else {
                            statement.addListener((UpdateListener) obj);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("listener " + className + " could not be created for statement " + statement.getName(), e);
                    }
                }
            }
        }

        // attach the callback if supplied
        // will override the Subscriber defined in Annotations
        // in live trading stop the statement before attaching (and restart afterwards)
        // to make sure that the subscriber receives the first event
        if (callback != null) {
            if (simulation) {
                statement.setSubscriber(callback);
            } else {
                statement.stop();
                statement.setSubscriber(callback);
                statement.start();
            }
        }
        return statement;
    }

    private static String[] findStatementNames(String strategyName, final String statementNameRegex) {

        EPAdministrator administrator = getServiceProvider(strategyName).getEPAdministrator();

        // find the first statement that matches the given statementName regex
        return CollectionUtils.select(Arrays.asList(administrator.getStatementNames()), new Predicate<String>() {

            @Override
            public boolean evaluate(String statement) {
                return statement.matches(statementNameRegex);
            }
        }).toArray(new String[] {});
    }

    private static void internalSendEvent(String strategyName, Object obj) {

        // if the engine is currently processing and event route the new event
        if (processing.get().contains(strategyName)) {
            getServiceProvider(strategyName).getEPRuntime().route(obj);
        } else {
            processing.get().add(strategyName);
            getServiceProvider(strategyName).getEPRuntime().sendEvent(obj);
            processing.get().remove(strategyName);
        }
    }

    private static void externalSendEvent(String strategyName, Object obj) {

        // sent to the strateyg queue
        getTemplate("strategyTemplate").convertAndSend(strategyName + ".QUEUE", obj);

        logger.trace("propagated event to " + strategyName + " " + obj);
    }

    /**
     * manual lookup of templates since they are only available if applicationContext-jms.xml is active
     */
    private static JmsTemplate getTemplate(String name) {

        if (!templates.containsKey(name)) {
            templates.put(name, ServiceLocator.instance().getService(name, JmsTemplate.class));
        }

        return templates.get(name);
    }

    private static Object getSubscriber(String fqdn) throws ClassNotFoundException {

        // try to see if the fqdn represents a class
        try {
            Class<?> cl = Class.forName(fqdn);
            return cl.newInstance();
        } catch (Exception e) {
            // do nothin
        }

        // otherwise the fqdn represents a method, in this case treate a subscriber
        return SubscriberCreator.createSubscriber(fqdn);
    }
}
