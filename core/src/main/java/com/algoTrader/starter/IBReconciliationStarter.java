package com.algoTrader.starter;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.algoTrader.ServiceLocator;
import com.algoTrader.service.ib.IBReconciliationService;

public class IBReconciliationStarter {

    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {

        String fileName = args[0];

        ServiceLocator.instance().init(ServiceLocator.LOCAL_BEAN_REFERENCE_LOCATION);
        IBReconciliationService service = ServiceLocator.instance().getService("iBReconciliationService", IBReconciliationService.class);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new File("files" + File.separator + "ib" + File.separator + fileName));

        for (int i = 1; i < args.length; i++) {

            String type = args[i];
            if ("CASH".equals(type)) {
                service.processCashTransactions(document);
            } else if ("POSITIONS".equals(type)) {
                service.reconcilePositions(document);
            } else if ("TRADES".equals(type)) {
                service.reconcileTrades(document);
            } else if ("UNBOOKED_TRADES".equals(type)) {
                service.reconcileUnbookedTrades(document);
            }
        }

        ServiceLocator.instance().shutdown();
    }
}
