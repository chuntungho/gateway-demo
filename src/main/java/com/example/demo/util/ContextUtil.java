package com.example.demo.util;

import brave.baggage.BaggageField;

import java.util.Locale;


public class ContextUtil {

    // NOTE case-sensitive for sleuth
    private static final String IP_FIELD = "x-context-ip";
    private static final String LOCALE_FIELD = "x-context-locale";
    private static final String USER_ID_FIELD = "x-context-user-id";

    public static void initIpAndLocale(String ip, Locale locale) {
        setValue(IP_FIELD, ip);
        if (locale != null) {
            setValue(LOCALE_FIELD, locale.toString());
        }
    }

    public static String getIp() {
        return getValue(IP_FIELD);
    }

    private static void setValue(String name, String value) {
        BaggageField field = BaggageField.getByName(name);
        if (field != null) {
            field.updateValue(value);
        }
    }

    private static String getValue(String name) {
        BaggageField field = BaggageField.getByName(name);
        if (field == null) return null;
        return field.getValue();
    }

}
