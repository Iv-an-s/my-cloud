package com.mycompany.my.cloud.client;

import com.mycompany.my.cloud.common.FileInfo;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ServerPanelController implements Initializable {

//    @FXML
//    ComboBox<String> disksBox;
    @FXML
    TableView<FileInfo> serverTable;
    @FXML
    TextField textField;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
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

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        TableColumn<FileInfo, String> serverFileDateColumn = new TableColumn<>("Дата изменения");
        serverFileDateColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastModified().format(dtf)));
        serverFileDateColumn.setPrefWidth(120);

        //table.getColumns().add(clientFileTypeColumn);
        serverTable.getColumns().addAll(serverFileTypeColumn, serverFilenameColumn, serverFileSizeColumn, serverFileDateColumn);
        serverTable.getSortOrder().add(serverFileTypeColumn); // задали столбец, по которому будем по умолчанию сортироваться

        updateServerList(Paths.get(".")); // todo создать метод updateServerList()

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

    public String getSelectedFilename() { // todo написать метод для серверной таблицы
        if (!serverTable.isFocused()) {
            return null;
        }
        return serverTable.getSelectionModel().getSelectedItem().getFilename();
    }

//    public String getCurrentPath() {
//        return pathField.getText();
//    }

}
