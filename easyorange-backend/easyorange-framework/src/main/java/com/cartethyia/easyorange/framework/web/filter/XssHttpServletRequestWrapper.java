package com.cartethyia.easyorange.framework.web.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.web.util.HtmlUtils;

public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

    public XssHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String[] getParameterValues(String parameter) {
        String[] values = super.getParameterValues(parameter);
        if (values == null) {
            return null;
        }
        int count = values.length;
        String[] encodedValues = new String[count];
        for (int i = 0; i < count; i++) {
            encodedValues[i] = stripXss(values[i]);
        }
        return encodedValues;
    }

    @Override
    public String getParameter(String parameter) {
        String value = super.getParameter(parameter);
        return stripXss(value);
    }

    private static final java.util.Set<String> SAFE_HEADERS = java.util.Set.of(
        "Authorization",
        "Content-Type",
        "Content-Length",
        "Accept",
        "Accept-Encoding",
        "Accept-Language",
        "Cache-Control",
        "Connection",
        "Host",
        "Origin",
        "Referer",
        "User-Agent"
    );

    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        if (value != null && !SAFE_HEADERS.contains(name)) {
            value = stripXss(value);
        }
        return value;
    }

    private String stripXss(String value) {
        if (value != null) {
            value = HtmlUtils.htmlEscape(value);
        }
        return value;
    }
}
