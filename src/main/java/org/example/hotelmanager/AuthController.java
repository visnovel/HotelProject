package org.example.hotelmanager;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AuthController {

    @FXML private TextField loginField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;

    @FXML
    protected void onLoginButtonClick() {
        String login = loginField.getText();
        String password = passwordField.getText();

        if (login.isEmpty() || password.isEmpty()) {
            showAlert("Ошибка", "Введите логин и пароль!");
            return;
        }

        String sql = "SELECT * FROM admins WHERE login = ? AND password = ?";
        try (Connection conn = AdminDatabaseHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, login);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                loadAdminPanel();
            } else {
                showAlert("Ошибка доступа", "Неверный логин или пароль!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Ошибка", "Детали: " + e.getCause());
        }
    }

    private void loadAdminPanel() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("admin_panel.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 900, 600);

        Stage stage = (Stage) loginButton.getScene().getWindow();
        stage.setScene(scene);
        stage.setTitle("Система управления отелем - Панель администратора");
        stage.centerOnScreen();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}