module org.example.finaltermjava_server {

    requires javafx.controls;
    requires javafx.fxml;

    requires java.sql;
    requires java.desktop;

    requires bcrypt;
    requires com.google.zxing;
    requires itextpdf;

    opens org.example.finaltermjava_server to javafx.fxml;
    opens org.example.finaltermjava_server.controller to javafx.fxml;
    opens org.example.finaltermjava_server.model to javafx.base;

    exports org.example.finaltermjava_server;
    exports org.example.finaltermjava_server.controller;
}