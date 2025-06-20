module org.example.finaltermjava_server {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires bcrypt;
    requires java.desktop;
    requires com.google.zxing;
    requires itextpdf;


    opens org.example.finaltermjava_server to javafx.fxml;
    opens org.example.finaltermjava_server.controller to javafx.fxml;
    opens org.example.finaltermjava_server.model to javafx.base;

    exports org.example.finaltermjava_server;
}