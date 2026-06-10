-- WordleXTremePy database schema
-- Run this against the live database to fully recreate all tables.
-- WARNING: drops all existing data.

DROP TABLE IF EXISTS DailyScores;
DROP TABLE IF EXISTS GamesPlayers;
DROP TABLE IF EXISTS Games;
DROP TABLE IF EXISTS Players;
DROP TABLE IF EXISTS Guilds;

CREATE TABLE Guilds (
    ID        INT AUTO_INCREMENT PRIMARY KEY,
    GuildID   VARCHAR(20) NOT NULL UNIQUE,
    ChannelID VARCHAR(20)
);

CREATE TABLE Players (
    ID          INT AUTO_INCREMENT PRIMARY KEY,
    UserID      VARCHAR(20) NOT NULL UNIQUE,
    DisplayName VARCHAR(100)
);

CREATE TABLE Games (
    ID            INT AUTO_INCREMENT PRIMARY KEY,
    GuildID       INT NOT NULL,
    Name          VARCHAR(100) NOT NULL,
    StartDateTime DATETIME NOT NULL,
    EndDateTime   DATETIME NOT NULL,
    Current       BOOLEAN NOT NULL DEFAULT TRUE,
    WinnerID      INT,
    FOREIGN KEY (GuildID)  REFERENCES Guilds(ID),
    FOREIGN KEY (WinnerID) REFERENCES Players(ID)
);

CREATE TABLE GamesPlayers (
    GameID   INT NOT NULL,
    PlayerID INT NOT NULL,
    PRIMARY KEY (GameID, PlayerID),
    FOREIGN KEY (GameID)   REFERENCES Games(ID),
    FOREIGN KEY (PlayerID) REFERENCES Players(ID)
);

-- Guesses: 1-6 for a solve, 7 for a failed attempt (X/6) or a missed midnight deadline.
-- Tallied: set to TRUE once superscores have been calculated for this wordle number.
CREATE TABLE DailyScores (
    ID           INT AUTO_INCREMENT PRIMARY KEY,
    GameID       INT NOT NULL,
    PlayerID     INT NOT NULL,
    WordleNumber INT NOT NULL,
    Guesses      INT NOT NULL,
    SuperScore   FLOAT NOT NULL DEFAULT 0.0,
    Tallied      BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (GameID, PlayerID, WordleNumber),
    FOREIGN KEY (GameID)   REFERENCES Games(ID),
    FOREIGN KEY (PlayerID) REFERENCES Players(ID)
);
