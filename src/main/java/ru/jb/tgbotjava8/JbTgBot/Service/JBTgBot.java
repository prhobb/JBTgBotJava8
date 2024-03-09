package ru.jb.tgbotjava8.JbTgBot.Service;

import ru.jb.tgbotjava8.JbTgBot.Extensions.TelegramMessage;
import ru.jb.tgbotjava8.JbTgBot.Listeners.JbTgBotListener;

public interface JBTgBot {
    public void Send(TelegramMessage message);

    public void AddListener(JbTgBotListener listener);
}
