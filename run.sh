#!/bin/sh

cd /usr/local/AlgoTrader

CP=classes
for name in lib/*.jar ; do
  CP=$CP:$name
done

java -cp $CP com.algoTrader.starter.TickStarter

