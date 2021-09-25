import java.io.File;

public class TestApp {
    public static void main(String[] args) {
        File file = new File("client_repository/1.txt");
        String filename = file.getName();
        System.out.println(filename);
    }
}
