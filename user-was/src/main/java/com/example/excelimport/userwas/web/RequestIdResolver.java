package com.example.excelimport.userwas.web;

import com.example.excelimport.common.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class RequestIdResolver {

    public String resolve(HttpServletRequest request) {
        Object attr = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTR);
        return attr == null ? "" : attr.toString();
    }
}
