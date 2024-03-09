package ru.jb.tgbotjava8.JbTgBot.Service.impl;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.jb.tgbotjava8.DataBase.Entity.JbTgUser;
import ru.jb.tgbotjava8.DataBase.Repository.JbTgUsersRepository;
import ru.jb.tgbotjava8.JbTgBot.Service.JbTgAuthentication;
import ru.jb.tgbotjava8.JbTgBot.Xml.OneTimePasswords;
import ru.jb.tgbotjava8.config.TgBotConfig;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;




@Log4j2
@Service
public class JbTgAuthenticationImpl implements JbTgAuthentication {

    TgBotConfig tgBotConfig;
    Set<JbTgUser> jbTgUsers;
    JbTgUsersRepository jbTgUsersRepository;
    OneTimePasswords oneTimePasswords;


    @Autowired
    public JbTgAuthenticationImpl(JbTgUsersRepository jbTgUsersRepository, TgBotConfig tgBotConfig) {
        this.tgBotConfig = tgBotConfig;

        //Load Authenticated users
        this.jbTgUsersRepository = jbTgUsersRepository;
        jbTgUsers = new HashSet<>(jbTgUsersRepository.findAll());

        //Load onetime passwords
        oneTimePasswords = new OneTimePasswords();
        //Adding OneTimePassword
        //AddOtp("123");
        try (BufferedReader br = new BufferedReader(new FileReader(tgBotConfig.getOtpfile()))) {
            String body = br.lines().collect(Collectors.joining());
            StringReader reader = new StringReader(body);
            JAXBContext context = JAXBContext.newInstance(OneTimePasswords.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            oneTimePasswords = (OneTimePasswords) unmarshaller.unmarshal(reader);

        }catch (FileNotFoundException e){
            saveOtp();
        } catch (Exception ex) {
            log.error(ex);
            oneTimePasswords = new OneTimePasswords();
        }


    }

    /**
     * Verifies userId in message. Returns true if user already present in  jbTgUsers, if not, trys to Authenticate user
     * via provided text in message and loaded oneTimePasswords
     * @param message
     * @return
     */
    @Override
    public boolean Verify(Message message) {
        if (message != null) {
            if (jbTgUsers.contains(new JbTgUser(message.getFrom().getId())))
                return true;
            else if (Authenticate(message)) {
                AddUser(new JbTgUser(message.getFrom().getId()));
                return true;
            }
        }
        return false;
    }


    private boolean Authenticate(Message message) {
        if (oneTimePasswords.contains(message.getText())) {
            message.setText(JbTelegramBotImpl.START_COMMAND);
            RemoveOtp(message.getText());
            return true;
        }
        return false;
    }

    private void RemoveOtp(String otp){
        oneTimePasswords.remove(otp);
        saveOtp();
    }
    @Override
    public void AddOtp(String otp){
        oneTimePasswords.add(otp);
        saveOtp();
    }

    private void saveOtp() {
        if (oneTimePasswords != null) {
            try (FileWriter fileWriter = new FileWriter(tgBotConfig.getOtpfile())) {
                StringWriter writer = new StringWriter();
                JAXBContext context = JAXBContext.newInstance(OneTimePasswords.class);
                Marshaller marshaller = context.createMarshaller();
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                marshaller.marshal(oneTimePasswords, writer);
                fileWriter.write(writer.toString());
            } catch (Exception ex) {
                log.error(ex);
            }

        }
    }

    private void AddUser(JbTgUser newJbTgUser) {
        jbTgUsers.add(newJbTgUser);
        jbTgUsersRepository.save(newJbTgUser);
    }

}
