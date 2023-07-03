package com.lenerdz.commands;

import com.lenerdz.services.GameBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Game extends ListenerAdapter {

   @Override
   public void onMessageReceived(MessageReceivedEvent event) {
      String[] message = event.getMessage().getContentRaw().split(" ");

      if (message[0].equals("Wordle") && message[1].equalsIgnoreCase("game") && message.length == 2) {
         GameBuilder gameBoy = new GameBuilder(event.getGuild().getId());
         event.getChannel().sendMessage(gameBoy.getCurrrentGame()).queue();
      }
   }
}