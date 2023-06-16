package com.lenerdz.commands;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.sql.PreparedStatement;

import com.lenerdz.commands.utils.ScoreManyPlayers;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Score extends ListenerAdapter {

   @Override
   public void onMessageReceived(MessageReceivedEvent event) {
      String[] message = event.getMessage().getContentRaw().split(" ");
      String formatString = "Wordle Score [GameNumber]\n"
            + "[PlayerName] [numberOfGuesses]\n"
            + "[PlayerName] [numberOfGuesses]\n"
            + "...";

      if (message.length >= 2 && message[0].equals("Wordle") && message[1].equalsIgnoreCase("score")) {
         Dotenv dotenv = Dotenv.load();
         try (Connection conn = DriverManager.getConnection(dotenv.get("JDBC_URL"));
               Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);) {
            // No scores were listed at all. User wanted more information about the use of
            // the command.
            if (message.length == 2) {
               event.getChannel()
                     .sendMessage("To add a new score, try this command again with this format:\n" + formatString).queue();
               return;
            }

            // Based on the necessary input format, there should always be an odd number of
            // words/charsequences entered.
            System.out.println(message.length + " " + (message.length % 2));
            if ((message.length % 2) == 0) {
               event.getChannel()
                     .sendMessage("Oh no! You haven't entered the command properly. Try this format:\n" + formatString)
                     .queue();
               return;
            }
            HashMap<String, Integer> namesToSubScores = new HashMap<>();
            HashMap<String, Double> namesToSuperScores = new HashMap<>();

            ArrayList<String> messageNames = new ArrayList<>();
            //ArrayList<Integer> messageSubScores = new ArrayList<>();
            for (int i = 3; i < message.length; i += 2) {
               try {
                  namesToSubScores.put(message[i], Integer.parseInt(message[i + 1]));
                  namesToSuperScores.put(message[i], null);
                  messageNames.add(message[i]);
                  // messageSubScores.add(Integer.parseInt(message[i + 1]));
               } catch (NumberFormatException e) {
                  // If the second String in each line cannot be parsed as an int, there was an
                  // input issue.
                  event.getChannel()
                        .sendMessage(
                              "Oh no! You haven't entered the command properly. Try this format:\n" + formatString)
                        .queue();
                  return;
               }
            }
            messageNames.sort(Comparator.naturalOrder());
            int[] subScores = new int[namesToSubScores.size()];
            
            for (int c = 0; c < namesToSubScores.size(); c++) {
               subScores[c] = namesToSubScores.get(messageNames.get(c));
            }

            double[] placeValues = new double[namesToSubScores.size()];
            for (int i = 0; i < placeValues.length; i++) {
               placeValues[i] = namesToSubScores.size() - 1 - i;
            }
            try {
               ResultSet tablePlayers = stmt.executeQuery(
                     "SELECT gp.PlayerID, gp.GameID, p.WordleName FROM GamesPlayers gp JOIN Players p ON gp.PlayerID = p.ID JOIN Games g ON gp.GameID = g.ID WHERE g.Current = 1;");
               int resultSetSize = 0;
               while (tablePlayers.next()) {
                  String testyname = tablePlayers.getString("WordleName");
                  // System.out.print("{newlineLottie} " + messageNames.contains("\nLottie"));
                  ++resultSetSize;
                  if (!namesToSubScores.containsKey(testyname)) {
                     //System.out.println(messageNames.toString() + " {" + testyname + "}");
                     event.getChannel().sendMessage(
                           "Oh no! You entered a WordleName that does not exist in the current game! Try again with this format:\n"
                                 + formatString)
                           .queue();
                           return;
                  }
               }

               if (resultSetSize != namesToSubScores.size()) {
                  //System.out.println(tablePlayers.getFetchSize() + " " + namesToSubScores.size());
                  event.getChannel().sendMessage(
                        "Oh no! You have not entered the correct amount of players to add a new score to the game! Try again with this format:")
                        .queue();
                        return;
               }

               double[] superScore = ScoreManyPlayers.scoreVariable(subScores, placeValues);
               for(int i = 0; i < namesToSuperScores.size(); i++) {
                  namesToSuperScores.replace(messageNames.get(i), superScore[i]);
               }
               //System.out.println("In 0 " + tablePlayers.getType());
               tablePlayers.beforeFirst();

               while (tablePlayers.next()) {
                  //System.out.println("In 1");
                  String currName = tablePlayers.getString("WordleName");
                  Integer currSubScore = namesToSubScores.get(currName);
                  Double currSuperScore = namesToSuperScores.get(currName);

                  String insertString = "INSERT INTO Scores (GameID, PlayerID, WordleNum, SubScore, SuperScore) VALUES (?, ? ,?, ?, ?)";
                  PreparedStatement insertScore = conn.prepareStatement(insertString);

                  conn.setAutoCommit(false);
                  insertScore.setInt(1, tablePlayers.getInt("GameID"));
                  insertScore.setInt(2, tablePlayers.getInt("PlayerID"));
                  insertScore.setInt(3, Integer.parseInt(message[2]));
                  insertScore.setInt(4, currSubScore);
                  insertScore.setDouble(5, currSuperScore);
                  insertScore.executeUpdate();
                  conn.commit();
               }
            } catch (SQLException e) {
               event.getChannel().sendMessage("Something went wrong!! Contact Jack and Lottie :/").queue();
            }
         } catch (SQLException e) {
            e.printStackTrace();
         }
      }
   }
}
