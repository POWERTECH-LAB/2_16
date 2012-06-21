#!/bin/sh

cd $ALGOTRADER_HOME

nohup java \
-cp lib/*:`cat bin/cp.txt` \
-DstrategyName=BASE \
-Dcom.algoTrarder.rmi.registryPort=1099 \
-Dcom.algoTrarder.rmi.serverPort=1098 \
-Djava.rmi.server.hostname=127.0.0.1 \
-Dcom.sun.management.jmxremote.authenticate=false \
-Dcom.sun.management.jmxremote.ssl=true \
-Dcom.sun.management.jmxremote.ssl.need.client.auth=true \
-Dcom.sun.management.jmxremote.registry.ssl=true \
-Djavax.net.ssl.keyStore=$ALGOTRADER_HOME/bin/keystore \
-Djavax.net.ssl.keyStorePassword=Zermatt11 \
-Djavax.net.ssl.trustStore=$ALGOTRADER_HOME/bin/truststore \
-Djavax.net.ssl.trustStorePassword=Zermatt11 \
-Dlog4j.configuration=log4j-prod.xml \
-Denvironment=$ENVIRONMENT \
-javaagent:lib/agent.jar \
-XX:MaxHeapFreeRatio=10 \
-XX:MinHeapFreeRatio=5 \
-XX:-UseParallelGC \
-Dstatement.processCashTransactions=true \
-Dstatement.setMargins=false \
com.algoTrader.starter.MarketDataStarter \
> log/nohup.log 2>&1 &

