package com.mycompany.my.cloud.client;

import sun.security.util.ArrayUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Network {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private Callback onAuthOkCallback;
    private Callback onAuthFailedCallback;
    private Callback onMessageReceivedCallback;
    private Callback onConnectCallback;
    private Callback onDisconnectCallback;


    public void setOnAuthOkCallback(Callback onAuthOkCallback) {
        this.onAuthOkCallback = onAuthOkCallback;
    }

    public void setOnAuthFailedCallback(Callback onAuthFailedCallback) {
        this.onAuthFailedCallback = onAuthFailedCallback;
    }

    public void setOnMessageReceivedCallback(Callback onMessageReceivedCallback) {
        this.onMessageReceivedCallback = onMessageReceivedCallback;
    }

    public void setOnConnectCallback(Callback onConnectCallback) {
        this.onConnectCallback = onConnectCallback;
    }

    public void setOnDisconnectCallback(Callback onDisconnectCallback) {
        this.onDisconnectCallback = onDisconnectCallback;
    }

    public boolean isConnected(){
        return socket != null && !socket.isClosed();
    }

    public void connect(int port) throws IOException {
            socket = new Socket("localhost", port);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            if(onConnectCallback != null) {
                onConnectCallback.callback();
            }
            Thread t = new Thread(() -> {
                try {
                    // Цикл авторизации
                    while (true) {
                        System.out.println("Ждем подтверждения авторизации");
                        String msg = readMessage();
                        System.out.println("Цикл авторизации: прочитали [" + msg + "]");
                        if(msg.startsWith("/login_ok ")){
                            if (onAuthOkCallback != null) {
                                onAuthOkCallback.callback(msg);
                            }
                            break;
                        }
                        if(msg.startsWith("/login_failed ")){
                            String cause = msg.split("\\s", 2)[1];
                            if (onAuthFailedCallback != null) {
                                onAuthFailedCallback.callback(cause);
                            }
                        }
                    }
                    // Цикл общения
                    while (true){
                        System.out.println("Перешли в цикл общения...");
                        String msg = in.readUTF();
                        if (onMessageReceivedCallback != null){
                            onMessageReceivedCallback.callback(msg);
                        }
                    }
                }catch (IOException e){
                    e.printStackTrace();
                }finally {
                    disconnect();
                }
            });
            t.start();
        }

    public void sendMessage(String message) throws IOException {
        out.writeUTF(message);
    }

    public String readMessage(){
        try {
            int messageSize = in.readInt();
            System.out.println("Network: messageSize = " + messageSize);
            byte[]messageBytes = new byte[messageSize];
            in.read(messageBytes);
            return new String(messageBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "incorrect message";
    }

    public void tryToLogin(String login, String password) throws IOException {
        // протокол для авторизации:
        // 5 байт (/auth) -> 4 байта(int - длина логина) -> n байт (логин) -> 4 байта (int - длина пароля) -> n байт (пароль)
        int messageLenth = 5 + 4 + login.length() + 4 + password.length();
        int tempDstPos = 0;

        byte [] result = new byte[messageLenth];
                System.arraycopy("/auth".getBytes(), 0, result, tempDstPos, 5);
                tempDstPos += 5;
                System.arraycopy(ByteBuffer.allocate(4).putInt(login.length()).array(), 0, result, tempDstPos, 4);
                tempDstPos += 4;
                System.arraycopy(login.getBytes(), 0, result, tempDstPos, login.getBytes().length);
                tempDstPos += login.getBytes().length;
                System.arraycopy(ByteBuffer.allocate(4).putInt(password.length()).array(), 0, result, tempDstPos, 4);
                tempDstPos += 4;
                System.arraycopy(password.getBytes(), 0, result, tempDstPos, password.length());

        out.write(result);

//        out.write("/auth".getBytes());
//        out.writeInt(login.length());
//        out.write(login.getBytes());
//        out.writeInt(password.length());
//        out.write(password.getBytes());
        System.out.println("Отправлены данные для авторизации");
    }

    public void disconnect(){
        if (onDisconnectCallback != null){
            onDisconnectCallback.callback();
        }
        try {
            if(in != null) {
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if(out != null) {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if(socket != null){
                socket.close();
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
