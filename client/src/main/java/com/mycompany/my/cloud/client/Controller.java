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
    TextField msgField, loginField, pathField;
    @FXML
    PasswordField passwordField;
    @FXML
    VBox rootElement, loginPanel;
    @FXML
    HBox tables, aboveTables, buttonPanel;
    @FXML
    MenuBar menuPanel;
    @FXML
    TableView<FileInfo> clientTable, serverTable;
    @FXML
    ComboBox<String> disksBox;






    private Network network;
    private String username;

    public void setUsername(String username) {
        this.username = username;
        boolean usernameIsNull = username == null;
        loginPanel.setVisible(usernameIsNull);
        loginPanel.setManaged(usernameIsNull);
//        workPanel.setVisible(!usernameIsNull);
//        workPanel.setManaged(!usernameIsNull);
        menuPanel.setVisible(!usernameIsNull);
        menuPanel.setManaged(!usernameIsNull);
        tables.setVisible(!usernameIsNull);
        tables.setManaged(!usernameIsNull);
        aboveTables.setVisible(!usernameIsNull);
        aboveTables.setManaged(!usernameIsNull);
        clientTable.setVisible(!usernameIsNull);
        clientTable.setManaged(!usernameIsNull);
        serverTable.setVisible(!usernameIsNull);
        serverTable.setManaged(!usernameIsNull);
        buttonPanel.setVisible(!usernameIsNull);
        buttonPanel.setManaged(!usernameIsNull);
//        disksBox.setVisible(!usernameIsNull);
//        disksBox.setManaged(!usernameIsNull);


        //clientsList.setVisible(!usernameIsNull);
        //clientsList.setManaged(!usernameIsNull);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setUsername(null);
        network = new Network();

        // НАСТРАИВАЕМ КЛИЕТСКУЮ ТАБЛИЦУ
        // Когда создаем столбец, то указываем, что должно храниться с таблице, и как это в ней должно выглядеть
        // Информация о файле у нас храниться в FileInfo и мы это хотим преобразовать в строку:
        TableColumn<FileInfo, String> clientFileTypeColumn = new TableColumn<>();
        clientFileTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().getName()));
        clientFileTypeColumn.setPrefWidth(24); // todo прорезинить столбцы

        TableColumn<FileInfo, String> clientFilenameColumn = new TableColumn<>("Имя");
        clientFilenameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFilename()));
        clientFilenameColumn.setPrefWidth(240);

        TableColumn<FileInfo, Long> clientFileSizeColumn = new TableColumn<>("Размер");
        clientFileSizeColumn.setCellValueFactory(param -> new SimpleObjectProperty(param.getValue().getSize()));
        clientFileSizeColumn.setCellFactory(column -> {
            return new TableCell<FileInfo, Long>(){
                @Override
                protected void updateItem(Long item, boolean empty) {
                    super.updateItem(item, empty);
                    if(item == null || empty){
                        setText(null);
                        setStyle("");
                    }else {
                        String text = String.format("%,d bytes", item); // разделение порядков (по 3 цифры) через пробел
                        if(item == -1L){
                            text = "[DIR]";
                        }
                        setText(text);
                    }
                }
            };
        });
        clientFileSizeColumn.setPrefWidth(120);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        TableColumn<FileInfo, String> clientFileDateColumn = new TableColumn<>("Дата изменения");
        clientFileDateColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastModified().format(dtf)));
        clientFileDateColumn.setPrefWidth(120);

        //table.getColumns().add(clientFileTypeColumn);
        clientTable.getColumns().addAll(clientFileTypeColumn, clientFilenameColumn, clientFileSizeColumn, clientFileDateColumn);
        clientTable.getSortOrder().add(clientFileTypeColumn); // задали столбец, по которому будем по умолчанию сортироваться

        //получаем список дисков:
        disksBox.getItems().clear();
        for (Path p : FileSystems.getDefault().getRootDirectories()){
            disksBox.getItems().add(p.toString());
        }
        disksBox.getSelectionModel().select(0); // по умолчанию выбираем первый из них

        updateClientList(Paths.get("."));

        // НАСТРАИВАЕМ СЕРВЕРНУЮ ТАБЛИЦУ
        TableColumn<FileInfo, String> serverFileTypeColumn = new TableColumn<>();
        serverFileTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().getName()));
        serverFileTypeColumn.setPrefWidth(24);

        TableColumn<FileInfo, String> serverFilenameColumn = new TableColumn<>("Имя");
        serverFilenameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFilename()));
        serverFilenameColumn.setPrefWidth(240);

        TableColumn<FileInfo, Long> serverFileSizeColumn = new TableColumn<>("Размер");
        serverFileSizeColumn.setCellValueFactory(param -> new SimpleObjectProperty(param.getValue().getSize()));
        serverFileSizeColumn.setCellFactory(column -> {
            return new TableCell<FileInfo, Long>(){
                @Override
                protected void updateItem(Long item, boolean empty) {
                    super.updateItem(item, empty);
                    if(item == null || empty){
                        setText(null);
                        setStyle("");
                    }else {
                        String text = String.format("%,d bytes", item); // разделение порядков (по 3 цифры) через пробел
                        if(item == -1L){
                            text = "[DIR]";
                        }
                        setText(text);
                    }
                }
            };
        });
        serverFileSizeColumn.setPrefWidth(120);

        TableColumn<FileInfo, String> serverFileDateColumn = new TableColumn<>("Дата изменения");
        serverFileDateColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastModified().format(dtf)));
        serverFileDateColumn.setPrefWidth(120);

        //table.getColumns().add(clientFileTypeColumn);
        serverTable.getColumns().addAll(serverFileTypeColumn, serverFilenameColumn, serverFileSizeColumn, serverFileDateColumn);
        serverTable.getSortOrder().add(clientFileTypeColumn); // задали столбец, по которому будем по умолчанию сортироваться

        updateServerList(Paths.get(".")); // todo создать метод updateServerList()

        clientTable.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) { // todo проверить метод. Ошибки в переходах
                if(event.getClickCount() == 2){
                    Path path = Paths.get(pathField.getText()).resolve(clientTable.getSelectionModel().getSelectedItem().getFilename());
                    if(Files.isDirectory(path)){
                        updateClientList(path);
                    }
                }
            }
        });


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

    // Метод, который умеет по к-л пути собрать список файлов.
    // Задача метода взять путь к какой-либо папке, и наполнить таблицу списком файлов и директорий, которые там есть
    public void updateClientList(Path path){
        try {
            pathField.setText(path.normalize().toAbsolutePath().toString());
            clientTable.getItems().clear(); // getItems - запрос списка элементов в таблице
            // стрим берет любую папку, вычитывает оттуда список файлов, преобразует их к FileInfo, и закидывает в таблицу
            clientTable.getItems().addAll(Files.list(path).map(FileInfo::new).collect(Collectors.toList()));
            clientTable.sort();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Не удалось обновить список файлов", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void updateServerList(Path path){
        try {
            serverTable.getItems().clear(); // getItems - запрос списка элементов в таблице
            // стрим берет любую папку, вычитывает оттуда список файлов, преобразует их к FileInfo, и закидывает в таблицу
            serverTable.getItems().addAll(Files.list(path).map(FileInfo::new).collect(Collectors.toList()));
            serverTable.sort();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Не удалось обновить список файлов", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void pressOnUploadBtn(){

    }

    public void pressOnDownloadBtn(){

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

    public void btnPathUpAction(ActionEvent actionEvent) {
        Path upperPath = Paths.get(pathField.getText()).getParent();
        if (upperPath != null){
            updateClientList(upperPath);
        }
    }

    public void selectDiskAction(ActionEvent actionEvent) {
        ComboBox<String> element = (ComboBox<String>) actionEvent.getSource();
        updateClientList(Paths.get(element.getSelectionModel().getSelectedItem()));
    }
}
