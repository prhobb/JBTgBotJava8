package ru.jb.tgbotjava8.JBTlsServer;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
public class JbTlsServerPendingMessage {
    int messageNumber;
    LocalDateTime date;
    byte[] buffer;
}
