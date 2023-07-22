package com.lenerdz.commands;

import com.lenerdz.services.GameBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Help extends ListenerAdapter {

   @Override
   public void onMessageReceived(MessageReceivedEvent event) {
      String[] message = event.getMessage().getContentRaw().split("\\s+");

      if (message[0].equals("Wordle") && message[1].equalsIgnoreCase("help") && message.length == 2) {
         StringBuilder sb = new StringBuilder();
         sb.append("## WELCOME TO THE WORLDEXTREME!\n");
         sb.append("This helpful bot is here to keep track of all your wordle scoring competition needs.\nEach command must start with 'Wordle' - remember, the capitalization matters!\n");
         sb.append("### COMMAND LIST\n");
         sb.append("`add player` - adds a player to this guild's current game.\n");
         sb.append("`game` - displays the status of the current game in this guild.\n");
         sb.append("`game end` - ends the current game in this guild.\n");
         sb.append("`game new` - creates a new current game in this server.\n");
         sb.append("`history` - displays the ID numbers and names of all previous games in this guild.\n");
         sb.append("`score` - calculates and adds scores to the current game in this guild.");

         event.getChannel().sendMessage(sb).queue();
      }
   }
}