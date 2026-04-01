package com.agent.gateway.core;

public interface CollaborationListener {
    void onStepStart(String agentName, int step);
    void onStepComplete(String agentName, String result);
    void onComplete(String finalResult);
    void onError(String message);
}
