package com.lenerdz.commands;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Scanner;
import java.lang.Integer;

import com.lenerdz.commands.utils.ScoreManyPlayers;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ScoreMany extends ListenerAdapter{
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String[] message = event.getMessage().getContentRaw().split(" ");
        String[] lines = event.getMessage().getContentRaw().split("\n");

        if (message[0].equals("Wordle") && message[1].equalsIgnoreCase("scoreMany")) {
            //testing for not long enough prompt
            if(message.length <= 2) {
                event.getChannel().sendMessage("Oh no! Not enough info! Please use the following format: \n\n" + 
                "Wordle scoreMany [int num players] \n" +
                "p1Score p2Score p3Score p4Score ... \n" +
                "Player1Name superScore subScore \n" + 
                "Player2Name superScore subScore \n" +
                "Player3Name superScore subScore \n" + 
                "...").queue();
                return;
            }

            //testing for not enough players to match given numPlayers
            int numPlayers = 0;
            try {
                String players = message[2];
                if (message[2].charAt(message[2].length() - 1) == '\n') {
                    players = message[2].substring(0, message[2].length() - 1);
                }
                numPlayers = Integer.parseInt(players);
            } catch (NumberFormatException e) {
                event.getChannel().sendMessage("Oh no! Remember the **[int num players]**! Please use the following format: \n\n" + 
                "Wordle scoreMany [int num players] \n" +
                "p1Score p2Score p3Score p4Score ... \n" +
                "Player1Name superScore subScore \n" + 
                "Player2Name superScore subScore \n" +
                "Player3Name superScore subScore \n" + 
                "...").queue();
                return;
            }
            if (numPlayers != lines.length - 2) {
                event.getChannel().sendMessage("Oh no! Make sure numPlayers is the same as the number of players listed in your prompt! Please use the following format: \n\n" + 
                "Wordle scoreMany [int num players] \n" +
                "p1Score p2Score p3Score p4Score ... \n" +
                "Player1Name superScore subScore \n" + 
                "Player2Name superScore subScore \n" +
                "Player3Name superScore subScore \n" + 
                "...").queue();
                return;
            }

            Dotenv dotenv = Dotenv.load();
            try (Connection conn = DriverManager.getConnection(dotenv.get("JDBC_URL"));
                Statement stmt = conn.createStatement();) {
                int[] subScore = new int[numPlayers];
                String newScoresLine = lines[1];
                Scanner scnr = new Scanner(newScoresLine);
                int c = 0;
                while(scnr.hasNext() && c < subScore.length) {
                    subScore[c] = Integer.parseInt(scnr.next());
                    c++;
                }
                scnr.close();

                double[] placeValues = new double[numPlayers];
                for(int i = 0; i < placeValues.length; i++) {
                    placeValues[i] = numPlayers - 1 - i;
                }
                double[] superScore = ScoreManyPlayers.scoreVariable(subScore, placeValues);

                String result = "";
                String errorMessage = "Invalid input, try the following format:\n" +
                        "Wordle scuffed p1Score p2Score p3Score p4Score \n" +
                        "p1Name superScore subScore \n" +
                        "p2Name superScore subScore \n" +
                        "p3Name superScore subScore \n" +
                        "p4Name superScore subScore \n";
                try {
                    int tracker = numPlayers + 3;
                    for (int i = 0; i < numPlayers; i++) {
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
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
   }
}
