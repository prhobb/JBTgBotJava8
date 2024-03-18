package ru.jb.tgbotjava8.JBTlsServer.Service;


import ru.jb.tgbotjava8.JBTlsServer.Listeners.TLSServerListener;

import java.net.Socket;

public interface JBTlsServer {
    public String Send(byte[] message);
    public void AddListener(TLSServerListener listener);
    public void AddClientSocket(Socket clientSocket);
    public boolean isConnected();

}
