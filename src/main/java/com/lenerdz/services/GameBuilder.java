package com.lenerdz.services;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

import com.lenerdz.services.GameBuilder;

import io.github.cdimascio.dotenv.Dotenv;

public class GameBuilder {
    String guildStringID;
    public GameBuilder(String givenGuildID) {
        guildStringID = givenGuildID;
    }

    public ResultSet getCurrentGameResultSet() {
        Dotenv dotenv = Dotenv.load();
        try (Connection conn = DriverManager.getConnection(dotenv.get("JDBC_URL"));
               Statement stmt = conn.createStatement();) {

            int gameID = currentGameID(stmt);        
            
            if (gameID == -1) {
                return null;
            }

            return stmt.executeQuery("SELECT p.WordleName, gp.PlayerID, g.Name, SUM(s.SubScore) AS totalSubScore, SUM(s.SuperScore) AS totalSuperScore FROM GamesPlayers gp JOIN Games g ON gp.GameID = g.ID JOIN Scores s ON gp.PlayerID = s.PlayerID AND gp.GameID = s.GameID JOIN Players p on gp.PlayerID = p.ID WHERE g.ID = " + gameID + " GROUP BY p.WordleName, gp.PlayerID, gp.GameID ORDER BY p.WordleName;");
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getCurrrentGame() {
        Dotenv dotenv = Dotenv.load();
        try (Connection conn = DriverManager.getConnection(dotenv.get("JDBC_URL"));
               Statement stmt = conn.createStatement();) {

            int gameID = currentGameID(stmt);        
            
            if (gameID == -1) {
                return "No current game in this server!";
            }

            ResultSet gameInfo = stmt.executeQuery("SELECT p.WordleName, gp.PlayerID, g.Name, SUM(s.SubScore) AS totalSubScore, SUM(s.SuperScore) AS totalSuperScore FROM GamesPlayers gp JOIN Games g ON gp.GameID = g.ID JOIN Scores s ON gp.PlayerID = s.PlayerID AND gp.GameID = s.GameID JOIN Players p on gp.PlayerID = p.ID WHERE g.ID = " + gameID + " GROUP BY p.WordleName, gp.PlayerID, gp.GameID ORDER BY p.WordleName;");
            
            String gameName = "";
            String gameStatus = "";
            while (gameInfo.next()) {
               gameName = gameInfo.getString("Name");
               gameStatus += "\n" + gameInfo.getString("WordleName") + ": " + gameInfo.getDouble("totalSuperScore") + " " + gameInfo.getInt("totalSubScore");
            }

            if (gameName == "") {
                gameName = getGameName(stmt, gameID);
                gameStatus = "\nNo Players";
            }

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

            int confirmedGameID = pastGameID(stmt, gameID);
            if (confirmedGameID == -1) {
                return "There is no game with that ID in this server!";
            }

            ResultSet gameInfo = stmt.executeQuery("SELECT p.WordleName, g.PlayerID AS WinnerID, gp.PlayerID, g.Name, SUM(s.SubScore) AS totalSubScore, SUM(s.SuperScore) AS totalSuperScore FROM GamesPlayers gp JOIN Games g ON gp.GameID = g.ID JOIN Scores s ON gp.PlayerID = s.PlayerID AND gp.GameID = s.GameID JOIN Players p on gp.PlayerID = p.ID JOIN Guilds glds ON glds.ID = g.GuildID WHERE g.ID = " + gameID + " AND glds.GuildStringID = \"" + guildStringID + "\" GROUP BY p.WordleName, gp.PlayerID, gp.GameID ORDER BY p.WordleName;");
            
            String gameName = "";
            String gameStatus = "";
            int winnerID = -1;
            String winnerName = "";
            int count = 0;
            while (gameInfo.next()) {
               gameName = gameInfo.getString("Name");
               String playerName = gameInfo.getString("WordleName");
               gameStatus += "\n" + playerName + ": " + gameInfo.getDouble("totalSuperScore") + " " + gameInfo.getInt("totalSubScore");
               winnerID = gameInfo.getInt("WinnerID");
               if (winnerID == gameInfo.getInt("PlayerID")) {
                    winnerName = playerName;
               } else if (count > 0) {
                    winnerName = "NONE - A TIE!";
               } else {
                    winnerName = "SINGLE PLAYER DEFAULT WIN";
               }
               count++;
            }

            if (gameName == "") {
                gameName = getGameName(stmt, gameID);
                gameStatus = "\nNo Players";
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
        ResultSet gameInfo = stmt.executeQuery("SELECT g.ID AS GameID FROM Games g JOIN Guilds glds ON g.guildID = glds.ID WHERE g.Current = 1 AND glds.GuildStringID = \"" + guildStringID + "\";");
        while(gameInfo.next()) {
            return gameInfo.getInt("GameID");
        }
        return -1;
    }

    private int pastGameID(Statement stmt, int pastID) throws SQLException {
        ResultSet gameInfo = stmt.executeQuery("SELECT g.ID AS GameID FROM Games g JOIN Guilds glds ON g.guildID = glds.ID WHERE g.ID = " + pastID + " AND glds.GuildStringID = \"" + guildStringID + "\";");
        while(gameInfo.next()) {
            return gameInfo.getInt("GameID");
        }
        return -1;
    }

    private String getGameName(Statement stmt, int currentID) throws SQLException {
        ResultSet gameInfo = stmt.executeQuery("SELECT Name FROM Games WHERE ID = " + currentID + ";");
        while(gameInfo.next()) {
            return gameInfo.getString("Name");
        }
        return "No Name!";
    }
}
