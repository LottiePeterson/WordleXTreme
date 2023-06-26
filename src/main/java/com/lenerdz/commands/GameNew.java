package com.lenerdz.commands;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
//import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;

import com.lenerdz.services.GameBuilder;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
//import net.dv8tion.jda.api.EmbedBuilder;

public class GameNew extends ListenerAdapter {

   @Override
   public void onMessageReceived(MessageReceivedEvent event) {
      String[] message = event.getMessage().getContentRaw().split(" ");

      if (message.length >= 3 && message[0].equals("Wordle") && message[1].equalsIgnoreCase("game") && message[2].equalsIgnoreCase("new")) {
         if (message.length <= 3) {
            event.getChannel()
                  .sendMessage(
                        "Syntax error! Remember to name your new game with this syntax: \nWordle game new [name]")
                  .queue();
         } else {
            Dotenv dotenv = Dotenv.load();
            try (Connection conn = DriverManager.getConnection(dotenv.get("JDBC_URL"));
                  Statement stmt = conn.createStatement();) {
               
               // set all other games Current column to 0 so they are not the current games
               // String updateCurrString = "UPDATE Games SET Current = 0;";
               // stmt.executeUpdate(updateCurrString);
               ResultSet currentGames = stmt.executeQuery("SELECT ID FROM Games WHERE Current = 1;");
               if(currentGames.next()) {
                  event.getChannel().sendMessage("There is already a game active in this server! Try ending that game and then starting a new one.").queue();
                  return;
               }
               
               String insertString = "INSERT INTO Games (Name, StartDate, EndDate, Current, PlayerID) VALUES (?, ?, NULL, 1, NULL);";
               PreparedStatement insertNewGame = conn.prepareStatement(insertString);

               conn.setAutoCommit(false);
               insertNewGame.setString(1, message[3]);
               insertNewGame.setString(2, LocalDate.now().toString());
               insertNewGame.executeUpdate();
               conn.commit();
                              
               // String gameString = "=====** " + message[3] + " **=====\n" + "No Players\n";
               // for(int i = 0; i < message[3].length() + 12; i++) {
               //    gameString += "=";
               // }
               GameBuilder gameBoy = new GameBuilder();
               event.getChannel().sendMessage(gameBoy.getCurrrentGame()).queue();

               // EmbedBuilder embed = new EmbedBuilder();
               // embed.setTitle("New Game");
               // embed.setDescription("Add some players!");
               // embed.setFooter("ID: ");
               // //embed.add("Player 1:", "Scores here!", true);
               // event.getChannel().sendMessage("You've made a new
               // game!").setEmbeds(embed.build()).queue();
            } catch (SQLException e) {
               event.getChannel().sendMessage("Couldn't make a new game :(").queue();
               e.printStackTrace();
            }
         }

      }
   }

}