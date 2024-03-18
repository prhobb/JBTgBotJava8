package ru.jb.tgbotjava8.JBTlsServer;


import lombok.extern.log4j.Log4j2;
import ru.jb.tgbotjava8.JBTlsServer.Exeptions.StreamShouldBeReset;
import ru.jb.tgbotjava8.JBTlsServer.Listeners.JbTlsClientSocketListener;

import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

@Log4j2
public class JbTlsClientSocket implements Runnable{

    public enum MessageType {
        NONE((byte) 0),
        DATA((byte) 1),
        KEEP_ALIVE((byte) 2),
        ACK((byte) 3);
        private byte type;


        MessageType(byte type) {
            this.type = type;
        }

        public byte getValue() {
            return type;
        }

        public static JbTlsClientSocket.MessageType getTypeOfByte(byte type){
            switch (type){
                case 1:
                    return DATA;
                case 2:
                    return KEEP_ALIVE;
                case 3:
                    return ACK;
                default:
                    return NONE;
            }
        }
    }
    static final int PAYLOAD_SIZE_POSITION=0;
    static final int HASH_POSITION=4;
    static final int MESSAGETYPE_POSITION=36;
    static final int MESSAGENUMBER_POSITION=37;
    static final int PAYLOAD_POSITION=41;
    static final int KEEPALIVE_TIME=30;
    //static final int KEEPALIVE_TIMEOUT=5;
    static final int KEEPALIVE_PROBE=2;

    Socket clientSocket;
    String clientSocketAddressPort;
    InputStream inputStream;
    OutputStream outputStream;
    JbTlsClientSocketListener listener;
    Thread listenThread;
    Thread keepaliveThread;
    boolean started;



    boolean isKeepalive;
    boolean isKeepaliveReceived;
    int keepaliveTries;
    int keepaliveTime;
    //int keepaliveTimeout;
    int keepaliveProbe;



    public boolean isKeepalive() {
        return isKeepalive;
    }

    public void setKeepalive(boolean keepalive) {
        if(keepalive && !isKeepalive && started) {

            keepaliveThread.start();
        }
        isKeepalive = keepalive;
        keepaliveTries=keepaliveProbe;
    }


    public JbTlsClientSocket(Socket clientSocket, JbTlsClientSocketListener listener){
        super();

        this.started=false;
        this.keepaliveTime=KEEPALIVE_TIME;
        //this.keepaliveTimeout=KEEPALIVE_TIMEOUT;
        this.keepaliveProbe=KEEPALIVE_PROBE;
        this.isKeepaliveReceived=false;
        try {
            this.clientSocket=clientSocket;
            this.listener = listener;
            clientSocketAddressPort=clientSocket.getInetAddress().getHostAddress()+":"+clientSocket.getPort();
            this.outputStream = clientSocket.getOutputStream();
            listenThread = new Thread(this, "TlsClientSocketListener");
            listenThread.start();
            keepaliveThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    keepAlive();
                }
            });
            log.info("Connected new client. Client address:{}", clientSocket.getRemoteSocketAddress().toString());
        } catch (IOException e){
            log.error((e));
        }
        catch (Exception ex){
            log.error(ex);
        }
    }

    public void run() {


        this.started = true;
        this.isKeepaliveReceived=false;
        this.keepaliveTries=keepaliveProbe;
        if(isKeepalive)
            keepaliveThread.start();
        int receivedByte;
        try {
            this.inputStream = clientSocket.getInputStream();

            do {
                try {
                    //Read new Packet
                    byte[] sizeBuffer = new byte[4];
                    byte[] hashBuffer = new byte[32];
                    byte[] messagenumberBuffer = new byte[4];
                    byte[] payloadBuffer = new byte[0];
                    int messagenumber=0;
                    MessageType messageType = MessageType.NONE;
                    for(int i=0,paketLength=PAYLOAD_POSITION;i<paketLength;i++){
                        receivedByte = inputStream.read();
                        //FirstByte
                        if(i==0){
                            if (receivedByte < 0) //First 4 bytes - size. This is int and should be positive, so first byte MUST > 0
                                throw new StreamShouldBeReset("FirstByte should be positive");
                            if (inputStream.available() < PAYLOAD_POSITION)
                                throw new StreamShouldBeReset("Stream too short");
                            //sizeBuffer[0] = (byte) receivedByte;
                        }
                        //Read size
                        if(i< PAYLOAD_SIZE_POSITION+ sizeBuffer.length && i>=PAYLOAD_SIZE_POSITION) {
                            sizeBuffer[i-PAYLOAD_SIZE_POSITION] = (byte) receivedByte;
                            if (i==PAYLOAD_SIZE_POSITION+ sizeBuffer.length-1){
                                int size = new BigInteger(sizeBuffer).intValue();
                                if (size < 0) throw new StreamShouldBeReset("Buffer size should be positive");
                                payloadBuffer = new byte[size];
                                paketLength=PAYLOAD_POSITION+size;
                            }
                        }
                        //Read hash
                        else if(i<HASH_POSITION+hashBuffer.length && i>=HASH_POSITION){
                            hashBuffer[i-HASH_POSITION] = (byte) receivedByte;
                        }
                        //Read MessageType
                        else if(i==MESSAGETYPE_POSITION){
                            messageType = MessageType.getTypeOfByte((byte) receivedByte);
                        }
                        //Read messagenumber
                        else if(i<MESSAGENUMBER_POSITION+messagenumberBuffer.length && i>=MESSAGENUMBER_POSITION){
                            messagenumberBuffer[i-MESSAGENUMBER_POSITION] = (byte) receivedByte;
                            if (i==MESSAGENUMBER_POSITION+ sizeBuffer.length-1){
                                messagenumber = new BigInteger(messagenumberBuffer).intValue();
                                if (messagenumber < 0) throw new StreamShouldBeReset("Messagenumber should be positive");
                            }
                        }
                        //Read payload
                        else if(i<PAYLOAD_POSITION+payloadBuffer.length && i>=PAYLOAD_POSITION){
                            payloadBuffer[i-PAYLOAD_POSITION] = (byte) receivedByte;
                        }
                        //Checking after full message received
                        if(i==paketLength-1) {

                            switch (messageType) {
                                case DATA:
                                    //Check received data hash
                                    if (!checkHash(payloadBuffer, hashBuffer))
                                        throw new StreamShouldBeReset("Wrong hash.");
                                    //Run listener if all is fine
                                    if(listener!=null)
                                        listener.OnSslReceive(payloadBuffer);
                                    //Send ACK
                                    Send(null,MessageType.ACK,messagenumber);
                                    break;
                                case KEEP_ALIVE:
                                    isKeepaliveReceived=true;
                                    break;
                                case  ACK:
                                    if(listener!=null)
                                        listener.OnAckReceive(messagenumber);
                                    break;
                            }
                        }
                    }
                    //OLD but saved for possible future use
                    /* OLD Implement
                    receivedByte = inputStream.read();
                    if (receivedByte < 0) //First 4 bytes - size. This is int and should be positive, so first byte MUST > 0
                        throw new StreamShouldBeReset("FirstByte should be positive");
                    if (inputStream.available() < 3 + 32)
                        throw new StreamShouldBeReset("Stream too short");
                    byte[] sizeBuffer = new byte[4];
                    byte[] hashBuffer = new byte[32];
                    //Read size
                    sizeBuffer[0] = (byte) receivedByte;
                    for (int i = 1; i < sizeBuffer.length; i++)
                        sizeBuffer[i] = (byte) inputStream.read();
                    int size = new BigInteger(sizeBuffer).intValue();
                    if (size < 0) throw new StreamShouldBeReset("Buffer size should be positive");
                    byte[] buffer = new byte[size];

                    //Read hash
                    for (int i = 0; i < hashBuffer.length; i++)
                        hashBuffer[i] = (byte) inputStream.read();

                    //Read MessageType
                    //Refactored to for-loop

                    //Read Data
                    for (int i = 0; i < buffer.length; i++)
                        buffer[i] = (byte) inputStream.read();

                    //Check received data hash
                    if (!checkHash(buffer, hashBuffer))
                        throw new StreamShouldBeReset("Wrong hash.");
                    //Run listener if all is fine
                    listener.OnSslReceive(buffer);

                    */
                } catch (IOException e) {
                    log.error("IOException: {1}",e);
                    started=false;
                }
                catch (StreamShouldBeReset exx){
                    log.error(exx);
                    started=false;
                }
                catch (Exception ex) {
                    log.error("Exception: {1}",ex);
                    started=false;
                }
            } while (started);


        } catch (IOException e) {
            log.error("2IOException: {1}",e);
        } catch (Exception ex) {
            log.error("4Exception: {1}",ex);
        }
        Close();
    }

    private void keepAlive() {
        keepaliveTries = -1;
        isKeepaliveReceived=false;
        do {

            if (!isKeepaliveReceived) {
                keepaliveTries--;
                if (keepaliveTries == 0) {
                    if(listener!=null)
                        listener.OnKeepAliveDown();
                }
                else if (keepaliveTries < -1)
                    keepaliveTries = -1;
            }
            else {
                if(keepaliveTries <= 0)
                    if(listener!=null)
                        listener.OnKeepAliveUP();
                keepaliveTries = keepaliveProbe;
            }
            isKeepaliveReceived=false;
            Send(null,MessageType.KEEP_ALIVE,0);
            try {
                Thread.sleep(keepaliveTime*1000);
            } catch (InterruptedException e) {
                log.error("KeepAlive thread interrupted. ",e);
                isKeepalive=false;
            }
        } while (isKeepalive);
    }
    private boolean checkHash(byte[] message, byte[] hash) {
        //Correct Check
        if (message == null || hash == null || message.length == 0 || hash.length != 32) {
            log.error("Hash function arguments are incorrect");
            return false;
        }
        return Arrays.equals(getHash256(message), hash);
    }

    public static byte[] getHash256(byte[] message)
    {
        if(message==null) {
            log.error("Hash function arguments are incorrect");
            return new byte[0];
        }
        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] res = messageDigest.digest(message);
            return res;
        } catch (NoSuchAlgorithmException e) {
            log.error("NoSuchAlgorithmException: {1}",e);
        }
        return new byte[0];
    }

    private boolean resetStream(InputStream inputStream) throws IOException {
        if(inputStream!=null && inputStream.available()>0) {
            while (inputStream.available()>0 && inputStream.read() != -1) {
            }
            return true;
        }
        return false;
    }

    public boolean isClosed(){
        if (clientSocket==null)
            return true;
        return clientSocket.isClosed();
    }

    public String Send(byte[] message , MessageType messageType, int messageNumber){
        synchronized (this) {
            if (clientSocket != null && !clientSocket.isClosed()) {
                byte[] buffer = GetSendBuffer(message, messageType, messageNumber);
                if (buffer != null) {
                    try {
                        outputStream.write(buffer);
                        outputStream.flush();
                    } catch (IOException ex) {
                        log.error("Error while sending message via SSL. ", ex);
                        Close();
                    }
                } else
                    log.error("Can't send null buffer");
            } else
                return "Socket is closed";
            return "";
        }
    }

    private static byte[] GetSendBuffer(byte[] message,MessageType messageType,int messagenumber) {
        //Структура. 0-byte--3byte: Payload size, 4-byte--35byte: Hash, 36byte: MessageType, 37-byte--40byte:messagenumber 41-byte--end: Payload
        if (message == null && messageType!=MessageType.DATA)
            message = new byte[1];
        //Считаем хэш сообщения
        byte[] hash = getHash256(message);
        if (hash != null) {

            //Записываем размер сообщения
            byte[] result = new byte[message.length + PAYLOAD_POSITION];
            log.debug("Send data Length: {}", message.length);
            result[0] = (byte) (message.length >> 24);
            result[1] = (byte) (message.length >> 16);
            result[2] = (byte) (message.length >> 8);
            result[3] = (byte) message.length;

            //записываем хэш для проверки на сервере
            System.arraycopy(hash, 0, result, HASH_POSITION, hash.length);

            //Write MessageType
            result[MESSAGETYPE_POSITION]= messageType.getValue();

            //Write messagenumber
            result[MESSAGENUMBER_POSITION] = (byte) (messagenumber >> 24);
            result[MESSAGENUMBER_POSITION+1] = (byte) (messagenumber >> 16);
            result[MESSAGENUMBER_POSITION+2] = (byte) (messagenumber >> 8);
            result[MESSAGENUMBER_POSITION+3] = (byte) messagenumber;

            //Копируем данные в result
            System.arraycopy(message, 0, result, PAYLOAD_POSITION, message.length);

            return result;
        }

        return null;
    }

    public void Close() {
        listener.OnClose();
        isKeepalive=false;
        log.error("Closing client socket: "+clientSocketAddressPort );
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                outputStream.close();
                inputStream.close();
                clientSocket.close();
                log.error("ClientSocket closed");
            }
            else
                log.error("ClientSocket already closed");
        }
        catch (IOException ex) {
            log.error(ex);
        }
    }

}
