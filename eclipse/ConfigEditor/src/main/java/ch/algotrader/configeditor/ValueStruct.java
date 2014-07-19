package ch.algotrader.configeditor;

import java.util.ArrayList;
import java.util.List;

public class ValueStruct {
    public Object value;
    public List<String> comments;
    public String inlineComment;

    public ValueStruct() {
        comments = new ArrayList<String>();
    }

    public ValueStruct(Object pValue) {
        comments = new ArrayList<String>();
        value = pValue;
    }
}
