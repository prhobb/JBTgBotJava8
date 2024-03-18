package ru.jb.tgbotjava8.JBTlsServer.Listeners;

public interface JbTlsClientSocketListener {
    void OnSslReceive(byte[] message);
    void OnAckReceive(int messagenumber);
    void OnKeepAliveUP();
    void OnKeepAliveDown();
    void OnClose();
}
