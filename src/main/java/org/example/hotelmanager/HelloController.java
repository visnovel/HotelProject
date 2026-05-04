package org.example.hotelmanager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class HelloController {

    @FXML
    protected void onAdminButtonClick(ActionEvent event) {
        loadScene(event, "auth.fxml", "Авторизация Администратора");
    }

    @FXML
    protected void onClientButtonClick(ActionEvent event) {
        loadScene(event, "user_main.fxml", "Бронирование номеров");
    }

    private void loadScene(ActionEvent event, String fxmlFile, String title) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fxmlFile));
            Scene scene = new Scene(fxmlLoader.load());

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Не удалось загрузить файл: " + fxmlFile);
        }
    }
}