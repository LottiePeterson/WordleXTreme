package com.lenerdz.commands;

import java.lang.reflect.Member;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

import javax.management.QueryEval;

import java.sql.ResultSet;
import java.util.*;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.entities.User;
//import net.dv8tion.jda.api.EmbedBuilder;
//import scala.collection.immutable.List;

public class AddPlayer extends ListenerAdapter {

   @Override
   public void onMessageReceived(MessageReceivedEvent event) {
      String[] message = event.getMessage().getContentRaw().split(" ");

      if (message.length >= 3 && message[0].equals("Wordle") && message[1].equalsIgnoreCase("add") && message[2].equalsIgnoreCase("player")) {
         Dotenv dotenv = Dotenv.load();
         try (Connection conn = DriverManager.getConnection(dotenv.get("JDBC_URL"));
               Statement stmt = conn.createStatement();) {
            
            if(message.length < 5) {
               event.getChannel().sendMessage("Oh no! Not enough info to add a new player! Remember, names must not contain spaces or the @ character. Try this format: \nWordle add player [DisplayName] [@user]").queue();
            }

            List<User> users = event.getMessage().getMentions().getUsers();
            if (users.isEmpty()) {
               event.getChannel().sendMessage("Oh no! You didn't mention anyone! Try this format: \nWordle add player [DisplayName] [@user]").queue();
            }
            if(message[3].contains("@")) {
               event.getChannel().sendMessage("Oh no! You didn't give your player a dispaly name or the display name contained a @ symbol. Remember, names must not contain spaces or the @ character. Try this format: \nWordle add player [DisplayName] [@user]").queue();
            }

            // Get the playerID from the Players table
            int playerID = -1;
            ResultSet userInfo = stmt.executeQuery("SELECT ID FROM Players WHERE UserID = " + users.get(0).getId());
            while(userInfo.next()) {
               playerID = userInfo.getInt("ID");
            }

            // If the playerID is still the default -1, then the player did not exist in the table yet
            if (playerID == -1) {
               //Add the player to the table
               String insertString = "INSERT INTO Players (UserID, Name, WordleName) VALUES (?, ? ,?)";
               PreparedStatement insertPlayer = conn.prepareStatement(insertString);

               conn.setAutoCommit(false);
               insertPlayer.setString(1, users.get(0).getId());
               insertPlayer.setString(2, users.get(0).getAsTag());
               insertPlayer.setString(3, message[3]);
               insertPlayer.executeUpdate();
               conn.commit();

               conn.setAutoCommit(true);
               // Execute the playerID query again to properly fill the playerID
               userInfo = stmt.executeQuery("SELECT ID FROM Players WHERE UserID = " + users.get(0).getId());
               while(userInfo.next()) {
                  playerID = userInfo.getInt("ID");
               }
            }

            // Get the gameID from the Game table for the current game
            int gameID = -1;
            ResultSet gameInfo = stmt.executeQuery("SELECT ID FROM Games WHERE Current = 1");
            while(gameInfo.next()) {
               gameID = gameInfo.getInt("ID");
            }

            // Link the player to the current game via the GamesPlayers table
            String gamesPlayersInsertString = "INSERT INTO GamesPlayers (GameID, PlayerID) VALUES (" + gameID + ", " + playerID + ")";
            stmt.executeUpdate(gamesPlayersInsertString);
            System.out.println("Inserted into gamesPlayers!");

            // Add in a base score of (0, 0) for this player
            String baseScoreInsertString = "INSERT INTO Scores (GameID, PlayerID, WordleNum, SubScore, SuperScore) VALUES (" + gameID + ", " + playerID + ", -1, 0, 0)";
            stmt.executeUpdate(baseScoreInsertString);
            System.out.println("Inserted into scores");

            event.getChannel().sendMessage("Player added!").queue();
         } catch (SQLException e) {
            e.printStackTrace();
         }
         
      }
   }
}