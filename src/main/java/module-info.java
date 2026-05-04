module org.example.hotelmanager {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens org.example.hotelmanager to javafx.fxml;
    exports org.example.hotelmanager;
}