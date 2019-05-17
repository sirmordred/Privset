package com.mordred.privset;

import android.content.SharedPreferences;
import android.graphics.Color;

/**
 * Created by mordred on 18.05.2017.
 */

public class ConfigNodes {
    public String configString;
    public String configStringValue;
    public String configTitle;
    public String type;
    public String newValue = "";

    public int color = Color.parseColor("#80CBC6");

    private SharedPreferences preferences;

    public ConfigNodes(String configString, String configStringValue, String type, SharedPreferences preferences) {
        this.configString = configString;
        this.configStringValue = configStringValue;
        this.type = type;
        this.preferences = preferences;
        this.configTitle = getTitleFromConfig(configString);

        setNodeToDefPref();
        if (!getNodeFromPref().equals(getNodeFromDefPref())) {
            // Dark Green
            color = Color.parseColor("#FF5353");
        }
    }

    public void setNodeToPref(String s) {
        switch (type) {
            case "integer":
                preferences.edit().putInt(configString, Integer.parseInt(s)).commit();
                break;
            case "bool":
                preferences.edit().putBoolean(configString, Boolean.parseBoolean(s)).commit();
                break;
            case "string":
                preferences.edit().putString(configString, s).commit();
                break;
        }
    }

    public String getNodeFromPref() {
        String res = null;
        switch (type) {
            case "integer":
                res = String.valueOf(preferences.getInt(configString, Integer.parseInt(getNodeFromDefPref())));
                break;
            case "bool":
                res = String.valueOf(preferences.getBoolean(configString, Boolean.parseBoolean(getNodeFromDefPref())));
                break;
            case "string":
                res = preferences.getString(configString, getNodeFromDefPref());
                break;
        }
        return res;
    }

    public void delNodeFromPref() {
        if (preferences.contains(configString)) {
            preferences.edit().remove(configString).commit();
        }
    }

    public void setNodeToDefPref() {
        if (!preferences.contains(configString + "Default")) {
            switch (type) {
                case "integer":
                    preferences.edit().putInt(configString + "Default", Integer.parseInt(configStringValue)).commit();
                    break;
                case "bool":
                    preferences.edit().putBoolean(configString + "Default", Boolean.parseBoolean(configStringValue)).commit();
                    break;
                case "string":
                    preferences.edit().putString(configString + "Default", configStringValue).commit();
                    break;
            }
        }
    }

    public String getNodeFromDefPref() {
        String res = null;
        switch (type) {
            case "integer":
                res = String.valueOf(preferences.getInt(configString + "Default", Integer.parseInt(configStringValue)));
                break;
            case "bool":
                res = String.valueOf(preferences.getBoolean(configString + "Default", Boolean.parseBoolean(configStringValue)));
                break;
            case "string":
                res = preferences.getString(configString + "Default", configStringValue);
                break;
        }
        return res;
    }

    public String getXmlStringFromPref() {
        String res = null;
        switch (type) {
            case "integer":
                res = "<integer name=\"" + configString + "\">" + getNodeFromPref() + "</integer>\n";
                break;
            case "bool":
                res = "<bool name=\"" + configString + "\">" + getNodeFromPref() + "</bool>\n";
                break;
            case "string":
                res = "<string name=\"" + configString + "\">" + getNodeFromPref() + "</string>\n";
                break;
        }
        return res;
    }

    public String getXmlStringFromEdx() {
        String res = null;
        switch (type) {
            case "integer":
                res = "<integer name=\"" + configString + "\">" + newValue + "</integer>\n";
                break;
            case "bool":
                res = "<bool name=\"" + configString + "\">" + newValue + "</bool>\n";
                break;
            case "string":
                res = "<string name=\"" + configString + "\">" + newValue + "</string>\n";
                break;
        }
        return res;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public String getType() {
        return type;
    }

    private String getTitleFromConfig(String str) {
        String tempStr = str.replace("config_","").replace("_"," ");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tempStr.length(); i++) {
            if (i != 0) {
                if (Character.isSpaceChar(tempStr.charAt(i - 1)) && Character.isLowerCase(tempStr.charAt(i))) {
                    sb.append(Character.toUpperCase(tempStr.charAt(i)));
                    continue;
                }
                if (Character.isUpperCase(tempStr.charAt(i))) {
                    if(!Character.isSpaceChar(tempStr.charAt(i - 1)) && Character.isLowerCase(tempStr.charAt(i - 1))) {
                        sb.append(' ');
                    }
                }
                sb.append(tempStr.charAt(i));
            } else {
                if(Character.isLowerCase(tempStr.charAt(i))) {
                    sb.append(Character.toUpperCase(tempStr.charAt(i)));
                } else {
                    sb.append(tempStr.charAt(i));
                }
            }
        }
        return sb.toString();
    }

    public String getConfigTitle() {
        return configTitle;
    }
}
