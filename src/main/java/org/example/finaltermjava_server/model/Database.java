package org.example.finaltermjava_server.model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    public static final String URL = "jdbc:mysql://localhost:3306/ticket";
    public static final String USER = "root";
    public static final String PASSWORD = "";
    public static Connection getConnection() throws SQLException{
        return DriverManager.getConnection(URL,USER,PASSWORD);
    }

}
