package com.example.demo.util;

import brave.baggage.BaggageField;
import org.springframework.cloud.sleuth.TraceContext;
import org.springframework.cloud.sleuth.brave.bridge.BraveTraceContext;

import java.util.Locale;


public class ContextUtil {

    // NOTE case-sensitive for sleuth
    private static final String IP_FIELD = "x-context-ip";
    private static final String LOCALE_FIELD = "x-context-locale";
    private static final String USER_ID_FIELD = "x-context-user-id";

    public static void initIpAndLocale(String ip, Locale locale, TraceContext traceContext) {
        setValue(traceContext, IP_FIELD, ip);
        if (locale != null) {
            setValue(traceContext, LOCALE_FIELD, locale.toString());
        }
    }

    public static String getIp() {
        return getValue(IP_FIELD);
    }

    private static void setValue(TraceContext traceContext, String name, String value) {
        brave.propagation.TraceContext braveTraceContext = BraveTraceContext.toBrave(traceContext);
        BaggageField field = BaggageField.getByName(braveTraceContext, name);
        if (field != null) {
            field.updateValue(braveTraceContext, value);
        }
    }

    private static String getValue(String name) {
        BaggageField field = BaggageField.getByName(name);
        if (field == null) return null;
        return field.getValue();
    }

}
