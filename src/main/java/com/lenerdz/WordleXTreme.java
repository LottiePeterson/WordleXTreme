package com.lenerdz;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import io.github.cdimascio.dotenv.Dotenv;

import javax.security.auth.login.LoginException;

import com.lenerdz.commands.*;

public class WordleXTreme {

    public static void main(String[] args) throws LoginException {

        Dotenv dotenv = Dotenv.load();

        JDA bot = JDABuilder.createDefault(dotenv.get("TOKEN"), GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                .setActivity(Activity.listening(" you!"))
                .build();

        
        bot.addEventListener(new CalcScore());
        bot.addEventListener(new Score());
        bot.addEventListener(new ScuffedScore());
        //bot.addEventListener(new ScoreMultiple());
        bot.addEventListener(new GameNew());
        bot.addEventListener(new Game());
        bot.addEventListener(new ScoreMany());
        bot.addEventListener(new AddPlayer());
        bot.addEventListener(new GameEnd());
        bot.addEventListener(new History());
    }
}
