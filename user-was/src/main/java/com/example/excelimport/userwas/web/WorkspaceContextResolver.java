package com.example.excelimport.userwas.web;

import com.example.excelimport.userwas.exception.UserWasException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class WorkspaceContextResolver {

    public static final String WS_SESSION_KEY = "USER_WORKSPACE_ID";

    public UUID resolve(UUID workspaceId, UUID tenantId, HttpServletRequest request) {
        if (workspaceId != null) {
            setWorkspaceSession(request, workspaceId);
            return workspaceId;
        }
        if (tenantId != null) {
            setWorkspaceSession(request, tenantId);
            return tenantId;
        }
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object attr = session.getAttribute(WS_SESSION_KEY);
            if (attr != null) {
                return UUID.fromString(attr.toString());
            }
        }
        throw new UserWasException(400, "workspace_id is required");
    }

    public void setWorkspaceSession(HttpServletRequest request, UUID workspaceId) {
        request.getSession(true).setAttribute(WS_SESSION_KEY, workspaceId.toString());
    }

    public String getWorkspaceSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (String) session.getAttribute(WS_SESSION_KEY);
    }
}

