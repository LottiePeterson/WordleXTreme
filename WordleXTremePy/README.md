# WordleXTremePy

Discord bot for tracking Wordle competition scores among friends. Players paste their daily NYT
Wordle results into a designated channel; the bot parses them automatically, calculates
placement-based scores, and crowns a Wordle Champion at the end of each competition.

## Prerequisites

- Python 3.12+
- A MySQL database (the bot is configured for SparkedHost but any MySQL 8+ instance works)
- A Discord bot application with the following enabled:
  - **Privileged Gateway Intents**: Message Content Intent, Server Members Intent

## Setup

### 1. Clone and create a virtual environment

```bash
cd WordleXTremePy
python3 -m venv .venv
source .venv/bin/activate
```

### 2. Install dependencies

```bash
pip install discord.py==2.4.0 mysql-connector-python python-dotenv
```

### 3. Configure environment variables

Copy the template and fill in your values:

```bash
cp .env.example .env
```

`.env` variables:

| Variable        | Description                        |
|-----------------|------------------------------------|
| `DISCORD_TOKEN` | Your Discord bot token             |
| `DB_HOST`       | MySQL host                         |
| `DB_USERNAME`   | MySQL username                     |
| `DP_PASSWORD`   | MySQL password (note: `DP_`, not `DB_`) |
| `DATABASE_NAME` | MySQL database name                |

### 4. Initialize the database

Run `schema.sql` against your database to create all tables. **This drops existing data.**

```bash
mysql -h <DB_HOST> -u <DB_USERNAME> -p <DATABASE_NAME> < schema.sql
```

### 5. Run the bot

```bash
python wordlextremepy.py
```

On startup the bot syncs slash commands globally (Discord can take up to an hour to propagate them
the first time).

## First-time Discord setup

1. `/set-channel #your-channel` — designate the channel where Wordle results will be posted.
2. `/game new` — create a competition with a name, start date/time (Central Time), and duration.
3. Players use `/join` (or are added with `/invite @member`) during the registration period.
4. Once the game starts, players paste their NYT Wordle results directly into the results channel.

## How scoring works

Scores are calculated per Wordle puzzle using a placement system:

- **SubScore**: number of guesses (1–6). A failed attempt (`X/6`) counts as 7. Missing the
  midnight CT deadline also scores 7.
- **SuperScore**: placement points. With *n* players the available points are `[n-1, n-2, …, 0]`.
  Tied players split the points for the places they occupy equally.
- **Winner**: highest cumulative SuperScore; lowest cumulative SubScore breaks ties. The winner
  receives the **Wordle Champion** Discord role (created automatically if it doesn't exist).

Scores for a given day are tallied automatically once all players have submitted, or at midnight
CT if not everyone submits. Players who miss the deadline are penalised with a score of 7.

## Slash commands

| Command | Description |
|---|---|
| `/set-channel` | Set the results channel for this server |
| `/game new` | Create a new competition |
| `/game status` | Show the current leaderboard |
| `/game end` | End the current game early and declare a winner |
| `/join` | Join the current game (registration period only) |
| `/invite @member` | Add another player to the current game |
