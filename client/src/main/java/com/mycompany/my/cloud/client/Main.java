package com.mycompany.my.cloud.client;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/main.fxml"));
        Parent root = fxmlLoader.load();
        Controller controller = fxmlLoader.getController();
        primaryStage.setTitle("My cloud");
        primaryStage.setScene(new Scene(root, 1024, 600));
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                controller.exit();
            }
        });
        primaryStage.show();
    }

    public static void main(String[] args) {
        //  System.out.println(System.getenv().get("USERNAME")); // getenv - получаем переменные среды (мапа)
        launch(args); // начиная с launch  все оборачивается в поток JavaFX
    }
}
