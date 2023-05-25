package com.lenerdz.commands;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.EmbedBuilder;

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
               
               String updateCurrString = "UPDATE Games SET Current = 0;";
               stmt.executeUpdate(updateCurrString);
               
               System.out.println(LocalDate.now().toString());
               String makeGameSql = "INSERT INTO Games (Name, StartDate, EndDate, Current, PlayerID) VALUES (\"" + message[3] + "\", \""+ LocalDate.now().toString() +"\", NULL, 1, NULL);";
               stmt.executeUpdate(makeGameSql);

               event.getChannel().sendMessage("Hey hey hey she worked I think!").queue();
               // // Execute a query
               // String sql = "INSERT INTO `Games` (`ID`, `Name`, `StartDate`, `EndDate`,
               // `Current`, `PlayerID`) VALUES (NULL, 'New Game', '2023-03-17', NULL, '1',
               // NULL)";
               // //String sql = "INSERT INTO `Scores` (`GameID`, `PlayerID`, `WordleNum`,
               // `CumulativeScore`, `CompetitiveScore`) VALUES (1, 1, 674, 2, NULL)";
               // stmt.executeUpdate(sql);
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