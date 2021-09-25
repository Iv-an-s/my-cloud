import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class MainApp {
    public static void main(String[] args) {

        try (ServerSocket serverSocket = new ServerSocket(8189)) {
            System.out.println("Ожидаем клиента...");
            try (Socket socket = serverSocket.accept();
                 ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream())) {
                System.out.println("Клиент подключился...");
                CommonMessage commonMessage = (CommonMessage) inputStream.readObject();
                System.out.println(commonMessage.getText());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
