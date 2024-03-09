package ru.jb.tgbotjava8.JBTlsServer.Listeners;

public interface TLSServerListener {
    void OnSslReceive(byte[] message);
    void OnConnected();
    void OnDisconnected();
}
