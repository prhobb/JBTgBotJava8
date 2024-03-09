package ru.jb.tgbotjava8.Controllers;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.jb.tgbotjava8.JbTgBot.Extensions.TelegramMessage;
import ru.jb.tgbotjava8.JBTlsServer.Listeners.TLSServerListener;
import ru.jb.tgbotjava8.JBTlsServer.Service.impl.TlsServer;
import ru.jb.tgbotjava8.JbTgBot.Listeners.JbTgBotListener;
import ru.jb.tgbotjava8.JbTgBot.Service.JBTgBot;


@Service
@Log4j2
public class Controller implements TLSServerListener , JbTgBotListener {

    final static String SSL_CLIENT_DISCONNECTED_MESSAGE="SSL_Client_disconnected";
    final static String SSL_CLIENT_CONNECTED_MESSAGE="Connected new SSL_Client";
    JBTgBot telegramBot;
    TlsServer tlsServer;



    @Autowired
    public Controller(JBTgBot telegramBot, TlsServer tlsServer){
        this.telegramBot=telegramBot;
        this.tlsServer=tlsServer;
        telegramBot.AddListener(this);
        tlsServer.AddListener(this);
    }


    @Override
    public void OnSslReceive(byte[] message) {
        TelegramMessage telegramMessage = TelegramMessage.Deserialize(message);
        if(telegramMessage!=null){
            telegramBot.Send(telegramMessage);
        }
        else
            log.error("Received wrong message via SSL server");
    }

    @Override
    public void OnConnected() {
        telegramBot.Send(TelegramMessage.GetTelegramMessage(SSL_CLIENT_CONNECTED_MESSAGE,0));
    }

    @Override
    public void OnDisconnected() {
        telegramBot.Send(TelegramMessage.GetTelegramMessage(SSL_CLIENT_DISCONNECTED_MESSAGE,0));
    }

    @Override
    public String OnTgReceive(TelegramMessage message) {
        return tlsServer.Send(message.Serialize());
    }
}
