package com.lenerdz.commands;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import com.lenerdz.services.Score4Players;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

//hard coded for 4 people
public class ScuffedScore extends ListenerAdapter {

   @Override
   public void onMessageReceived(MessageReceivedEvent event) {
      String[] message = event.getMessage().getContentRaw().split(" ");

      if (message[0].equals("Wordle") && message[1].equalsIgnoreCase("scuffed")) {
         Dotenv dotenv = Dotenv.load();
         try (Connection conn = DriverManager.getConnection(dotenv.get("JDBC_URL"));
               Statement stmt = conn.createStatement();) {
            int[] subScore = new int[4];
            for (int i = 2; i < 6; i++) {
               subScore[i - 2] = Integer.parseInt(message[i]);
            }
            double[] superScore = Score4Players.score4Players(subScore);

            String result = "";
            String errorMessage = "Invalid input, try the following format:\n" +
                  "Wordle scuffed p1Score p2Score p3Score p4Score \n" +
                  "p1Name superScore subScore \n" +
                  "p2Name superScore subScore \n" +
                  "p3Name superScore subScore \n" +
                  "p4Name superScore subScore \n";
            try {
               int tracker = 6;
               for (int i = 0; i < 4; i++) {
                  String name = message[tracker];
                  if (name.charAt(0) != '\n') {
                     name = "\n" + name;
                  }
                  double currSuperScore = superScore[i] + Double.parseDouble(message[tracker + 1]);
                  int currSubScore = subScore[i] + Integer.parseInt(message[tracker + 2]);
                  result += name + " " + currSuperScore + " " + currSubScore + " ";
                  tracker += 3;
               }
               event.getChannel().sendMessage(result).queue();
            } catch (NumberFormatException e) {
               event.getChannel().sendMessage(errorMessage).queue();
            } catch (ArrayIndexOutOfBoundsException e) {
               event.getChannel().sendMessage(errorMessage).queue();
            } catch (Exception e) {
               event.getChannel().sendMessage(e.toString()).queue();
            }

            // Execute a query
         } catch (SQLException e) {
            e.printStackTrace();
         }
      }
   }

}
