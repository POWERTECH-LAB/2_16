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
package com.algoTrader.starter;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import com.algoTrader.ServiceLocator;
import com.algoTrader.service.ui.UIReconciliationService;

/**
 * @author <a href="mailto:andyflury@gmail.com">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class UIReconciliationStarter {

    public static void main(String[] args) throws IOException {

        ServiceLocator.instance().init(ServiceLocator.LOCAL_BEAN_REFERENCE_LOCATION);
        UIReconciliationService service = ServiceLocator.instance().getService("uIReconciliationService", UIReconciliationService.class);

        for (String fileName : args) {

            File file = new File(fileName);
            InputStream bis = new BufferedInputStream(new FileInputStream(file));
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            IOUtils.copy(bis, bos);

            service.reconcile(file.getName(), bos.toByteArray());
        }

        ServiceLocator.instance().shutdown();
    }
}
