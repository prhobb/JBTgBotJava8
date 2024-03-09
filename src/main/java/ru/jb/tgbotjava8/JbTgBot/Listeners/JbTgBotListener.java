package ru.jb.tgbotjava8.JbTgBot.Listeners;


import ru.jb.tgbotjava8.JbTgBot.Extensions.TelegramMessage;

public interface JbTgBotListener {
    String OnTgReceive(TelegramMessage message);
}
