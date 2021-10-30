package com.mycompany.my.cloud.client;

import com.mycompany.my.cloud.common.FileInfo;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {// Интерфейс дает возможность проводить подготовительные действия,
    // проводить преднастройку контроллера

    @FXML
    TextField loginField, textField;
    @FXML
    PasswordField passwordField;
    @FXML
    VBox rootElement, loginPanel, clientPanel;
    @FXML
    HBox tablesPanel, buttonPanel;
    @FXML
    MenuBar menuPanel;
    @FXML
    TableView<FileInfo> serverTable;

    private Network network;
    private String username;
    private List<FileInfo> fileInfoList;
    private URL location;
    private ResourceBundle resources;

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
        /** Инициализируем переменные */
        this.location = location;
        this.resources = resources;
        setUsername(null);
        network = new Network();
        fileInfoList = new ArrayList<>();

        /** НАСТРАИВАЕМ СЕРВЕРНУЮ ТАБЛИЦУ */
        TableColumn<FileInfo, String> serverFileTypeColumn = new TableColumn<>();
        serverFileTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().getName()));
        serverFileTypeColumn.setPrefWidth(24);

        TableColumn<FileInfo, String> serverFilenameColumn = new TableColumn<>("Имя");
        serverFilenameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFilename()));
        serverFilenameColumn.setPrefWidth(240);

        TableColumn<FileInfo, Long> serverFileSizeColumn = new TableColumn<>("Размер");
        serverFileSizeColumn.setCellValueFactory(param -> new SimpleObjectProperty(param.getValue().getSize()));
        serverFileSizeColumn.setCellFactory(column -> {
            return new TableCell<FileInfo, Long>() {
                @Override
                protected void updateItem(Long item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        String text = String.format("%,d bytes", item); // разделение порядков (по 3 цифры) через пробел
                        if (item == -1L) {
                            text = "[DIR]";
                        }
                        setText(text);
                    }
                }
            };
        });
        serverFileSizeColumn.setPrefWidth(120);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        TableColumn<FileInfo, String> serverFileDateColumn = new TableColumn<>("Дата изменения");
        serverFileDateColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastModified().format(dtf)));
        serverFileDateColumn.setPrefWidth(120);

        serverTable.getColumns().clear();
        serverTable.getColumns().addAll(serverFileTypeColumn, serverFilenameColumn, serverFileSizeColumn, serverFileDateColumn);
        serverTable.getSortOrder().add(serverFileTypeColumn); // задали столбец, по которому будем по умолчанию сортироваться

        /** ИНИЦИАЛИЗИРУЕМ КОЛБЕКИ */

        network.setOnAuthFailedCallback(new Callback() {
            @Override
            public void callback(Object... args) {
                String cause = (String) args[0];
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
            }
        });

        network.setOnUpdateServerFileList(new Callback() {
            @Override
            public void callback(Object... args) {
                String fileList = (String) args[0];
                String[] fileListTokens = fileList.split(" ");
                if (fileListTokens.length % 4 != 0) {
                    showErrorAlert("Получен некорректный список файлов от сервера!");
                    return;
                }
                for (int i = 0; i < fileListTokens.length; i += 4) {
                    String filename = fileListTokens[i];
                    System.out.println("Controller: filename = " + filename);
                    String type = fileListTokens[i + 1]; // false = FILE, true = DIRECTORY
                    System.out.println("Controller: type = " + type);
                    long size = Long.parseLong(fileListTokens[i + 2]);
                    System.out.println("Controller: size = " + size);
                    LocalDateTime lastModified = createLastModifyLocalDateTime(fileListTokens[i + 3]);
                    System.out.println("Controller: lastModified = " + lastModified);
                    FileInfo fileInfo = new FileInfo(filename, type, size, lastModified);
                    fileInfoList.add(fileInfo);
                }
                System.out.println("Controller: fileInfoList is [" + fileInfoList + "]");
                updateServerList(fileInfoList); // todo создать метод updateServerList()
            }
        });

        network.setOnDisconnectCallback(new Callback() {
            @Override
            public void callback(Object... args) {
                // сбрасываем имя пользователя, и забываем про его историю
                setUsername(null);
            }
        });

        network.setOnHandleCommandCallback(new Callback() {
            @Override
            public void callback(Object... args) {

            }
        });

    }

    /** МЕТОДЫ */
    public void updateServerList(List<FileInfo> list) {
        serverTable.getItems().clear(); // getItems - запрос списка элементов в таблице
        for (int i = 0; i < list.size(); i++) {
            System.out.println("fileList: " + list.get(i).getFilename() + " " + list.get(i).getType() + " " + list.get(i).getSize() + " " + list.get(i).getLastModified());
        }
        serverTable.getItems().addAll(list);
        serverTable.sort();
    }

    public String getSelectedFilename() { // todo написать метод для серверной таблицы
        if (!serverTable.isFocused()) {
            return null;
        }
        return serverTable.getSelectionModel().getSelectedItem().getFilename();
    }

    private LocalDateTime createLastModifyLocalDateTime(String lastModified) {
        //2021-10-21T13:39:15.559917
        //public static LocalDateTime of(int year, Month month, int dayOfMonth, int hour, int minute) {
        int year = Integer.parseInt(lastModified.substring(0, 4));
        int month = Integer.parseInt(lastModified.substring(5, 7));
        int day = Integer.parseInt(lastModified.substring(8, 10));
        int hour = Integer.parseInt(lastModified.substring(11, 13));
        int minute = Integer.parseInt(lastModified.substring(14, 16));
        return LocalDateTime.of(year, month, day, hour, minute);
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

    public void exit() {
        Platform.exit();
        network.disconnect();
    }

    private static void updateUI(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
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
        exit();
    }

    public void btnDownloadAction(ActionEvent actionEvent) {
        ClientPanelController clientPanelController = (ClientPanelController) clientPanel.getProperties().get("ctrl");
        String fileNameForDownload;

        if ((fileNameForDownload = this.getSelectedFilename()) == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Не выбран файл для скачивания", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        // Источник - this <-- server, назначение - clientPanelController
        Path dstPath = Paths.get(clientPanelController.getCurrentPath(), fileNameForDownload);
        network.getFile(fileNameForDownload, dstPath);
    }

    public void btnUploadAction(ActionEvent actionEvent) {
        ClientPanelController clientPanelController = (ClientPanelController) clientPanel.getProperties().get("ctrl");

        if (clientPanelController.getSelectedFilename() == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Не выбран файл для загрузки в удаленный репозиторий", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        // Источник - clientPanelController, назначение - this --> server
        Path srcPath = Paths.get(clientPanelController.getCurrentPath(), clientPanelController.getSelectedFilename());
        try {
            network.sendFile(srcPath, username);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void btnDeleteAction(ActionEvent actionEvent) {
        String fileNameForDelete;

        if ((fileNameForDelete = this.getSelectedFilename()) == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Не выбран файл для удаления", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        // todo написать Предупрежедение об удалении с подтверждением или отменой действия
        network.deleteFile(fileNameForDelete, username);
    }

    public void btnLogOutAction(ActionEvent actionEvent) {
        this.username = null;

        initialize(location, resources);
    }
}
