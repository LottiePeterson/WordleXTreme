package com.lenerdz.commands;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import com.lenerdz.services.GameBuilder;

import java.sql.ResultSet;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
//import net.dv8tion.jda.api.EmbedBuilder;

public class Game extends ListenerAdapter {

   @Override
   public void onMessageReceived(MessageReceivedEvent event) {
      String[] message = event.getMessage().getContentRaw().split(" ");

      if (message[0].equals("Wordle") && message[1].equalsIgnoreCase("game") && message.length == 2) {
         // Dotenv dotenv = Dotenv.load();
         // try (Connection conn = DriverManager.getConnection(dotenv.get("JDBC_URL"));
         //       Statement stmt = conn.createStatement();) {

         //    // EmbedBuilder embed = new EmbedBuilder();
         //    // embed.setTitle("March");
         //    // embed.addField("Players:", "boop boop", true);
         //    // event.getChannel().sendMessageEmbeds(embed.build()).queue();

         //    ResultSet gameInfo = stmt.executeQuery("SELECT p.WordleName, gp.PlayerID, gp.GameID, g.Name, SUM(s.SubScore) AS totalSubScore, SUM(s.SuperScore) AS totalSuperScore FROM GamesPlayers gp JOIN Games g ON gp.GameID = g.ID JOIN Scores s ON gp.PlayerID = s.PlayerID AND gp.GameID = s.GameID JOIN Players p on gp.PlayerID = p.ID WHERE g.Current = 1 GROUP BY p.WordleName, gp.PlayerID, gp.GameID ORDER BY p.WordleName;");
            
         //    String gameName = "";
         //    String tempResult = "";
         //    int gameID = -1;

         //    while (gameInfo.next()) {
         //       gameName = gameInfo.getString("Name");
         //       gameID = gameInfo.getInt("GameID");
         //       tempResult += "\n" + gameInfo.getString("WordleName") + ": " + gameInfo.getDouble("totalSuperScore") + " " + gameInfo.getInt("totalSubScore");
         //    }

         //    if (gameID == -1) {
         //       event.getChannel().sendMessage("No current game in this server!").queue();
         //       return;
         //    }

         //    System.out.println(gameName);
         //    String result = "=====** " + gameName + " **=====" + tempResult + "\n=====*Game ID " + gameID + "*=====";

         //    event.getChannel().sendMessage(result).queue();
         // } catch (SQLException e) {
         //    e.printStackTrace();
         // }
         GameBuilder gameBoy = new GameBuilder();
         event.getChannel().sendMessage(gameBoy.getCurrrentGame()).queue();;
         
      }
   }
}