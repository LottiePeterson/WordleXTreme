package com.lenerdz;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import io.github.cdimascio.dotenv.Dotenv;

import javax.security.auth.login.LoginException;

public class WordleXTreme {

    public static void main(String[] args) throws LoginException {
        
        Dotenv dotenv = Dotenv.load();
        dotenv.get("MY_ENV_VAR1");

        JDA bot = JDABuilder.createDefault(dotenv.get("TOKEN"))
                .setActivity(Activity.playing("testy test time"))
                .build();

    }
}