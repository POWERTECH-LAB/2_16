package ch.algotrader.configeditor.editingsupport;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.widgets.Composite;

public class DoubleCellEditorFactory implements CellEditorFactory {

    @Override
    public CellEditor createCellEditor(Composite parent) {
        return new DoubleCellEditor(parent);
    }
}
