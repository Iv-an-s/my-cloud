package com.mycompany.my.cloud.server;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class DBAuthenticationProvider implements AuthenticationProvider {
    private static final Logger log = LogManager.getLogger(DBAuthenticationProvider.class);
    private DbConnection dbConnection;

    @Override
    public void init() {
        dbConnection = new DbConnection();
    }

    @Override
    public Optional<String> getNicknameByLoginAndPassword(String login, String password) {
        String query = String.format("select username from users where login = '%s' and password = '%s';", login, password);
        try (ResultSet rs = dbConnection.getStmt().executeQuery(query)){
            if(rs.next()){
                return Optional.of(rs.getString("username"));
            }

        }catch (SQLException e){
            log.throwing(Level.ERROR, e);
        }
        return Optional.empty();
    }

    @Override
    public boolean changeNickname(String oldNickname, String newNickname) {
        String query = String.format("update users set nickname = '%s' where nickname = '%s';", newNickname, oldNickname);
        try {
            dbConnection.getStmt().executeUpdate(query);
            return true;
        } catch (SQLException e) {
            log.throwing(Level.ERROR, e);
            return false;
        }
    }

    @Override
    public boolean isNickBusy(String nickname) {
        String query = String.format("select id from users where nickname = '%s';", nickname);
        ResultSet rs = null;
        try {
            rs = dbConnection.getStmt().executeQuery(query);
            if (rs.next()){
                return true;
            }
        } catch (SQLException e) {
            log.throwing(Level.ERROR, e);
        }
        return false;
    }

    @Override
    public void shutdown() {
        dbConnection.close();
    }
}
