import java.io.*;
import java.net.Socket;

public class Main {

    public static void main(String[] args){
        try (Socket socket = new Socket("localhost", 8189);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())){

            String filename = "1.txt";
            // получение файла

            doAuth(out);
            getFile(socket, out, filename);
            //sendFile(out);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


//        CommonMessage commonMessage = new CommonMessage("Hello from client");
//
//        try (Socket socket = new Socket("localhost", 8189);
//             ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream())) {
//            Thread.sleep(5000);
//            outputStream.writeObject(commonMessage);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

    private static void getFile (Socket socket, DataOutputStream out, String filename){
        try (DataInputStream inputStream = new DataInputStream((socket.getInputStream()));
             OutputStream fileOutputStream = new BufferedOutputStream(new FileOutputStream(new File("client_repository", "_" + filename)))) {
            out.write(16);
            out.write(17);
            int filenameLength = filename.length();
            out.writeInt(filenameLength);
            byte[] filenameBytes = filename.getBytes();
            out.write(filenameBytes);
            System.out.println("Передали сигнальный байт, длину имени файла, имя файла");

            long fileLength = inputStream.readLong();
            //long fileLength = 539;
            long count = 0;
            System.out.println("Получили длину файла = " + fileLength);

            int i = 0;
            while ((i = inputStream.available()) != -1) {
                fileOutputStream.write(inputStream.read());
                count++;
                if (count == fileLength){
                    fileOutputStream.flush();
                    break;
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private static void sendFile(DataOutputStream out) throws IOException {
        InputStream in;
        out.write(15);
        File file = new File("client_repository/1.txt");
        String filename = file.getName();
        byte[] filenameBytes = filename.getBytes();
        out.writeInt(filenameBytes.length);
        out.write(filenameBytes);
        long fileLength = file.length();
        out.writeLong(fileLength);
        System.out.println("Отправлены параметры файла");

        in = new BufferedInputStream(new FileInputStream(file));
        int b;
        System.out.println("Готовимся отправить файл");
        while ((b = in.read()) != -1) {
            out.write(b);
        }
        System.out.println("Файл отправлен");
        in.close();
    }

    private static void doAuth(DataOutputStream out) throws IOException {
        String authCommand = new String("/auth");
        byte[] authCommandBytes = authCommand.getBytes();
        out.write(authCommandBytes);
        String login = "bob@google.com";
        String password = "123";
        int loginLength = login.length();
        int passwordLength = password.length();
        out.writeInt(loginLength);
        out.write(login.getBytes());
        out.writeInt(passwordLength);
        out.write(password.getBytes());
        System.out.println("Отправлены данные для авторизации");
    }
}
