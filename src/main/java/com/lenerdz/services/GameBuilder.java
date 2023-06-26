package com.lenerdz.services;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

import com.lenerdz.services.GameBuilder;

import io.github.cdimascio.dotenv.Dotenv;

public class GameBuilder {
    public GameBuilder() {

    }

    public String getCurrrentGame() {
        Dotenv dotenv = Dotenv.load();
        try (Connection conn = DriverManager.getConnection(dotenv.get("JDBC_URL"));
               Statement stmt = conn.createStatement();) {

            int gameID = currentGameID(stmt);
            
            
            if (gameID == -1) {
                return "No current game in this server!";
            }
            // if (!hasPlayers(stmt, gameID)) {
            //     gameStatus = "No Players";
            // }

            ResultSet gameInfo = stmt.executeQuery("SELECT p.WordleName, gp.PlayerID, gp.GameID, g.Name, SUM(s.SubScore) AS totalSubScore, SUM(s.SuperScore) AS totalSuperScore FROM GamesPlayers gp JOIN Games g ON gp.GameID = g.ID JOIN Scores s ON gp.PlayerID = s.PlayerID AND gp.GameID = s.GameID JOIN Players p on gp.PlayerID = p.ID WHERE g.Current = 1 GROUP BY p.WordleName, gp.PlayerID, gp.GameID ORDER BY p.WordleName;");
            
            String gameName = "";
            String gameStatus = "";
            while (gameInfo.next()) {
               gameName = gameInfo.getString("Name");
               gameID = gameInfo.getInt("GameID");
               gameStatus += "\n" + gameInfo.getString("WordleName") + ": " + gameInfo.getDouble("totalSuperScore") + " " + gameInfo.getInt("totalSubScore");
            }

            if (gameName == "") {
                gameName = curretGameName(stmt);
                gameStatus = "\nNo Players";
            }

            // if (gameID == -1) {
            //    return "No current game in this server!";
            // }
            String result = "=====** " + gameName + " **=====" + gameStatus + "\n=====*Game ID " + gameID + "*=====";
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            return "Oh no! Something went wrong!";
        }
    }

    public String getPreviousGame(int gameID) {
        Dotenv dotenv = Dotenv.load();
        try (Connection conn = DriverManager.getConnection(dotenv.get("JDBC_URL"));
               Statement stmt = conn.createStatement();) {

            ResultSet gameInfo = stmt.executeQuery("SELECT p.WordleName, g.PlayerID AS WinnerID, gp.PlayerID, gp.GameID, g.Name, SUM(s.SubScore) AS totalSubScore, SUM(s.SuperScore) AS totalSuperScore FROM GamesPlayers gp JOIN Games g ON gp.GameID = g.ID JOIN Scores s ON gp.PlayerID = s.PlayerID AND gp.GameID = s.GameID JOIN Players p on gp.PlayerID = p.ID WHERE g.ID = " + gameID + " GROUP BY p.WordleName, gp.PlayerID, gp.GameID ORDER BY p.WordleName;");
            
            String gameName = "";
            String gameStatus = "";
            int winnerID = -1;
            String winnerName = "";
            while (gameInfo.next()) {
               gameName = gameInfo.getString("Name");
               String playerName = gameInfo.getString("WordleName");
               gameStatus += "\n" + playerName + ": " + gameInfo.getDouble("totalSuperScore") + " " + gameInfo.getInt("totalSubScore");
               winnerID = gameInfo.getInt("WinnerID");
               if (winnerID == gameInfo.getInt("PlayerID")) {
                    winnerName = playerName;
               }
            }

            if (gameName == "") {
                return "There is no game with that ID in this server!";
            }
            String result = "";
            if(winnerID == -1) {
                result = "=====** " + gameName + " **=====" + gameStatus + "\n=====*Game ID " + gameID + "*=====";
            } else {
                result = "=====** " + gameName + " **=====" + gameStatus + "\n**WINNER: " + winnerName + "**\n=====*Game ID " + gameID + "*=====";
            }

            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            return "Oh no! Something went wrong!";
        }
    }

    private int currentGameID(Statement stmt) throws SQLException {
        ResultSet gameInfo = stmt.executeQuery("SELECT ID FROM Games WHERE Current = 1;");
        while(gameInfo.next()) {
            return gameInfo.getInt("ID");
        }
        return -1;
    }

    private String curretGameName(Statement stmt) throws SQLException {
        ResultSet gameInfo = stmt.executeQuery("SELECT Name FROM Games WHERE Current = 1;");
        while(gameInfo.next()) {
            return gameInfo.getString("Name");
        }
        return "No Name!";
    }
}
