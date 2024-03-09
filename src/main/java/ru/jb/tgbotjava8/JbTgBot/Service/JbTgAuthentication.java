package ru.jb.tgbotjava8.JbTgBot.Service;

import org.telegram.telegrambots.meta.api.objects.Message;

public interface JbTgAuthentication {
    public boolean Verify(Message message);
    public void AddOtp(String otp);

}
