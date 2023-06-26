package com.lenerdz.commands;

import java.sql.Connection;
import java.sql.DriverManager;
// import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;

import com.lenerdz.services.GameBuilder;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class GameEnd extends ListenerAdapter {

   @Override
   public void onMessageReceived(MessageReceivedEvent event) {
      String[] message = event.getMessage().getContentRaw().split(" ");

      if (message.length >= 3 && message[0].equals("Wordle") && message[1].equalsIgnoreCase("game") && message[2].equalsIgnoreCase("end")) {
         Dotenv dotenv = Dotenv.load();
         try (Connection conn = DriverManager.getConnection(dotenv.get("JDBC_URL"));
               Statement stmt = conn.createStatement();) {

            ResultSet currentGameInfo = stmt.executeQuery("SELECT p.WordleName, gp.PlayerID, gp.GameID, g.Name, SUM(s.SubScore) AS totalSubScore, SUM(s.SuperScore) AS totalSuperScore FROM GamesPlayers gp JOIN Games g ON gp.GameID = g.ID JOIN Scores s ON gp.PlayerID = s.PlayerID AND gp.GameID = s.GameID JOIN Players p on gp.PlayerID = p.ID WHERE g.Current = 1 GROUP BY p.WordleName, gp.PlayerID, gp.GameID ORDER BY p.WordleName;");
            int winnerID = -1;
            int maxSuperScore = -1;
            int minSubScore = -1;
            int gameID = -1;
            while(currentGameInfo.next()) {
               int testSuperScore = currentGameInfo.getInt("totalSuperScore");
               if (testSuperScore > maxSuperScore) {
                  maxSuperScore = testSuperScore;
                  winnerID = currentGameInfo.getInt("PlayerID");
                  minSubScore = currentGameInfo.getInt("totalSubScore");
               } else if (testSuperScore == maxSuperScore) {
                  int testSubScore = currentGameInfo.getInt("totalSubScore");
                  if(testSubScore < minSubScore) {
                     winnerID = currentGameInfo.getInt("PlayerID");
                  } else if (testSubScore == minSubScore) {
                     event.getChannel().sendMessage("Well this is awkward...a tie! There is no winner!").queue();
                     winnerID = -1;
                  }
               }
               gameID = currentGameInfo.getInt("GameID");
            }

            if (winnerID == -1) {
               String updateEndGameString = "UPDATE Games SET EndDate = \"" + LocalDate.now().toString() + "\" WHERE Current = 1;";
               stmt.executeUpdate(updateEndGameString);
            } else {
               String updateEndGameString = "UPDATE Games SET PlayerID = \"" + winnerID + "\", EndDate = \"" + LocalDate.now().toString() + "\" WHERE Current = 1;";
               stmt.executeUpdate(updateEndGameString);
            }

            String updateCurrString = "UPDATE Games SET Current = 0;";
            stmt.executeUpdate(updateCurrString);

            GameBuilder gameBoy = new GameBuilder();
            event.getChannel().sendMessage("Game has ended!\n\n" + gameBoy.getPreviousGame(gameID)).queue();

         } catch (SQLException e) {
            event.getChannel().sendMessage("Couldn't end the game :(").queue();
            e.printStackTrace();
         }

      }
   }

}