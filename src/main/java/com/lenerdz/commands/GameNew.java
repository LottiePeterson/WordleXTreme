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
               String guildStringID = event.getGuild().getId();
               ResultSet guilds = stmt.executeQuery("SELECT ID FROM Guilds WHERE GuildStringID = \"" + guildStringID + "\"");
               int guildTableID = -1;
               while(guilds.next()) {
                  guildTableID = guilds.getInt("ID"); 
               }
               if(guildTableID == -1) {
                  String guildInsertString = "INSERT INTO Guilds (GuildStringID) VALUES (?);";
                  PreparedStatement insertNewGuild = conn.prepareStatement(guildInsertString, Statement.RETURN_GENERATED_KEYS);

                  conn.setAutoCommit(false);
                  insertNewGuild.setString(1, guildStringID);
                  insertNewGuild.executeUpdate();
                  conn.commit();
                  conn.setAutoCommit(true);

                  ResultSet guildIDSet = insertNewGuild.getGeneratedKeys();
                  if(guildIDSet.next()) {
                     guildTableID = guildIDSet.getInt(1);
                  }
               }
               ResultSet currentGames = stmt.executeQuery("SELECT g.ID FROM Games g JOIN Guilds glds ON g.GuildID = glds.ID WHERE g.Current = 1 AND glds.GuildStringID = \"" + guildTableID + "\";");
               if(currentGames.next()) {
                  event.getChannel().sendMessage("There is already a game active in this server! Try ending that game and then starting a new one.").queue();
                  return;
               }
               
               String insertString = "INSERT INTO Games (Name, StartDate, EndDate, Current, PlayerID, GuildID) VALUES (?, ?, NULL, 1, NULL, ?);";
               PreparedStatement insertNewGame = conn.prepareStatement(insertString, Statement.RETURN_GENERATED_KEYS);

               conn.setAutoCommit(false);
               insertNewGame.setString(1, message[3]);
               insertNewGame.setString(2, LocalDate.now().toString());
               insertNewGame.setInt(3, guildTableID);
               insertNewGame.executeUpdate();
               conn.commit();
               conn.setAutoCommit(true);

               ResultSet generatedKeys = insertNewGame.getGeneratedKeys();
               int generatedKey = -1;
               if(generatedKeys.next()) {
                  generatedKey = generatedKeys.getInt(1);
               }
               System.out.println(generatedKey + " ");
               if(generatedKey == -1) {
                  event.getChannel().sendMessage("Bro something went wrong you should check that. Yes, you.").queue();
                  return;
               }
                                             
               GameBuilder gameBoy = new GameBuilder(event.getGuild().getId());
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