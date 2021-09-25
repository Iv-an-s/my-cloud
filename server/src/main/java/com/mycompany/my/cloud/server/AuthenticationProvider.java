package com.mycompany.my.cloud.server;

import java.util.Optional;

public interface AuthenticationProvider {
    void init();
    Optional<String> getNicknameByLoginAndPassword(String login, String password);
    boolean changeNickname(String oldNickname, String newNickname);
    boolean isNickBusy(String nickname);
    void shutdown();
}
