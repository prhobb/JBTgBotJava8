package ru.jb.tgbotjava8.JBTlsServer;

import lombok.extern.log4j.Log4j2;
import ru.jb.tgbotjava8.JBTlsServer.Listeners.JbSSLSocketValidatorListener;
import ru.jb.tgbotjava8.JBTlsServer.Service.JBTlsServer;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

@Log4j2
public class JbTlsServerSocket implements Runnable, JbSSLSocketValidatorListener {

    final int MAXIMUM_PENDINGSOCKETS=10;
    SSLServerSocket sslServerSocket;
    List<JbSSLSocketValidator> pendingSockets;
    private  JBTlsServer tlsServer;
    Thread thread;

    public JbTlsServerSocket(SSLServerSocket sslServerSocket, JBTlsServer tlsServer){
        super();
        this.sslServerSocket = sslServerSocket;
        this.tlsServer = tlsServer;
        this.pendingSockets=new ArrayList<>(MAXIMUM_PENDINGSOCKETS);
        thread = new Thread(this,"TlsServerSocketListener");
        thread.start();
    }
    public void run() {
        while (sslServerSocket != null) {
            try {
                SSLSocket socket = (SSLSocket)sslServerSocket.accept();
                if(pendingSockets.size()<MAXIMUM_PENDINGSOCKETS) {
                    //ADD pendingSocket
                    JbSSLSocketValidator jbSSLSocketValidator = new JbSSLSocketValidator(socket,this);
                    pendingSockets.add(jbSSLSocketValidator);
                }
                else {
                    log.error("Can't add new pendingSocket. pendingSockets is full. {}:{}",socket.getInetAddress().getAddress(),socket.getPort());
                    socket.close();
                }
            } catch (IOException ex) {
                log.error(ex);
                log.error("Closing  sslServerSocket");
                try {
                    sslServerSocket.close();
                }
                catch (IOException ex2) {
                    log.error(ex2);
                }
                sslServerSocket=null;
            }
        }
    }
    @Override
    public void onValidateResult(boolean validationResult, JbSSLSocketValidator jbSSLSocketValidator) {
        if(validationResult)
            tlsServer.AddClientSocket(jbSSLSocketValidator.getSslSocket());
        else
            log.error("SSL socket not valid. {}:{}",jbSSLSocketValidator.getSslSocket().getInetAddress().getAddress(),jbSSLSocketValidator.getSslSocket().getPort());
        pendingSockets.remove(jbSSLSocketValidator);
    }

}
