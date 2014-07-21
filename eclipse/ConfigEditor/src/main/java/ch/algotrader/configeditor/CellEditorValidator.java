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
package ch.algotrader.configeditor;

import java.util.regex.Pattern;

import org.eclipse.jface.viewers.ICellEditorValidator;

import ch.algotrader.configeditor.editingsupport.PropertyDefExtensionPoint;

public class CellEditorValidator implements ICellEditorValidator {

  private final EditorPropertyPage propertyPage;
  private final String dataType;
  private final String key;

  public CellEditorValidator(EditorPropertyPage propertyPage, String dataType, String pKey) {
    this.propertyPage = propertyPage;
    this.dataType = dataType;
    key = pKey;
  }

  @Override
  public String isValid(Object value) {
    FieldModel model = propertyPage.getFieldModel(propertyPage.getSelectedFile(), key);
    if (model.getRequired() && (value == null || value.toString().equals(""))) {
      String label = model.getLabel();
      if (label == null)
        label = key;
      return "The property \"" + label + "\" is required";
    }
    String regex = PropertyDefExtensionPoint.getRegex(dataType);
    if (regex == null)
      return null;
    if (Pattern.matches(regex, value.toString()))
      return null;
    return PropertyDefExtensionPoint.getRegexErrorMessage(dataType, value.toString());
  }
}
