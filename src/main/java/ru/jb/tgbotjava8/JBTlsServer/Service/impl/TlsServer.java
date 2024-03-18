package ru.jb.tgbotjava8.JBTlsServer.Service.impl;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.jb.tgbotjava8.JBTlsServer.JbTlsServerPendingMessage;
import ru.jb.tgbotjava8.JBTlsServer.Service.JBTlsServer;
import ru.jb.tgbotjava8.JBTlsServer.JbTlsClientSocket;
import ru.jb.tgbotjava8.JBTlsServer.JbTlsServerSocket;
import ru.jb.tgbotjava8.JBTlsServer.Listeners.JbTlsClientSocketListener;
import ru.jb.tgbotjava8.JBTlsServer.Listeners.TLSServerListener;
import ru.jb.tgbotjava8.config.TlsServerConfig;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
@Log4j2
public class TlsServer implements JBTlsServer , JbTlsClientSocketListener {

    final int MAX_PENDINGMESSAGE_AGE_SEC =180;
    private List<TLSServerListener> listeners;
    JbTlsServerSocket jbTlsServerSocket;
    JbTlsClientSocket jbTlsClientSocket;
    SSLServerSocket serverSocket;
    private Map<Integer, JbTlsServerPendingMessage> pendingMessages;
    private int messageNumber;




    @Autowired
    public void TlsServer(TlsServerConfig config)
            throws Exception {

        Objects.requireNonNull(config.getTlsVersion(), "TLS version is mandatory");

        if (config.getPort() <= 0 || config.getPort()>65535) {
            throw new IllegalArgumentException(
                    "Port number must be >0 and <65535");
        }

        listeners = new ArrayList<TLSServerListener>();
        this.pendingMessages = new HashMap<>();
        messageNumber=0;


        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream tstore =  new FileInputStream( config.getTrustStore());
        trustStore.load(tstore, config.getTrustStorePassword());
        tstore.close();
        TrustManagerFactory tmf = TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream kstore = new FileInputStream( config.getKeyStore());
        keyStore.load(kstore, config.getKeyStorePassword());
        kstore.close();

        KeyManagerFactory kmf = KeyManagerFactory
                .getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, config.getKeyStorePassword());

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(),
                SecureRandom.getInstanceStrong());


        SSLServerSocketFactory factory = ctx.getServerSocketFactory();
        try {
            serverSocket = (SSLServerSocket) factory.createServerSocket(config.getPort(), config.getBacklog());
            //sslListener = (SSLServerSocket) serverSocket;

            serverSocket.setNeedClientAuth(true);
            serverSocket.setEnabledProtocols(new String[]{config.getTlsVersion()});

            //Start listening
            jbTlsServerSocket = new JbTlsServerSocket(serverSocket, this);
        }
        catch (Exception e){
            log.error(e);
        }



    }



    /**
     * Sends ByteArray to SSlClient.
     */
    @Override
    public String Send(byte[] message) {
        if (jbTlsClientSocket!=null && !jbTlsClientSocket.isClosed()) {
            if(messageNumber>214748364)
                messageNumber=0;
            else
                messageNumber++;
            pendingMessages.put(messageNumber,
                    new JbTlsServerPendingMessage(messageNumber, LocalDateTime.now(),message));
            return jbTlsClientSocket.Send(message, JbTlsClientSocket.MessageType.DATA,messageNumber);
        }
        return "tlsClientSocket not connected";
    }

    @Override
    public void AddListener(TLSServerListener listener) {
        listeners.add(listener);
    }

    /**
     * Adds new clientSocket. Disconnect previous clientSocket if there is another connected clientSocket
     * @param clientSocket
     */
    @Override
    public void AddClientSocket(Socket clientSocket) {
        log.warn("New socket connection from IP: {}", clientSocket.getInetAddress());
        if (jbTlsClientSocket != null && !jbTlsClientSocket.isClosed()) {
            log.warn("Closing old socket");
            jbTlsClientSocket.Close();
        }
        jbTlsClientSocket = new JbTlsClientSocket(clientSocket, this);
        jbTlsClientSocket.setKeepalive(true);
        //Resend Pending Messages
        List<Integer> messagesToRemove = new ArrayList<>(pendingMessages.size());
        pendingMessages.forEach((number, message) -> {
            if (LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) - message.getDate().toEpochSecond(ZoneOffset.UTC)
                    < MAX_PENDINGMESSAGE_AGE_SEC) {
                log.info("Resend message. ID: {} Date:{}", number,message.getDate());
                jbTlsClientSocket.Send(message.getBuffer(), JbTlsClientSocket.MessageType.DATA, number);
            }
            else {
                //pendingMessages.remove(number);
                messagesToRemove.add(number);
                log.info("Removed message. ID: {} Date:{}", number, message.getDate());
            }
        });
        messagesToRemove.forEach(number -> pendingMessages.remove(number));
        //---

        listeners.forEach(listener -> {
            listener.OnConnected();
        });
    }

    @Override
    public boolean isConnected() {
        if(jbTlsClientSocket==null) return false;
        return !jbTlsClientSocket.isClosed();
    }


    @Override
    public void OnSslReceive(byte[] message) {
        listeners.forEach(listener -> {
            listener.OnSslReceive(message);
        });
    }

    @Override
    public void OnAckReceive(int messagenumber) {
        pendingMessages.remove(messagenumber);
    }

    @Override
    public void OnKeepAliveUP() {

    }

    @Override
    public void OnKeepAliveDown() {
        log.error("KeepAliveDown");
        jbTlsClientSocket.Close();
    }

    @Override
    public void OnClose() {

        listeners.forEach(listener -> {
            listener.OnDisconnected();
        });
    }
}
