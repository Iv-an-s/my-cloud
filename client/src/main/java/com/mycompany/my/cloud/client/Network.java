package com.mycompany.my.cloud.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.nio.ByteBuffer;


public class Network {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String fileList;

    private Callback onAuthOkCallback;
    private Callback onAuthFailedCallback;
    private Callback onHandleCommandCallback;
    private Callback onConnectCallback;
    private Callback onDisconnectCallback;
    private Callback onUpdateServerFileList;


    public void setOnAuthOkCallback(Callback onAuthOkCallback) {
        this.onAuthOkCallback = onAuthOkCallback;
    }

    public void setOnAuthFailedCallback(Callback onAuthFailedCallback) {
        this.onAuthFailedCallback = onAuthFailedCallback;
    }

    public void setOnHandleCommandCallback(Callback onHandleCommandCallback) {
        this.onHandleCommandCallback = onHandleCommandCallback;
    }

    public void setOnConnectCallback(Callback onConnectCallback) {
        this.onConnectCallback = onConnectCallback;
    }

    public void setOnDisconnectCallback(Callback onDisconnectCallback) {
        this.onDisconnectCallback = onDisconnectCallback;
    }

    public void setOnUpdateServerFileList(Callback onUpdateServerFileList){
        this.onUpdateServerFileList = onUpdateServerFileList;
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
                    askFileList();
                    if(onUpdateServerFileList!= null){
                        onUpdateServerFileList.callback(fileList);
                    }
                    while (true){
                        System.out.println("Перешли в цикл общения...");
                        String msg = readMessage();
                        /**
                         * Запрашиваем список файлов
                         * получаем список от сервера, присваиваем переменной содержимое списка (String msg)
                         * onUpdateFileListCallback.callback(msg)
                         * если лист обновился - запускаем обработчика команд, который ждем команды (от контроллера)
                         * onHandlerCommandCallback.callback()
                         * который через switch обрабатывает команды от кнопок, отправляя запросы и получая результат
                         *
                          */
                        if (onHandleCommandCallback != null){
                            onHandleCommandCallback.callback();
                        }

//                                if(onGetFileListCallback != null){
//                                    onGetFileListCallback.callback();
//                                }
                        }
//                        if (onMessageReceivedCallback != null){
//                            onMessageReceivedCallback.callback(msg);
//                        }

//                }catch (IOException e){
//                    e.printStackTrace();
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

    public void askFileList(){
//        протокол для запроса списка файлов - отправка сигнальных байтов 16 и 15
//      в ответ получаем Длину строки (1 int) + строку (String)
        try {
            out.write(16);
            out.write(15);
            int fileListSize = in.readInt();
            System.out.println("Network: fileListSize is " + fileListSize + " bytes");
            byte [] fileListBytes = new byte[fileListSize];
            in.read(fileListBytes);
            fileList = new String(fileListBytes);
            System.out.println(fileList);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
