package org.example.hotelmanager;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import java.time.LocalDate;

public class UserMainController {

    @FXML private DatePicker checkInPicker, checkOutPicker;
    @FXML private VBox cardsContainer, authBox, profileBox;
    @FXML private TextField nameReg, phoneReg, passReg, emailReg, emailLogin;
    @FXML private PasswordField passwordReg, passwordLogin;
    @FXML private Label welcomeLabel;
    @FXML private TableView<BookingReport> historyTable;

    private int selectedCategoryId = -1;

    @FXML
    public void initialize() {
        phoneReg.textProperty().addListener((obs, oldV, newV) -> {
            if (!newV.startsWith("+7")) phoneReg.setText("+7");
            if (newV.length() > 12) phoneReg.setText(oldV);
        });
        updateUI();
    }

    @FXML
    public void onSearchClick() {
        LocalDate in = checkInPicker.getValue();
        LocalDate out = checkOutPicker.getValue();
        if (in == null || out == null || !out.isAfter(in)) {
            showAlert("Ошибка", "Выберите корректные даты!");
            return;
        }
        cardsContainer.getChildren().clear();
        selectedCategoryId = -1;

        for (Category cat : ClientDatabaseHandler.getAvailableCategories(in.toString(), out.toString())) {
            cardsContainer.getChildren().add(createCategoryCard(cat));
        }
    }

    private VBox createCategoryCard(Category cat) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        HBox layout = new HBox(20);
        layout.setAlignment(javafx.geometry.Pos.TOP_LEFT);

        ImageView img = new ImageView();
        try {
            Image image = new Image(getClass().getResourceAsStream("/images/" + cat.getImagePath()));
            img.setImage(image);
            img.setFitWidth(180);
            img.setPreserveRatio(true);
        } catch (Exception e) {
            System.err.println("Картинка не найдена: " + cat.getImagePath());
        }

        VBox info = new VBox(8);
        Label title = new Label(cat.getName() + " — " + cat.getPrice() + " ₽/сутки");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label desc = new Label(cat.getDescription());
        desc.setWrapText(true);
        desc.setPrefWidth(350);
        desc.setStyle("-fx-text-fill: #555; -fx-font-size: 14px;");

        Button btn = new Button("Выбрать этот тариф");
        btn.setStyle("-fx-background-color: #6200EE; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        btn.setOnAction(e -> {
            selectedCategoryId = cat.getId();
            cardsContainer.getChildren().forEach(c -> c.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);"));
            card.setStyle("-fx-background-color: #f3e5f5; -fx-padding: 15; -fx-background-radius: 12; -fx-border-color: #6200EE; -fx-border-width: 2;");
        });

        info.getChildren().addAll(title, desc, btn);
        layout.getChildren().addAll(img, info);
        card.getChildren().add(layout);

        return card;
    }

    @FXML
    public void onBookClick() {
        if (!UserSession.getInstance().isLoggedIn()) {
            showAlert("Авторизация", "Пожалуйста, войдите в аккаунт.");
            return;
        }
        if (selectedCategoryId == -1) {
            showAlert("Ошибка", "Выберите категорию номера!");
            return;
        }

        try {
            int guestId = UserSession.getInstance().getUser().getId();
            ClientDatabaseHandler.createAutoBooking(guestId, selectedCategoryId,
                    checkInPicker.getValue().toString(),
                    checkOutPicker.getValue().toString());

            showAlert("Успех", "Бронирование оформлено!");

            updateUI();

            onSearchClick();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Ошибка", "Не удалось забронировать: " + e.getMessage());
        }
    }

    @FXML
    public void onLoginClick() {
        Guest g = ClientDatabaseHandler.login(emailLogin.getText(), passwordLogin.getText());
        if (g != null) {
            UserSession.getInstance().login(g);
            updateUI();
        } else {
            showAlert("Ошибка", "Неверный логин или пароль.");
        }
    }

    @FXML
    public void onRegisterClick() {
        try {
            ClientDatabaseHandler.registerUser(nameReg.getText(), phoneReg.getText(), passReg.getText(), emailReg.getText(), passwordReg.getText());
            showAlert("Успех", "Аккаунт создан! Теперь войдите в систему.");
        } catch (Exception e) { showAlert("Ошибка", "Не удалось зарегистрировать. Возможно, email или паспорт уже заняты."); }
    }

    @FXML
    public void onLogoutClick() {
        UserSession.getInstance().logout();
        updateUI();
    }

    @FXML private ProgressBar loyaltyProgress;
    @FXML private Label loyaltyStatusLabel, spentLabel, nextLevelLabel;

    private void updateUI() {
        boolean logged = UserSession.getInstance().isLoggedIn();
        authBox.setVisible(!logged);
        profileBox.setVisible(logged);

        if (logged) {
            Guest u = UserSession.getInstance().getUser();
            welcomeLabel.setText("Добро пожаловать, " + u.getFullName());

            // Расчет прогресса
            double spent = u.getTotalSpent();
            double progress = 0;
            String nextLevelText = "";

            if (spent < 20000) {
                progress = spent / 20000;
                loyaltyStatusLabel.setText("Новичок (0%)");
                spentLabel.setText(String.format("%.0f / 20 000 ₽", spent));
                nextLevelText = String.format("До скидки 5%% осталось потратить %.0f ₽", 20000 - spent);
            } else if (spent < 50000) {
                progress = (spent - 20000) / (50000 - 20000);
                loyaltyStatusLabel.setText("Постоянный гость (5%)");
                spentLabel.setText(String.format("%.0f / 50 000 ₽", spent));
                nextLevelText = String.format("До VIP-статуса (10%%) осталось %.0f ₽", 50000 - spent);
            } else {
                progress = 1.0;
                loyaltyStatusLabel.setText("VIP-клиент (10%)");
                spentLabel.setText(String.format("%.0f ₽ потрачено", spent));
                nextLevelText = "У вас максимальная скидка!";
            }

            loyaltyProgress.setProgress(progress);
            nextLevelLabel.setText(nextLevelText);

            historyTable.setItems(ClientDatabaseHandler.getGuestHistory(u.getPassport()));
            historyTable.refresh();
        }
    }

    private void showAlert(String t, String c) {
        Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle(t); a.setHeaderText(null); a.setContentText(c); a.showAndWait();
    }

    @FXML
    public void onDownloadReceiptClick() {
        BookingReport selected = historyTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Внимание", "Выберите бронирование из списка для формирования чека.");
            return;
        }

        String receiptText = String.format(
                "------------------------------------------\n" +
                        "         ПОДТВЕРЖДЕНИЕ БРОНИРОВАНИЯ       \n" +
                        "            ОТЕЛЬ \"HOTEL MANAGER\"         \n" +
                        "------------------------------------------\n" +
                        "Номер брони:      #%d\n" +
                        "Гость:            %s\n" +
                        "Категория номера: %s\n" +
                        "Дата заезда:      %s\n" +
                        "Дата выезда:      %s\n" +
                        "------------------------------------------\n" +
                        "К ОПЛАТЕ:         %.2f ₽\n" +
                        "Статус заказа:    %s\n" +
                        "------------------------------------------\n" +
                        "Ждем вас в нашем отеле!\n" +
                        "------------------------------------------\n",
                selected.getId(), selected.getGuestName(), selected.getRoomNumber(),
                selected.getCheckIn(), selected.getCheckOut(), selected.getTotalPrice(), selected.getStatus()
        );

        try (java.io.PrintWriter out = new java.io.PrintWriter("My_Booking_" + selected.getId() + ".txt")) {
            out.println(receiptText);
            showAlert("Успех", "Чек сохранен в файл My_Booking_" + selected.getId() + ".txt");
        } catch (Exception e) {
            showAlert("Ошибка", "Не удалось сохранить чек.");
        }
    }
}