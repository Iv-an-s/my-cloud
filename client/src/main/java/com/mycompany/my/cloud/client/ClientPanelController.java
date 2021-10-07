package com.mycompany.my.cloud.client;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ClientPanelController implements Initializable {
    @FXML
    ComboBox<String> disksBox;
    @FXML
    TableView<FileInfo> clientTable;
    @FXML
    TextField pathField;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
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
    }

    public void selectDiskAction(ActionEvent actionEvent) {
        ComboBox<String> element = (ComboBox<String>) actionEvent.getSource();
        updateClientList(Paths.get(element.getSelectionModel().getSelectedItem()));
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

    public String getSelectedFilename(){ // todo написать метод для серверной таблицы
        if(!clientTable.isFocused()){
            return null;
        }
        return clientTable.getSelectionModel().getSelectedItem().getFilename();
    }

    public String getCurrentPath(){
        return pathField.getText();
    }

    public void btnPathUpAction(ActionEvent actionEvent) {
        Path upperPath = Paths.get(pathField.getText()).getParent();
        if (upperPath != null){
            updateClientList(upperPath);
        }
    }
}
