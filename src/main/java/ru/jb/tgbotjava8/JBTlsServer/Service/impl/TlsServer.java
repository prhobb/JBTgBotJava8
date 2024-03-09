package ru.jb.tgbotjava8.JBTlsServer.Service.impl;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.jb.tgbotjava8.JBTlsServer.Service.JBTlsServer;
import ru.jb.tgbotjava8.JBTlsServer.JbTlsClientSocket;
import ru.jb.tgbotjava8.JBTlsServer.JbTlsServerSocket;
import ru.jb.tgbotjava8.JBTlsServer.Listeners.JbTlsClientSocketListener;
import ru.jb.tgbotjava8.JBTlsServer.Listeners.TLSServerListener;
import ru.jb.tgbotjava8.config.TlsServerConfig;

import javax.net.ssl.*;
import java.io.InputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Log4j2
public class TlsServer implements JBTlsServer , JbTlsClientSocketListener {

    private List<TLSServerListener> listeners;
    JbTlsServerSocket jbTlsServerSocket;
    JbTlsClientSocket jbTlsClientSocket;
    SSLServerSocket serverSocket;

    @Autowired
    public void TlsServer(TlsServerConfig config)
            throws Exception {

        Objects.requireNonNull(config.getTlsVersion(), "TLS version is mandatory");

        if (config.getPort() <= 0 || config.getPort()>65535) {
            throw new IllegalArgumentException(
                    "Port number must be >0 and <65535");
        }

        listeners = new ArrayList<TLSServerListener>();

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream tstore = TlsServer.class
                .getResourceAsStream("/" + config.getTrustStore());
        trustStore.load(tstore, config.getTrustStorePassword());
        tstore.close();
        TrustManagerFactory tmf = TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream kstore = TlsServer.class
                .getResourceAsStream("/" + config.getKeyStore());
        keyStore.load(kstore, config.getKeyStorePassword());

        KeyManagerFactory kmf = KeyManagerFactory
                .getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, config.getKeyStorePassword());
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(),
                SecureRandom.getInstanceStrong());

        SSLServerSocketFactory factory = ctx.getServerSocketFactory();
        try  {
            serverSocket = (SSLServerSocket)factory.createServerSocket(config.getPort(),config.getBacklog());
             //sslListener = (SSLServerSocket) serverSocket;

            serverSocket.setNeedClientAuth(true);
            serverSocket.setEnabledProtocols(new String[] {config.getTlsVersion()});

            //Start listening
            jbTlsServerSocket = new JbTlsServerSocket(serverSocket,this);
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
        if (jbTlsClientSocket!=null)
            return jbTlsClientSocket.Send(message, JbTlsClientSocket.MessageType.DATA);
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
        log.warn("New socket connection from IP: {1}", clientSocket.getInetAddress());
        if (jbTlsClientSocket != null && !jbTlsClientSocket.isClosed()) {
            log.warn("Closing old socket");
            jbTlsClientSocket.Close();
        }
        jbTlsClientSocket = new JbTlsClientSocket(clientSocket, this);
        jbTlsClientSocket.setKeepalive(true);
        listeners.forEach(listener -> {
            listener.OnConnected();
        });
    }


    @Override
    public void OnSslReceive(byte[] message) {
        listeners.forEach(listener -> {
            listener.OnSslReceive(message);
        });
    }

    @Override
    public void OnKeepAliveUP() {

    }

    @Override
    public void OnKeepAliveDown() {
        jbTlsClientSocket.Close();

    }

    @Override
    public void OnClose() {
        listeners.forEach(listener -> {
            listener.OnDisconnected();
        });
    }
}
