package ru.jb.tgbotjava8.JbTgBot.Service.impl;

//import lombok.extern.log4j.Log4j2;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
        import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.jb.tgbotjava8.DataBase.Entity.JbTgChat;
import ru.jb.tgbotjava8.DataBase.Repository.JbTgChatsRepository;
import ru.jb.tgbotjava8.JbTgBot.Extensions.TelegramMessage;
import ru.jb.tgbotjava8.JbTgBot.Listeners.JbTgBotListener;
//import ru.jb.tgbotjava8.JbTgBot.Service.impl.JbTgAuthentication;
import ru.jb.tgbotjava8.JbTgBot.Service.JBTgBot;
import ru.jb.tgbotjava8.JbTgBot.Service.JbTgAuthentication;
import ru.jb.tgbotjava8.config.TgBotConfig;

import java.util.*;


@Log4j2
@Service
public class JbTelegramBotImpl extends TelegramLongPollingBot  implements JBTgBot {
    public final static String START_COMMAND="start";
    TgBotConfig tgBotConfig;
    String botName ;
    JbTgAuthentication jbTgAuthenticationService;
    JbTgChatsRepository jbTgChatsRepository;

    private List<JbTgBotListener> listeners;

    Map<Long,JbTgChat> jbTgChats;

    @Override
    public String getBotUsername() {
        return botName;
    }


    @Autowired
    public JbTelegramBotImpl(TgBotConfig tgBotConfig, JbTgAuthentication jbTgAuthenticationService, JbTgChatsRepository jbTgChatsRepository) {
        super(tgBotConfig.getToken());
        this.tgBotConfig = tgBotConfig;
        this.botName = tgBotConfig.getBotName();
        this.jbTgAuthenticationService =jbTgAuthenticationService;
        this.jbTgChatsRepository=jbTgChatsRepository;

        jbTgChats = new HashMap<Long,JbTgChat>();
        jbTgChatsRepository.findAll().forEach(jbTgChat -> jbTgChats.put(jbTgChat.getId(),jbTgChat));

        listeners = new ArrayList<JbTgBotListener>();
    }

    /**
     * Process received Telegram message.
     * If user Authenticated: process hello message without sending it to SSLClient backend
     * or send message to SslClient if other.
     * If user NOT Authenticated: silently discard message.
     * @param update
     */
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            if (jbTgAuthenticationService.Verify(update.getMessage())) {
                String messageText = update.getMessage().getText();
                if(!jbTgChats.containsKey(update.getMessage().getChatId()))
                    AddChat(new JbTgChat(update.getMessage().getChatId()));
                switch (messageText.toLowerCase()){
                    case START_COMMAND:
                        startCommandReceived(update.getMessage().getChatId(), update.getMessage().getChat().getFirstName());
                        break;
                    default:
                        TelegramMessage telegramMessage = TelegramMessage.GetTelegramMessage(messageText, update.getMessage().getChatId());
                        if (telegramMessage != null)
                            listeners.forEach(listener -> {
                                sendMessageToTg(update.getMessage().getChatId(), listener.OnTgReceive(telegramMessage));
                            });
                        else {
                            log.error("Wrong telegramMessage");
                            sendMessageToTg(update.getMessage().getChatId(),"Error while parse telegramMessage");
                        }
                }

            }
        }
    }

    private void startCommandReceived(Long chatId, String name) {
        String answer = "Hi!";
        sendMessageToTg(chatId, answer);
    }

    private void sendMessageToTg(Long chatId, String textToSend){
        if (textToSend != "" && chatId>0) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(String.valueOf(chatId));
            sendMessage.setText(textToSend);
            try {
                execute(sendMessage);
            } catch (TelegramApiException e) {
                log.error("Error while sending message to Telegram. ", e);
            }
        }
    }
    private void sendPhotoToTg(Long chatId, InputFile photoToSend){
        if (photoToSend != null && chatId>0) {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(String.valueOf(chatId));
            sendPhoto.setPhoto(photoToSend);
            try {
                execute(sendPhoto);
            } catch (TelegramApiException e) {
                log.error("Error while sending message to Telegram. ", e);
            }
        }
    }

    @EventListener({ContextRefreshedEvent.class})
    private void init()throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        try{
            telegramBotsApi.registerBot(this);
        } catch (TelegramApiException e){
            log.error(e);
        }
    }

    /**
     * Sends text ot photo to telegram
     * @param message
     */
    @Override
    public void Send(TelegramMessage message) {
        Set<JbTgChat> chats2send;
        if (message.getChatId() == 0)
            chats2send = new HashSet<>(this.jbTgChats.values());
        else {
            chats2send=new HashSet<>(1);
            chats2send.add(new JbTgChat(message.getChatId()));
        }

            switch (message.getType()) {
                case STRING_OBJECT:
                    chats2send.forEach(chat -> {
                        sendMessageToTg(chat.getId(), message.toString());
                    });
                    break;
                case BITMAP_OBJECT:
                    chats2send.forEach(chat -> {
                    sendPhotoToTg(chat.getId(), message.getImage());
                    });
                    break;
            }
    }


    @Override
    public void AddListener(JbTgBotListener listener) {
        listeners.add(listener);
    }

    private void AddChat(JbTgChat newJbTgChat){
        jbTgChats.put(newJbTgChat.getId(),newJbTgChat);
        jbTgChatsRepository.save(newJbTgChat);
    }

}
