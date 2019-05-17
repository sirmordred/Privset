package com.mordred.privset;

import java.io.Serializable;

/**
 * Created by mordred on 29.10.2017.
 */

public class TempConfigNodes implements Serializable {

    public String getTitle2() {
        return title2;
    }

    public String getDescription2() {
        return description2;
    }

    public String getDefaultValue2() {
        return defaultValue2;
    }

    public String getCurrentValue2() {
        return currentValue2;
    }

    public TempConfigNodes(String title2, String description2, String defaultValue2, String currentValue2) {
        this.title2 = title2;
        this.description2 = description2;
        this.defaultValue2 = defaultValue2;
        this.currentValue2 = currentValue2;
    }

    public String title2;
    public String description2;
    public String defaultValue2;
    public String currentValue2;


}
