package ru.jb.tgbotjava8.JBTlsServer.Listeners;

public interface JbTlsClientSocketListener {
    void OnSslReceive(byte[] message);
    void OnKeepAliveUP();
    void OnKeepAliveDown();
    void OnClose();
}
