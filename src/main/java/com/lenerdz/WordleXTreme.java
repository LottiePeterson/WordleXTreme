package com.lenerdz;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;

import javax.security.auth.login.LoginException;

public class WordleXTreme {

    public static void main(String[] args) throws LoginException {

        JDA bot = JDABuilder.createDefault("")
                .setActivity(Activity.playing("with your mom"))
                .build();

    }
}