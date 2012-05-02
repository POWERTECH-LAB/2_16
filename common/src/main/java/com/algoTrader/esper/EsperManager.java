package com.algoTrader.esper;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
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
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;

import com.algoTrader.ServiceLocator;
import com.algoTrader.entity.Strategy;
import com.algoTrader.entity.StrategyImpl;
import com.algoTrader.entity.Subscription;
import com.algoTrader.entity.marketData.MarketDataEvent;
import com.algoTrader.entity.marketData.TickCallback;
import com.algoTrader.entity.trade.Order;
import com.algoTrader.entity.trade.TradeCallback;
import com.algoTrader.esper.annotation.Condition;
import com.algoTrader.esper.annotation.Listeners;
import com.algoTrader.esper.annotation.RunTimeOnly;
import com.algoTrader.esper.annotation.SimulationOnly;
import com.algoTrader.esper.annotation.Subscriber;
import com.algoTrader.esper.io.BatchDBTickInputAdapter;
import com.algoTrader.esper.io.CsvBarInputAdapter;
import com.algoTrader.esper.io.CsvBarInputAdapterSpec;
import com.algoTrader.esper.io.CsvTickInputAdapter;
import com.algoTrader.esper.io.CsvTickInputAdapterSpec;
import com.algoTrader.esper.io.DBInputAdapter;
import com.algoTrader.esper.subscriber.SubscriberCreator;
import com.algoTrader.util.MyLogger;
import com.algoTrader.util.StrategyUtil;
import com.algoTrader.util.metric.MetricsUtil;
import com.algoTrader.vo.StatementMetricVO;
import com.espertech.esper.adapter.InputAdapter;
import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.ConfigurationVariable;
import com.espertech.esper.client.EPAdministrator;
import com.espertech.esper.client.EPOnDemandQueryResult;
import com.espertech.esper.client.EPPreparedStatement;
import com.espertech.esper.client.EPPreparedStatementImpl;
import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.SafeIterator;
import com.espertech.esper.client.StatementAwareUpdateListener;
import com.espertech.esper.client.UpdateListener;
import com.espertech.esper.client.deploy.DeploymentInformation;
import com.espertech.esper.client.deploy.DeploymentOptions;
import com.espertech.esper.client.deploy.DeploymentResult;
import com.espertech.esper.client.deploy.EPDeploymentAdmin;
import com.espertech.esper.client.deploy.Module;
import com.espertech.esper.client.deploy.ModuleItem;
import com.espertech.esper.client.soda.AnnotationAttribute;
import com.espertech.esper.client.soda.AnnotationPart;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.time.TimerControlEvent;
import com.espertech.esper.core.service.EPServiceProviderImpl;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esperio.AdapterCoordinator;
import com.espertech.esperio.AdapterCoordinatorImpl;
import com.espertech.esperio.csv.CSVInputAdapter;
import com.espertech.esperio.csv.CSVInputAdapterSpec;

public class EsperManager {

    private static Logger logger = MyLogger.getLogger(EsperManager.class.getName());

    private static boolean simulation = ServiceLocator.instance().getConfiguration().getSimulation();
    private static List<String> moduleDeployExcludeStatements = Arrays.asList((ServiceLocator.instance().getConfiguration().getString("misc.moduleDeployExcludeStatements")).split(","));
    private static final boolean metricsEnabled = ServiceLocator.instance().getConfiguration().getBoolean("misc.metricsEnabled");

    private static Map<String, AdapterCoordinator> coordinators = new HashMap<String, AdapterCoordinator>();
    private static Map<String, Boolean> internalClock = new HashMap<String, Boolean>();
    private static Map<String, EPServiceProvider> serviceProviders = new HashMap<String, EPServiceProvider>();

    private static JmsTemplate marketDataTemplate;
    private static JmsTemplate strategyTemplate;

    public static void initServiceProvider(String strategyName) {

        String providerURI = getProviderURI(strategyName);

        Configuration configuration = new Configuration();
        configuration.configure("esper-" + providerURI.toLowerCase() + ".cfg.xml");

        initVariables(strategyName, configuration);

        Strategy strategy = ServiceLocator.instance().getLookupService().getStrategyByName(strategyName);
        configuration.getVariables().get("engineStrategy").setInitializationValue(strategy);

        EPServiceProvider serviceProvider = EPServiceProviderManager.getProvider(providerURI, configuration);

        // must send time event before first schedule pattern
        serviceProvider.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        internalClock.put(strategyName, false);

        logger.debug("initialized service provider: " + strategyName);

        serviceProviders.put(providerURI, serviceProvider);
    }

    public static boolean isInitialized(String strategyName) {

        return serviceProviders.containsKey(getProviderURI(strategyName));
    }

    public static void destroyServiceProvider(String strategyName) {

        getServiceProvider(strategyName).destroy();
        serviceProviders.remove(getProviderURI(strategyName));

        logger.debug("destroyed service provider: " + strategyName);
    }

    public static void deployStatement(String strategyName, String moduleName, String statementName) {

        internalDeployStatement(strategyName, moduleName, statementName, null, new Object[] {}, null);
    }

    public static void deployStatement(String strategyName, String moduleName, String statementName, String alias, Object[] params) {

        internalDeployStatement(strategyName, moduleName, statementName, alias, params, null);
    }

    public static void deployStatement(String strategyName, String moduleName, String statementName, String alias, Object[] params, Object callback) {

        internalDeployStatement(strategyName, moduleName, statementName, alias, params, callback);
    }

    private static void internalDeployStatement(String strategyName, String moduleName, String statementName, String alias, Object[] params, Object callback) {

        EPAdministrator administrator = getServiceProvider(strategyName).getEPAdministrator();

        // do nothing if the statement already exists
        EPStatement oldStatement = administrator.getStatement(statementName);
        if (oldStatement != null && oldStatement.isStarted()) {
            logger.warn(statementName + " is already deployed and started");
            return;
        }

        // read the statement from the module
        EPDeploymentAdmin deployAdmin = administrator.getDeploymentAdmin();

        Module module;
        try {
            module = deployAdmin.read("module-" + moduleName + ".epl");
        } catch (Exception e) {
            throw new RuntimeException("module" + moduleName + " could not be read", e);
        }

        // go through all statements in the module
        EPStatement newStatement = null;
        items: for (ModuleItem item : module.getItems()) {
            String exp = item.getExpression();

            // get the ObjectModel for the statement
            EPStatementObjectModel model;
            if (exp.contains("?")) {
                EPPreparedStatementImpl prepared = ((EPPreparedStatementImpl) administrator.prepareEPL(exp));
                model = prepared.getModel();
            } else {
                model = administrator.compileEPL(exp);
            }

            // go through all annotations and check if the statement has the 'name' 'statementName'
            List<AnnotationPart> annotationParts = model.getAnnotations();
            for (AnnotationPart annotationPart : annotationParts) {
                if (annotationPart.getName().equals("Name")) {
                    for (AnnotationAttribute attribute : annotationPart.getAttributes()) {
                        if (attribute.getValue().equals(statementName)) {

                            // create the statement and set the prepared statement params if a prepared statement
                            if (exp.contains("?")) {
                                EPPreparedStatement prepared = administrator.prepareEPL(exp);
                                for (int i = 0; i < params.length; i++) {
                                    prepared.setObject(i + 1, params[i]);
                                }
                                if (alias != null) {
                                    newStatement = administrator.create(prepared, alias);
                                } else {
                                    newStatement = administrator.create(prepared);
                                }
                            } else {
                                if (alias != null) {
                                    newStatement = administrator.createEPL(exp, alias);
                                } else {
                                    newStatement = administrator.createEPL(exp);
                                }
                            }

                            // process annotations
                            processAnnotations(strategyName, newStatement);

                            // attach the callback if supplied (will override the Subscriber defined in Annotations)
                            if (callback != null) {
                                newStatement.setSubscriber(callback);
                            }

                            // break iterating over the statements
                            break items;
                        }
                    }
                }
            }
        }

        if (newStatement == null) {
            logger.warn("statement " + statementName + " was not found");
        } else {
            logger.debug("deployed statement " + newStatement.getName() + " on service provider: " + strategyName);
        }
    }

    public static void deployModule(String strategyName, String moduleName) {

        EPAdministrator administrator = getServiceProvider(strategyName).getEPAdministrator();
        EPDeploymentAdmin deployAdmin = administrator.getDeploymentAdmin();

        DeploymentResult deployResult;
        try {
            Module module = deployAdmin.read("module-" + moduleName + ".epl");
            deployResult = deployAdmin.deploy(module, new DeploymentOptions());
        } catch (Exception e) {
            throw new RuntimeException("module " + moduleName + " could not be deployed", e);
        }

        for (EPStatement statement : deployResult.getStatements()) {

            // check if the statement should be excluded
            if (moduleDeployExcludeStatements.contains(statement.getName())) {
                statement.destroy();
                continue;
            }

            // check if the statement is elgible, other destory it righ away
            processAnnotations(strategyName, statement);
        }

        logger.debug("deployed module " + moduleName + " on service provider: " + strategyName);
    }

    public static void deployAllModules(String strategyName) {

        Strategy strategy = ServiceLocator.instance().getLookupService().getStrategyByName(strategyName);
        String[] modules = strategy.getModules().split(",");
        for (String module : modules) {
            if (module != null && !module.equals("")) {
                deployModule(strategyName, module);
            }
        }
    }


    /**
     * @param statementNameRegex statement name regular expression
     */
    public static String[] findStatementNames(String strategyName, final String statementNameRegex) {

        EPAdministrator administrator = getServiceProvider(strategyName).getEPAdministrator();

        // find the first statement that matches the given statementName regex
        return CollectionUtils.select(Arrays.asList(administrator.getStatementNames()), new Predicate<String>() {

            @Override
            public boolean evaluate(String statement) {
                return statement.matches(statementNameRegex);
            }
        }).toArray(new String[] {});
    }


    /**
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

    public static void undeployStatement(String strategyName, String statementName) {

        // destroy the statement
        EPStatement statement = getServiceProvider(strategyName).getEPAdministrator().getStatement(statementName);

        if (statement != null && statement.isStarted()) {
            statement.destroy();
            logger.debug("undeployed statement " + statementName);
        }
    }

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

    public static void sendEvent(String strategyName, Object obj) {

        if (simulation) {
            if (isInitialized(strategyName)) {

                long startTime = System.nanoTime();

                getServiceProvider(strategyName).getEPRuntime().sendEvent(obj);

                MetricsUtil.accountEnd("EsperManager." + strategyName + "." + ClassUtils.getShortClassName(obj.getClass()), startTime);
            }
        } else {

            // check if it is the localStrategy
            if (StrategyUtil.getStartedStrategyName().equals(strategyName)) {
                getServiceProvider(strategyName).getEPRuntime().sendEvent(obj);
            } else {
                sendExternalEvent(strategyName, obj);
            }
        }
    }

    public static void routeEvent(String strategyName, Object obj) {

        if (simulation) {
            if (isInitialized(strategyName)) {
                getServiceProvider(strategyName).getEPRuntime().route(obj);
            }
        } else {

            // check if it is the localStrategy
            if (StrategyUtil.getStartedStrategyName().equals(strategyName)) {
                getServiceProvider(strategyName).getEPRuntime().route(obj);
            } else {
                sendExternalEvent(strategyName, obj);
            }
        }
    }

    private static void sendExternalEvent(String strategyName, Object obj) {

        // sent to the strateyg queue
        getStrategyTemplate().convertAndSend(strategyName + ".QUEUE", obj);

        logger.trace("propagated event to " + strategyName + " " + obj);
    }


    public static void sendMarketDataEvent(final MarketDataEvent marketDataEvent) {

        if (simulation) {
            for (Subscription subscription : marketDataEvent.getSecurity().getSubscriptions()) {
                if (!subscription.getStrategy().getName().equals(StrategyImpl.BASE)) {
                    sendEvent(subscription.getStrategy().getName(), marketDataEvent);
                }
            }

        } else {

            // send using the jms template
            getMarketDataTemplate().convertAndSend(marketDataEvent, new MessagePostProcessor() {

                @Override
                public Message postProcessMessage(Message message) throws JMSException {

                    // ad securityId Property
                    message.setIntProperty("securityId", marketDataEvent.getSecurity().getId());
                    return message;
                }
            });
        }
    }

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

    public static Object getLastEvent(String strategyName, String statementName) {

        EPStatement statement = getServiceProvider(strategyName).getEPAdministrator().getStatement(statementName);
        if (statement != null && statement.isStarted()) {
            SafeIterator<EventBean> it = statement.safeIterator();
            try {
                if (it.hasNext()) {
                    return it.next().getUnderlying();
                }
            } finally {
                it.close();
            }
        }
        return null;
    }

    public static Object getLastEventProperty(String strategyName, String statementName, String property) {

        EPStatement statement = getServiceProvider(strategyName).getEPAdministrator().getStatement(statementName);
        if (statement != null && statement.isStarted()) {
            SafeIterator<EventBean> it = statement.safeIterator();
            try {
                return it.next().get(property);
            } finally {
                it.close();
            }
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    public static List getAllEvents(String strategyName, String statementName) {

        EPStatement statement = getServiceProvider(strategyName).getEPAdministrator().getStatement(statementName);
        List<Object> list = new ArrayList<Object>();
        if (statement != null && statement.isStarted()) {
            SafeIterator<EventBean> it = statement.safeIterator();
            try {
                while (it.hasNext()) {
                    EventBean bean = it.next();
                    Object underlying = bean.getUnderlying();
                    list.add(underlying);
                }
            } finally {
                it.close();
            }
        }
        return list;
    }

    @SuppressWarnings("rawtypes")
    public static List getAllEventsProperty(String strategyName, String statementName, String property) {

        EPStatement statement = getServiceProvider(strategyName).getEPAdministrator().getStatement(statementName);
        List<Object> list = new ArrayList<Object>();
        if (statement != null && statement.isStarted()) {
            SafeIterator<EventBean> it = statement.safeIterator();
            try {
                while (it.hasNext()) {
                    EventBean bean = it.next();
                    Object underlying = bean.get(property);
                    list.add(underlying);
                }
            } finally {
                it.close();
            }
        }
        return list;
    }

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

    public static boolean isInternalClock(String strategyName) {

        return internalClock.get(strategyName);
    }

    public static void setCurrentTime(CurrentTimeEvent currentTimeEvent) {

        // sent currentTime to all local engines
        for (String providerURI : EPServiceProviderManager.getProviderURIs()) {
            sendEvent(providerURI, currentTimeEvent);
        }
    }

    public static long getCurrentTime(String strategyName) {

        return getServiceProvider(strategyName).getEPRuntime().getCurrentTime();
    }

    public static void initCoordination(String strategyName) {

        coordinators.put(strategyName, new AdapterCoordinatorImpl(getServiceProvider(strategyName), true, true));
    }

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


    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void coordinate(String strategyName, Collection baseObjects, String timeStampColumn) {

        InputAdapter inputAdapter = new DBInputAdapter(getServiceProvider(strategyName), baseObjects, timeStampColumn);
        coordinators.get(strategyName).coordinate(inputAdapter);
    }

    public static void coordinateTicks(String strategyName, Date startDate) {

        InputAdapter inputAdapter = new BatchDBTickInputAdapter(getServiceProvider(strategyName), startDate);
        coordinators.get(strategyName).coordinate(inputAdapter);
    }

    public static void startCoordination(String strategyName) {

        coordinators.get(strategyName).start();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void setVariableValue(String strategyName, String variableName, String value) {

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
        }
    }

    public static void setVariableValue(String strategyName, String variableName, Object value) {

        variableName = variableName.replace(".", "_");
        EPRuntime runtime = getServiceProvider(strategyName).getEPRuntime();
        if (runtime.getVariableValueAll().containsKey(variableName)) {
            runtime.setVariableValue(variableName, value);
        }
    }

    public static Object getVariableValue(String strategyName, String key) {

        key = key.replace(".", "_");
        EPRuntime runtime = getServiceProvider(strategyName).getEPRuntime();
        return runtime.getVariableValue(key);
    }

    public static void addTradeCallback(String strategyName, Collection<Order> orders, TradeCallback callback) {

        if (orders.size() == 0) {
            throw new IllegalArgumentException("at least 1 order has to be specified");
        }

        // get the securityIds sorted asscending
        Set<Integer> sortedSecurityIds = new TreeSet<Integer>(CollectionUtils.collect(orders, new Transformer<Order, Integer>() {

            @Override
            public Integer transform(Order order) {
                return order.getSecurity().getId();
            }
        }));

        if (sortedSecurityIds.size() < orders.size()) {
            throw new IllegalArgumentException("cannot place multiple orders for the same security at the same time");
        }

        // get the statement alias based on all security ids
        String alias = "ON_TRADE_COMPLETED_" + StringUtils.join(sortedSecurityIds, "_");

        if (isDeployed(strategyName, alias)) {

            logger.warn(alias + " is already deployed");
        } else {

            deployStatement(strategyName, "prepared", "ON_TRADE_COMPLETED", alias, new Object[] { sortedSecurityIds.size(), sortedSecurityIds }, callback);
        }
    }

    public static void addFirstTickCallback(String strategyName, int[] securityIds, TickCallback callback) {

        // sort the securityIds
        Arrays.sort(securityIds);

        // get unique values
        Set<Integer> sortedSecurityIds = new TreeSet<Integer>();
        sortedSecurityIds.addAll(Arrays.asList(ArrayUtils.toObject(securityIds)));

        if (sortedSecurityIds.size() < securityIds.length) {
            throw new IllegalArgumentException("cannot specify same securityId multiple times");
        }
        String alias = "ON_FIRST_TICK_" + StringUtils.join(sortedSecurityIds, "_");

        if (isDeployed(strategyName, alias)) {

            logger.warn(alias + " is already deployed");
        } else {

            deployStatement(strategyName, "prepared", "ON_FIRST_TICK", alias, new Object[] { sortedSecurityIds.size(), sortedSecurityIds }, callback);
        }
    }

    @SuppressWarnings("unchecked")
    public static void logStatementMetrics() {

        for (Map.Entry<String, EPServiceProvider> entry : serviceProviders.entrySet()) {
            List<StatementMetricVO> metrics = getAllEvents(entry.getKey(), "METRICS");
            for (StatementMetricVO metric : metrics) {
                logger.info(metric.getEngineURI() + "." + metric.getStatementName() + ": " + metric.getNumInput() + " events " + metric.getWallTime() + " millis");
            }
        }
    }

    private static String getProviderURI(String strategyName) {

        return (strategyName == null || "".equals(strategyName)) ? StrategyImpl.BASE : strategyName.toUpperCase();
    }

    private static EPServiceProvider getServiceProvider(String strategyName) {

        String providerURI = getProviderURI(strategyName);

        EPServiceProvider serviceProvider = serviceProviders.get(providerURI);
        if (serviceProvider == null) {
            throw new RuntimeException("strategy " + providerURI + " is not initialized yet!");
        }

        return serviceProvider;
    }

    /**
     * initialize all the variables from the Configuration
     */
    private static void initVariables(String strategyName, Configuration configuration) {

        try {
            Map<String, ConfigurationVariable> variables = configuration.getVariables();
            for (Map.Entry<String, ConfigurationVariable> entry : variables.entrySet()) {
                String variableName = entry.getKey().replace("_", ".");
                String value = ServiceLocator.instance().getConfiguration().getString(strategyName, variableName);
                if (value != null) {
                    Class<?> clazz = Class.forName(entry.getValue().getType());
                    Object castedObj = JavaClassHelper.parse(clazz, value);
                    entry.getValue().setInitializationValue(castedObj);
                }
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static void processAnnotations(String strategyName, EPStatement statement) {

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

                Listeners listeners = (Listeners)annotation;
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
            } else if (annotation instanceof RunTimeOnly && simulation) {

                statement.destroy();
                return;

            } else if (annotation instanceof SimulationOnly && !simulation) {

                statement.destroy();
                return;

            } else if (annotation instanceof Condition) {

                Condition condition = (Condition) annotation;
                String key = condition.key();
                if (!ServiceLocator.instance().getConfiguration().getBoolean(strategyName, key)) {
                    statement.destroy();
                    return;
                }
            }
        }
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

    // manual lookup of templates since they are only available if applicationContext-jms.xml is active
    private static JmsTemplate getStrategyTemplate() {

        if (strategyTemplate == null) {
            strategyTemplate = ServiceLocator.instance().getService("strategyTemplate", JmsTemplate.class);
        }
        return strategyTemplate;
    }

    private static JmsTemplate getMarketDataTemplate() {

        if (marketDataTemplate == null) {
            marketDataTemplate = ServiceLocator.instance().getService("marketDataTemplate", JmsTemplate.class);
        }
        return marketDataTemplate;
    }
}
