package org.example.hotelmanager;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public class AdminPanelController {

    @FXML private TableView<Object> mainTable;
    @FXML private ScrollPane chessboardPane;
    @FXML private GridPane chessboardGrid;
    @FXML private VBox financePane;
    @FXML private LineChart<String, Number> revenueChart;

    @FXML private HBox roomControls, bookingControls, reportControls, guestControls;

    @FXML private TextField roomNumberField;
    @FXML private ComboBox<String> categoryCombo, statusCombo;

    @FXML private TextField guestNameField, guestPhoneField, guestPassportField;
    @FXML private ComboBox<String> bookingRoomCombo;
    @FXML private DatePicker checkInPicker, checkOutPicker;

    @FXML private ComboBox<String> reportStatusCombo;

    private final LocalDate START_DATE = LocalDate.of(2026, 5, 1);
    private final int DAYS_COUNT = 31;
    private final int ROW_HEIGHT = 55;

    @FXML
    public void initialize() {
        categoryCombo.setItems(FXCollections.observableArrayList("Стандарт", "Люкс", "Апартаменты"));
        statusCombo.setItems(FXCollections.observableArrayList("Доступен", "Занят", "Требует уборки"));
        reportStatusCombo.setItems(FXCollections.observableArrayList("Активна", "Подтверждение оплаты", "Отмена брони", "Выполнено"));

        checkInPicker.valueProperty().addListener((obs, oldV, newV) -> updateAvailableRooms());
        checkOutPicker.valueProperty().addListener((obs, oldV, newV) -> updateAvailableRooms());

        onRoomsMenuClick();
    }

    private void switchView(String mode) {
        mainTable.setVisible(!mode.equals("chess") && !mode.equals("finance"));
        chessboardPane.setVisible(mode.equals("chess"));
        financePane.setVisible(mode.equals("finance"));

        roomControls.setVisible(mode.equals("rooms"));
        bookingControls.setVisible(mode.equals("chess"));
        if (guestControls != null) guestControls.setVisible(mode.equals("guests"));
        if (reportControls != null) reportControls.setVisible(mode.equals("reports"));
    }

    @FXML
    public void onRoomsMenuClick() {
        switchView("rooms");
        mainTable.getColumns().clear();
        addColumn("ID", "id", 50);
        addColumn("Номер", "roomNumber", 100);
        addColumn("Категория", "category", 150);
        addColumn("Статус", "status", 120);
        addColumn("Цена (сутки)", "price", 100);

        mainTable.setItems(AdminDatabaseHandler.getRoomsList());
    }

    @FXML
    public void onAddRoomClick() {
        String num = roomNumberField.getText();
        String cat = categoryCombo.getValue();
        if (num.isEmpty() || cat == null) return;

        int catId = cat.equals("Люкс") ? 2 : (cat.equals("Апартаменты") ? 3 : 1);
        try {
            AdminDatabaseHandler.addRoom(num, catId);
            onRoomsMenuClick();
            roomNumberField.clear();
        } catch (SQLException e) {
            showAlert("Ошибка", "Такой номер уже есть в базе!");
        }
    }

    @FXML
    public void onGuestsMenuClick() {
        switchView("guests");
        mainTable.getColumns().clear();

        addColumn("ID", "id", 40);
        addColumn("ФИО Гостя", "fullName", 200);
        addColumn("Телефон", "phone", 130);
        addColumn("Паспорт", "passport", 120);

        addColumn("Визиты", "visits", 80);

        addColumn("Лояльность", "loyaltyStatus", 150);

        mainTable.setItems(AdminDatabaseHandler.getGuestsList());
    }

    @FXML
    public void onShowGuestHistoryClick() {
        Object selected = mainTable.getSelectionModel().getSelectedItem();
        if (selected instanceof Guest) {
            int guestId = ((Guest) selected).getId();
            onReportsMenuClick();
            mainTable.setItems(AdminDatabaseHandler.getGuestReportsList(guestId));
        } else {
            showAlert("Внимание", "Сначала выберите гостя в списке!");
        }
    }

    @FXML
    public void onChessboardMenuClick() {
        switchView("chess");
        drawChessboard();
    }

    private void drawChessboard() {
        chessboardGrid.getChildren().clear();
        chessboardGrid.getColumnConstraints().clear();
        chessboardGrid.getColumnConstraints().add(new ColumnConstraints(140));

        for (int i = 0; i < DAYS_COUNT; i++) {
            chessboardGrid.getColumnConstraints().add(new ColumnConstraints(65));
            LocalDate date = START_DATE.plusDays(i);
            boolean isWeekend = date.getDayOfWeek().getValue() >= 6;
            Label d = new Label(date.getDayOfMonth() + "\nмай");
            d.setStyle("-fx-background-color: " + (isWeekend ? "#fce4ec;" : "#f5f5f5;") +
                    " -fx-border-color: #ddd; -fx-alignment: center; -fx-font-weight: bold;");
            d.setPrefSize(65, 45);
            chessboardGrid.add(d, i + 1, 0);
        }

        Map<Integer, Integer> rowMap = new HashMap<>();
        int row = 1;
        Map<Integer, String> roomsMap = AdminDatabaseHandler.getRoomsMap();
        for (Map.Entry<Integer, String> entry : roomsMap.entrySet()) {
            rowMap.put(entry.getKey(), row);
            Label rl = new Label(" Номер " + entry.getValue());
            rl.setPrefSize(140, ROW_HEIGHT);
            rl.setStyle("-fx-border-color: #ddd; -fx-background-color: white; -fx-font-weight: bold; -fx-padding: 0 0 0 10;");
            chessboardGrid.add(rl, 0, row++);
        }

        for (String[] b : AdminDatabaseHandler.getChessboardBookings()) {
            try {
                LocalDate in = LocalDate.parse(b[0]);
                LocalDate out = LocalDate.parse(b[1]);
                int rId = Integer.parseInt(b[2]);
                int startCol = (int) ChronoUnit.DAYS.between(START_DATE, in) + 1;
                int span = (int) Math.abs(ChronoUnit.DAYS.between(in, out));

                if (rowMap.containsKey(rId) && startCol > 0) {
                    Label stripe = new Label(" " + b[3]);
                    stripe.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 11px;");
                    stripe.setMaxSize(Double.MAX_VALUE, ROW_HEIGHT - 12);
                    GridPane.setMargin(stripe, new javafx.geometry.Insets(6, 2, 6, 2));
                    chessboardGrid.add(stripe, startCol, rowMap.get(rId), span, 1);
                }
            } catch (Exception ignored) {}
        }
    }

    private void updateAvailableRooms() {
        LocalDate in = checkInPicker.getValue();
        LocalDate out = checkOutPicker.getValue();
        if (in != null && out != null && in.isBefore(out)) {
            bookingRoomCombo.setItems(AdminDatabaseHandler.getAvailableRooms(in.toString(), out.toString()));
        }
    }

    @FXML
    public void onAddBookingClick() {
        String name = guestNameField.getText();
        String room = bookingRoomCombo.getValue();
        if (name.isEmpty() || room == null || checkInPicker.getValue() == null) return;

        try {
            int guestId = AdminDatabaseHandler.getOrCreateGuest(name, guestPassportField.getText(), guestPhoneField.getText());
            AdminDatabaseHandler.createBooking(guestId, room, checkInPicker.getValue().toString(), checkOutPicker.getValue().toString());
            drawChessboard();
            showAlert("Успех", "Бронирование создано!");
        } catch (SQLException e) {
            showAlert("Ошибка", "Не удалось создать бронь.");
        }
    }

    @FXML
    public void onReportsMenuClick() {
        switchView("reports");
        mainTable.getColumns().clear();
        addColumn("Бронь №", "id", 70);
        addColumn("ФИО Гостя", "guestName", 180);
        addColumn("Номер", "roomNumber", 70);
        addColumn("Заезд", "checkIn", 100);
        addColumn("Выезд", "checkOut", 100);
        addColumn("Итого (₽)", "totalPrice", 110);
        addColumn("Статус", "status", 120);

        mainTable.setItems(AdminDatabaseHandler.getReportsList());
    }

    @FXML
    public void onFinanceMenuClick() {
        switchView("finance");
        revenueChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Выручка (₽) за Май 2026");

        Map<String, Double> stats = AdminDatabaseHandler.getRevenueData();
        stats.forEach((date, sum) -> series.getData().add(new XYChart.Data<>(date, sum)));

        revenueChart.getData().add(series);
    }

    @FXML
    public void onChangeStatusClick() {
        Object selected = mainTable.getSelectionModel().getSelectedItem();
        String newStatus = reportStatusCombo.getValue();
        if (selected instanceof BookingReport && newStatus != null) {
            try {
                AdminDatabaseHandler.updateBookingStatus(((BookingReport) selected).getId(), newStatus);
                onReportsMenuClick();
            } catch (SQLException e) {
                showAlert("Ошибка", "Не удалось обновить статус.");
            }
        }
    }

    private void addColumn(String title, String property, double width) {
        TableColumn<Object, Object> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        col.setPrefWidth(width);
        mainTable.getColumns().add(col);
    }

    @FXML
    public void onDeleteButtonClick() {
        Object selected = mainTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            int id = (selected instanceof Room) ? ((Room) selected).getId() : ((Guest) selected).getId();
            String table = (selected instanceof Room) ? "rooms" : "guests";
            AdminDatabaseHandler.deleteRecord(table, id);
            if (selected instanceof Room) onRoomsMenuClick(); else onGuestsMenuClick();
        } catch (SQLException e) {
            showAlert("Ошибка", "Нельзя удалить объект, у которого есть история бронирований!");
        }
    }

    private void showAlert(String t, String c) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(t); a.setHeaderText(null); a.setContentText(c); a.showAndWait();
    }

    @FXML
    public void onGenerateReceiptClick() {
        Object selected = mainTable.getSelectionModel().getSelectedItem();
        if (!(selected instanceof BookingReport)) {
            showAlert("Ошибка", "Выберите выполненную бронь в таблице отчетов!");
            return;
        }

        BookingReport report = (BookingReport) selected;

        String receiptText = String.format(
                "==========================================\n" +
                        "        ОТЕЛЬ \"HOTEL MANAGER\"             \n" +
                        "           ТОВАРНЫЙ ЧЕК                   \n" +
                        "==========================================\n" +
                        "ID Бронирования:  %d\n" +
                        "Гость:            %s\n" +
                        "Номер:            %s\n" +
                        "Период:           %s — %s\n" +
                        "------------------------------------------\n" +
                        "ИТОГО К ОПЛАТЕ:   %.2f ₽\n" +
                        "Статус:           %s\n" +
                        "==========================================\n" +
                        "      Спасибо, что выбрали нас!           \n" +
                        "==========================================\n",
                report.getId(), report.getGuestName(), report.getRoomNumber(),
                report.getCheckIn(), report.getCheckOut(), report.getTotalPrice(), report.getStatus()
        );

        try (java.io.PrintWriter out = new java.io.PrintWriter("receipt_" + report.getId() + ".txt")) {
            out.println(receiptText);
            showAlert("Успех", "Чек успешно сформирован в файл: receipt_" + report.getId() + ".txt");
        } catch (Exception e) {
            showAlert("Ошибка", "Не удалось сохранить файл.");
        }
    }

    @FXML
    public void onCheckOutClick() {
        Object selected = mainTable.getSelectionModel().getSelectedItem();
        if (!(selected instanceof BookingReport)) {
            showAlert("Внимание", "Выберите активную бронь в таблице отчетов!");
            return;
        }

        BookingReport report = (BookingReport) selected;
        if (!report.getStatus().equals("Активна")) {
            showAlert("Инфо", "Выезд можно оформить только для активных броней.");
            return;
        }

        try {
            int roomId = AdminDatabaseHandler.getRoomIdByNumber(report.getRoomNumber());

            AdminDatabaseHandler.completeBooking(report.getId(), roomId);

            showAlert("Успех", "Выезд оформлен. Номер переведен в статус 'Уборка', " +
                    "а гостю начислена сумма в лояльность.");

            onReportsMenuClick();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}