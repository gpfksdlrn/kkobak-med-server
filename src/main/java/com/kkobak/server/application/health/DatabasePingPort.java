package com.kkobak.server.application.health;

// "DB가 살아있는지 알려줘"라는 요구사항. 어떻게 확인할지(JDBC 등)는 모르고, 구현은 infrastructure 가 맡는다.
public interface DatabasePingPort {

    boolean isDatabaseReachable();
}
