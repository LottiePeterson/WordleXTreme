package com.lenerdz.commands;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Score extends ListenerAdapter {

   @Override
   public void onMessageReceived(MessageReceivedEvent event) {
      String[] message = event.getMessage().getContentRaw().split(" ");
      
      if (message[0].equals("Wordle") && message[1].equalsIgnoreCase("score")) {
         Dotenv dotenv = Dotenv.load();
         try (Connection conn = DriverManager.getConnection(dotenv.get("JDBC_URL"));
               Statement stmt = conn.createStatement();) {
            int[] subScore = new int[message.length - 2];
            for(int i = 2; i < message.length; i++){
                subScore[i - 2] = Integer.parseInt(message[i]);
            }
            double[] superScore = Score4Players.score4Players(subScore);
            for(int i = 0; i < superScore.length; i++){
                event.getChannel().sendMessage(superScore[i] + " "+ subScore[i]).queue();
            }
            // Execute a query
            String sql = "INSERT INTO `Scores` (`GameID`, `PlayerID`, `WordleNum`, `CumulativeScore`, `CompetitiveScore`) VALUES (1, 1, 674, 2, NULL)";
            stmt.executeUpdate(sql);
         } catch (SQLException e) {
            e.printStackTrace();
         }
      }
   }

}