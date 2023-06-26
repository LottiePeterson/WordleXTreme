package com.lenerdz.commands;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import com.lenerdz.services.GameBuilder;
import com.mysql.cj.protocol.a.BooleanValueEncoder;

import java.sql.ResultSet;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
//import net.dv8tion.jda.api.EmbedBuilder;

public class History extends ListenerAdapter {

   @Override
   public void onMessageReceived(MessageReceivedEvent event) {
      String[] message = event.getMessage().getContentRaw().split(" ");

      if (message.length >= 2 && message[0].equals("Wordle") && message[1].equalsIgnoreCase("history")) {
         Dotenv dotenv = Dotenv.load();
         try (Connection conn = DriverManager.getConnection(dotenv.get("JDBC_URL"));
               Statement stmt = conn.createStatement();) {

            // EmbedBuilder embed = new EmbedBuilder();
            // embed.setTitle("March");
            // embed.addField("Players:", "boop boop", true);
            // event.getChannel().sendMessageEmbeds(embed.build()).queue();
            if (message.length == 2) {
               ResultSet gameHistory = stmt.executeQuery("SELECT Name, ID, StartDate, EndDate, PlayerID AS Winner FROM Games ORDER BY StartDate;");
            
               String tempResult = "";
               int gameID = -1;
               while (gameHistory.next()) {
                  //gameName = gameHistory.getString("Name");
                  gameID = gameHistory.getInt("ID");
                  tempResult += "\n" + gameID + " | " + gameHistory.getString("Name");
               }

               if (gameID == -1) {
                  tempResult = "No games are in your history!";
                  return;
               }
               String result = "=====**Game History**=====" + tempResult + "\n=================";
               event.getChannel().sendMessage(result).queue();
            } else {
               try {
                  int inputGameId = Integer.parseInt(message[2]);
                  //ResultSet previousGame = stmt.executeQuery("SELECT Name, ID, StartDate, EndDate, PlayerID AS Winner FROM Games WHERE ID = " + inputGameId + ";");

                  GameBuilder gameBoy = new GameBuilder();
                  // String previousGameString = "";
                  // while(previousGame.next()) {
                  //    previousGameString = gameBoy.getPreviousGame(inputGameId);
                  // }
                  event.getChannel().sendMessage(gameBoy.getPreviousGame(inputGameId)).queue();

               } catch (NumberFormatException e) {
                  event.getChannel().sendMessage("Oh no! You did not put in a number properly. Try again with this format: \nWordle History [GameID]").queue();
               }
            }         
         } catch (SQLException e) {
            e.printStackTrace();
         }        
      }
   }
}