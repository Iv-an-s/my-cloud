package com.mycompany.my.cloud.client;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class Controller implements Initializable {// Интерфейс дает возможность проводить подготовительные действия,
    // проводить преднастройку контроллера
    @FXML
    TextArea msgArea;
    @FXML
    ListView<String> clientsList;
    @FXML
    TextField msgField, loginField;
    @FXML
    PasswordField passwordField;
    @FXML
    VBox rootElement, loginPanel, clientPanel, serverPanel;
    @FXML
    HBox tablesPanel, buttonPanel;
    @FXML
    MenuBar menuPanel;


    private Network network;
    private String username;

    public void setUsername(String username) {
        this.username = username;
        boolean usernameIsNull = username == null;
        loginPanel.setVisible(usernameIsNull);
        loginPanel.setManaged(usernameIsNull);
        menuPanel.setVisible(!usernameIsNull);
        menuPanel.setManaged(!usernameIsNull);
        tablesPanel.setVisible(!usernameIsNull);
        tablesPanel.setManaged(!usernameIsNull);
        buttonPanel.setVisible(!usernameIsNull);
        buttonPanel.setManaged(!usernameIsNull);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setUsername(null);
        network = new Network();

        network.setOnAuthFailedCallback(new Callback() {
            @Override
            public void callback(Object... args) {
                String cause = (String) args[0];
                //msgArea.appendText(cause + "\n");
                updateUI(() -> showErrorAlert(cause));
                loginField.clear();
                passwordField.clear();
            }
        });

        network.setOnAuthOkCallback(new Callback() {
            @Override
            public void callback(Object... args) {
                String msg = (String) args[0];
                setUsername(msg.split("\\s")[1]);
                System.out.println("setOnAuthOkCallback уставливает username: " + msg.split("\\s")[1]);
                //msgArea.clear();
            }
        });

        network.setOnMessageReceivedCallback(new Callback() {
            @Override
            public void callback(Object... args) {
                String msg = (String) args[0];
                if (msg.startsWith("/")) {
                    if (msg.startsWith("/clients_list ")) {
                        // /clients_list Bob Max Jack
                        String[] tokens = msg.split("\\s");
                        Platform.runLater(() -> { //передаем задачу в поток JavaFX. Если пытаться это делать из текущего треда напрямую - будут ошибки
                            // В поток JavaFX из других потоков не лезем. Предаем задачи через Platform
                            clientsList.getItems().clear(); // getItems - запрос списка элементов, которые есть у view
                            for (int i = 1; i < tokens.length; i++) {
                                clientsList.getItems().add(tokens[i]);
                            }
                        });
                    }
                    return;
                }
                msgArea.appendText(msg + "\n");
            }
        });

        network.setOnDisconnectCallback(new Callback() {
            @Override
            public void callback(Object... args) {
                // сбрасываем имя пользователя, и забываем про его историю
                setUsername(null);
            }
        });
    }





    public void refreshLocalFileList(){

    }

    public void refreshServerFileList(){

    }



    public void login() {
        if (loginField.getText().isEmpty()) {
            showErrorAlert("Имя пользователя не может быть пустым");
            return;
        }
        if (!network.isConnected()) {
            try {
                network.connect(8189);
            } catch (IOException e) {
                showErrorAlert("Невозможно подключиться к серверу на порт: " + 8189);
                return;
            }
        }
        try {
            String login = loginField.getText();
            String password = passwordField.getText();
            System.out.println("Controller: login =" + login + ", password =" + password);
            network.tryToLogin(login, password);
        } catch (IOException e) {
            showErrorAlert("Невозможно отправить данные пользователя");
            return;
        }
    }

    public void sendMsg() {
        try {
            network.sendMessage(msgField.getText());
            msgField.clear();
            msgField.requestFocus(); // после предыдущего действия запрашиваем фокус в поле msgField
        } catch (IOException e) {
            showErrorAlert("Невозможно отправить сообщение");
        }
    }

    public void exit(){
        network.disconnect();
    }

    private static void updateUI(Runnable r){
        if (Platform.isFxApplicationThread()){
            r.run();
        }else {
            Platform.runLater(r);
        }
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.setTitle("My Cloud");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    public void btnExitAction(ActionEvent actionEvent) {
        Platform.exit();
        network.disconnect();
    }

    public void btnDownloadAction(ActionEvent actionEvent) {
        ClientPanelController clientPanelController = (ClientPanelController) clientPanel.getProperties().get("ctrl");
        ServerPanelController serverPanelController = (ServerPanelController) serverPanel.getProperties().get("ctrl");

        if(serverPanelController.getSelectedFilename() == null){
            Alert alert = new Alert(Alert.AlertType.WARNING,"Не выбран файл для скачивания", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        // todo логика метода
    }

    public void btnUploadAction(ActionEvent actionEvent) {
        ClientPanelController clientPanelController = (ClientPanelController) clientPanel.getProperties().get("ctrl");
        ServerPanelController serverPanelController = (ServerPanelController) serverPanel.getProperties().get("ctrl");

        if(clientPanelController.getSelectedFilename() == null){
            Alert alert = new Alert(Alert.AlertType.WARNING,"Не выбран файл для загрузки в удаленный репозиторий", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        // todo логика метода
    }
}
