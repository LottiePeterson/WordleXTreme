package com.lenerdz.commands;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class CalcScore extends ListenerAdapter {

   @Override
   public void onMessageReceived(MessageReceivedEvent event) {
      String message = event.getMessage().getContentRaw();
      
      if (message.equalsIgnoreCase("!calcScores")) {
         Dotenv dotenv = Dotenv.load();
         try (Connection conn = DriverManager.getConnection(dotenv.get("JDBC_URL"));
               Statement stmt = conn.createStatement();) {
            // Execute a query
            String sql = "INSERT INTO `Scores` (`GameID`, `PlayerID`, `WordleNum`, `CumulativeScore`, `CompetitiveScore`) VALUES (1, 1, 674, 2, NULL)";
            stmt.executeUpdate(sql);
         } catch (SQLException e) {
            e.printStackTrace();
         }
      }
   }

}