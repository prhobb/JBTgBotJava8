package ru.jb.tgbotjava8.JbTgBot.Extensions;

import lombok.extern.log4j.Log4j2;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Log4j2
public class TelegramMessage {
    public enum Type {
        NONE((byte) 0),
        STRING_OBJECT((byte) 1),
        BITMAP_OBJECT((byte) 2);
        private byte type;

        Type(byte type) {
            this.type = type;
        }

        public byte getValue() {
            return type;
        }

        public static Type getTypeOfByte(byte type){
            switch (type){
                case 1:
                    return STRING_OBJECT;
                case 2:
                    return BITMAP_OBJECT;
                default:
                    return NONE;
            }
        }
    }

    final static int TYPE_POSITION = 0;
    final static int CHAT_ID_POSITION = 1;
    final static int MESSAGE_POSITION = 9;

    TelegramMessage.Type type;



    InputFile image;
    String text;
    long chatId;

    public long getChatId() {
        return chatId;
    }
    public InputFile getImage() {
        return image;
    }

    public TelegramMessage.Type getType() { return type; }

    /**
     * Gets new TelegramMessage. Better to check if null
     * @param obj - Text or Photo
     * @param chatId - Chat ID. It can be 0 If unknown
     * @return new TelegramMessage or null if obj is incorrect
     */
    public static TelegramMessage GetTelegramMessage(Object obj, long chatId)
    {
        //Obj can be text or photo only. If not return null
        if (obj == null) return null;
        TelegramMessage telegramMessage=new TelegramMessage();

        if (obj.getClass()== String.class)
        {
            telegramMessage.text = (String)obj;
            telegramMessage.type = TelegramMessage.Type.STRING_OBJECT;
        }
        else if (obj.getClass() == InputFile.class)
        {
            telegramMessage.image= (InputFile)obj;
            telegramMessage.type = TelegramMessage.Type.BITMAP_OBJECT;
        }
        else
            return null;

        telegramMessage.chatId = chatId;
        return telegramMessage;
    }

    /**
     * Gets bite array represents this TelegramMessage.
     * Structure. 0-bit: type, 1-bit--8bit: chatId(can be 0), 10-bit--end: payload (text or photo)
     * @return
     */
    public byte[] Serialize()
    {

        byte[] result;
        byte[] temp;
        switch (type)
        {
            case STRING_OBJECT:
                temp = text.getBytes();
                break;
            case BITMAP_OBJECT:
                try(InputStream stream = image.getNewMediaStream()) {
                    temp=new byte[stream.available()];
                    stream.read(temp);
                }
                catch (IOException ex){
                    log.error("Can't Serialize TelegramMessage typeof BITMAP_OBJECT. IOException: ",ex);
                    temp = new byte[0];
                }
                break;
            default:
                temp = new byte[0];
                break;
        }
        //Making byte array
        result = new byte[temp.length + MESSAGE_POSITION];

        result[TYPE_POSITION] = type.getValue();
        result[CHAT_ID_POSITION] = (byte)(chatId >> 56);
        result[CHAT_ID_POSITION+1] = (byte)(chatId >> 48);
        result[CHAT_ID_POSITION + 2] = (byte)(chatId >> 40);
        result[CHAT_ID_POSITION + 3] = (byte)(chatId >> 32);
        result[CHAT_ID_POSITION + 4] = (byte)(chatId >> 24);
        result[CHAT_ID_POSITION + 5] = (byte)(chatId >> 16);
        result[CHAT_ID_POSITION + 6] = (byte)(chatId >> 8);
        result[CHAT_ID_POSITION + 7] = (byte) chatId;
        System.arraycopy(temp,0,result,MESSAGE_POSITION,temp.length);

        return result;
    }

    /// <summary>
/// Обязательно проверять на null!!!
/// </summary>
/// <param name="message">Сериализованный TelegramMessage</param>
/// <returns>Возвращает десериализованный TelegramMessage или null если что-то пошло не так</returns>

    /**
     * Deserialize TelegramMessage from byte array. Highly recommend to check if null
     * @param message
     * @return  new TelegramMessage or null if message is incorrect
     */
    public static TelegramMessage Deserialize(byte[] message)
    {
        if(message == null || message.length< MESSAGE_POSITION+1) return null;

        TelegramMessage telegramMessage = new TelegramMessage();

        telegramMessage.type = Type.getTypeOfByte(message[TYPE_POSITION]);
        telegramMessage.chatId= getLongFromByteArray(message, CHAT_ID_POSITION);
        message= Arrays.copyOfRange(message, MESSAGE_POSITION, message.length);
        switch (telegramMessage.type)
        {
            case STRING_OBJECT:
                telegramMessage.text = new String(message, StandardCharsets.UTF_8);
                break;
            case BITMAP_OBJECT:
                InputStream targetStream = new ByteArrayInputStream(message);
                telegramMessage.image = new InputFile(targetStream,"image");
            break;
            default:
                return null;
        }
        return telegramMessage;
    }

    @Override
    public  String toString()
    {
        if(text!="")
        return text;
        else if (image!=null)
            return "Photo present";
        return "Incorrect TelegramMessage";
    }


    private static int getLongFromByteArray(byte[] b, int startPosition)
    {

     if(b==null || b.length-startPosition<8)
         return 0;
        return  b[startPosition+7] & 0xFF |
                (b[startPosition+6] & 0xFF) << 8 |
                (b[startPosition+5] & 0xFF) << 16 |
                (b[startPosition+4] & 0xFF) << 24 |
                (b[startPosition+3] & 0xFF) << 32 |
                (b[startPosition+2] & 0xFF) << 40 |
                (b[startPosition+1] & 0xFF) << 48 |
                (b[startPosition] & 0xFF) << 56;
    }


}
