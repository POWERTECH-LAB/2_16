#!/bin/sh

cd "`dirname \"$0\"`"/..

java \
-cp "lib/*" \
-DstrategyName=SERVER \
-Dcom.sun.management.jmxremote.port=1099 \
-Dcom.sun.management.jmxremote.authenticate=false \
-Dcom.sun.management.jmxremote.ssl=false \
-Dspring.profiles.active=live,pooledDataSource,iBMarketData,iBNative,iBHistoricalData \
ch.algotrader.starter.ServerStarter \
