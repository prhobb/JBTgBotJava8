This is JBTelegramBot Front part on Java 8. The Back part writed on .Net C# located here: [[<link>](https://github.com/prhobb/JbTlsClient)](https://github.com/prhobb/JbTlsClient)


JBTelegramBot Front consists of two parts:
1. Telegram connector: JBTgBot
2. TLS Server: JBTlsServer

Back part connects to Front via SSL.

What do you have to do by yourself before start:
1. Make application.properties file in \src\main\resources
  tgbot.name=<your tgbot name>
  tgbot.token=<your tgbot token>
  tgbot.otpfile=<One time passwords file name>

  tlsserver.port=<port>
  tlsserver.tlsversion=TLSv1.2
  tlsserver.keystore=<jks Key store with SSLServer certificate>
  tlsserver.keystorepassword=<password for jks Key store with SSLServer certificate>
  tlsserver.truststore=<jks Key store with trusted (client's) certificates>
  tlsserver.truststorepassword=<password jks Key store with trusted (client's) certificates>

  db.driverClassName=org.sqlite.JDBC
  db.url=<DB URL>
  db.username=<DB username>
  db.password=<DB password>
  spring.jpa.database-platform=org.sqlite.hibernate.dialect.SQLiteDialect
  spring.jpa.hibernate.ddl-auto=update

2. Place keystore.jks and truststore.jks in \src\main\resources
3. Set Java8 JDK in IDE

How it works:
1. When JBTgBot receives message it verifies UserID via JbTgAuthentication. Authentificated users locate in DataBase.
   1.1. If there is no UserID in DataBase, JbTgAuthentication compare message.text with otpfile (IneTimePasssword). If OTP exists - user adds to DataBase, if not - message silently discards.
   1.2. If there is UserID in DataBase - message sends to Backend via SSL.
2. When JbTlsClientSocket receives message it sends this message to JBTgBot.
   2.1. If chatID provided message will be sent to chat with this ID.
   2.2. If chatID=0 message will be sent to all known chats.


There can be only one Backend connected to JBTelegramBotFront. If new Backend connects old Backend will be disconnected.

Otpfile and Database will be created while first run.

You can add OTP to Otpfile mannualy or via JbTgAuthentication.AddOtp(String otp) method.

Keepalive for Backend enabled by default. You can mange it with JbTlsClientSocket.setKeepalive(boolean keepalive) method.
   
