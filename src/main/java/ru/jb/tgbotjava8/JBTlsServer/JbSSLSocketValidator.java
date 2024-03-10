package ru.jb.tgbotjava8.JBTlsServer;

import lombok.extern.log4j.Log4j2;
import ru.jb.tgbotjava8.JBTlsServer.Listeners.JbSSLSocketValidatorListener;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.net.SocketException;

@Log4j2
public class JbSSLSocketValidator implements Runnable {

    final int TIMEOUT = 5000;

    public SSLSocket getSslSocket() {
        return sslSocket;
    }

    private SSLSocket sslSocket;
    private JbSSLSocketValidatorListener caller;
    Thread thread;

    public JbSSLSocketValidator(SSLSocket sslSocket, JbSSLSocketValidatorListener caller) {
        this.sslSocket = sslSocket;
        this.caller = caller;
        thread = new Thread(this, "JbSSLSocketValidator");
        thread.start();
    }

    @Override
    public void run() {
        try {
            sslSocket.setSoTimeout(TIMEOUT);
            SSLSession sslSession = sslSocket.getSession();
            sslSocket.setSoTimeout(0);
            caller.onValidateResult(sslSession.isValid(), this);
        } catch (SocketException e) {
            caller.onValidateResult(false, this);
        }
    }
}
