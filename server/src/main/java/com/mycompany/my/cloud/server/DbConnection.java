package com.mycompany.my.cloud.server;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DbConnection {
    //private static final Logger log = LogManager.getLogger(DbConnection.class);
    private Connection connection;
    private Statement stmt;

    public DbConnection() {
        try {
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection("jdbc:sqlite:clouddatabase.db");
            this.stmt = connection.createStatement();
        } catch (ClassNotFoundException | SQLException e) {
            //log.throwing(Level.ERROR, e);
            e.printStackTrace();
            throw new RuntimeException("Невозможно подключиться к базе данных");
        }
    }

    public Statement getStmt() {
        return stmt;
    }

    public void close(){
        if (stmt != null){
            try {
                stmt.close();
            } catch (SQLException throwables) {
                //log.throwing(Level.ERROR, throwables);
            }
        }
        if (connection != null){
            try {
                connection.close();
            } catch (SQLException throwables) {
                //log.throwing(Level.ERROR, throwables);
            }
        }
    }
 }
