package com.lenerdz.commands;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

import com.lenerdz.services.Score4Players;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
//hard coded for 4 people
public class ScoreMultiple extends ListenerAdapter {

   @Override
   public void onMessageReceived(MessageReceivedEvent event) {
      String[] message = event.getMessage().getContentRaw().split("\n");
      Scanner scnr;
      if (message[0].equals("Wordle multiple ")) {
         Dotenv dotenv = Dotenv.load();
         try (Connection conn = DriverManager.getConnection(dotenv.get("JDBC_URL"));
               Statement stmt = conn.createStatement();) {


            String result = "";
            String errorMessage = "Invalid input, try the following format:\n" +
                "Wordle multiple \n" +
                "p1Name score1 score2 . . . score n \n" +
                "p2Name score1 score2 . . . score n \n" +
                "p3Name score1 score2 . . . score n \n" +
                "p4Name score1 score2 . . . score n \n";
                try{
                  String[] names = new String[4];
                  int[][] dayScores = new int[4][];
                  int[] subScores = {0, 0, 0, 0};
                  double[] superScores = {0, 0, 0, 0};
                  for(int  i = 1; i < 5; i++){
                     scnr = new Scanner(message[i]);
                     names[i - 1] = scnr.next();
                     int strLen = (message[i].length() - names[i - 1].length()) / 2;
                     dayScores[i - 1] = new int[strLen];
                     int index = 0;
                     while(scnr.hasNextInt()){
                        int score = scnr.nextInt();
                        dayScores[i - 1][index] = score;
                        subScores[i - 1] += score;
                        index++;
                     } 
                  }
                  for(int i = 0; i < dayScores[0].length; i++){
                     int[] todaysSubScores = {dayScores[0][i], dayScores[1][i], dayScores[2][i], dayScores[3][i]};
                     double[] todaysSuperScores = Score4Players.score4Players(todaysSubScores);
                     for(int j = 0; j < 4; j++){
                        superScores[i] += todaysSuperScores[i];
                     }
                  }
                  for(int i = 0; i < 4; i++){
                     result += names[i] + " " + superScores[i] + " " + subScores[i] + " \n";
                  }
                event.getChannel().sendMessage(result).queue();
            } catch(NumberFormatException e){
                event.getChannel().sendMessage(errorMessage).queue();
            }  catch(ArrayIndexOutOfBoundsException e){
               event.getChannel().sendMessage(errorMessage).queue();
            }
            
            // Execute a query
         } catch (SQLException e) {
            e.printStackTrace();
         }
      }
   }

}
