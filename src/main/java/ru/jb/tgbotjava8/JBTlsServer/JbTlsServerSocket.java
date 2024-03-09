package ru.jb.tgbotjava8.JBTlsServer;

import lombok.extern.log4j.Log4j2;
import ru.jb.tgbotjava8.JBTlsServer.Service.JBTlsServer;

import javax.net.ssl.SSLServerSocket;
import java.io.IOException;
import java.net.Socket;

@Log4j2
public class JbTlsServerSocket implements Runnable{

    SSLServerSocket sslServerSocket;
    private  JBTlsServer tlsServer;
    Thread thread;

    public JbTlsServerSocket(SSLServerSocket sslServerSocket, JBTlsServer tlsServer){
        super();
        this.sslServerSocket = sslServerSocket;
        this.tlsServer = tlsServer;
        thread = new Thread(this,"TlsServerSocketListener");
        thread.start();
    }
    public void run() {
        while (sslServerSocket != null) {
                try {
                    Socket socket = sslServerSocket.accept();
                    tlsServer.AddClientSocket(socket);
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


}
