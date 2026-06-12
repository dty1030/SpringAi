package com.maorou.SpringAIDemo.auth;

import org.springframework.stereotype.Component;

@Component
public class ToolAccessPolicy {

    public boolean isToolAllowed(String role, String toolMode) {
        if ("admin".equals(role)) {
            return true;
        }
        if ("trusted".equals(role)) {
            return "none".equals(toolMode)
                    || "time".equals(toolMode)
                    || "rag".equals(toolMode)
                    || "file".equals(toolMode);
        }
        return "none".equals(toolMode)
                || "time".equals(toolMode)
                || "rag".equals(toolMode);
    }
}
