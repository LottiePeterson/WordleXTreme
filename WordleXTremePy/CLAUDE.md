# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this project is

WordleXTremePy is a Discord bot (Python rewrite of a dormant Java bot) that tracks Wordle scoring competitions across Discord servers. Players paste their NYT Wordle results into a designated channel; the bot parses them automatically, calculates placement-based scores, and crowns a Wordle Champion at the end of each competition.

## Running the bot

```bash
source venv/bin/activate
python wordlextremepy.py
```

Requires a `.env` file with these keys:
- `DISCORD_TOKEN` — Discord bot token
- `DB_HOST`, `DB_USERNAME`, `DP_PASSWORD` (note: `DP_`, not `DB_`), `DATABASE_NAME` — MySQL credentials

For local development, point `DB_HOST` at `127.0.0.1` and keep a `.env.prod` backup of the SparkedHost credentials (both are gitignored). Initialize a local test DB with:

```bash
mysql -u root -p wordlextreme_test < schema.sql
```

## Dependencies

```bash
pip install discord.py==2.4.0 mysql-connector-python python-dotenv
```

Discord bot requires **Message Content Intent** and **Server Members Intent** enabled in the Discord Developer Portal.

## Architecture

**Entry point:** `wordlextremepy.py` — creates a `commands.Bot` (prefix unused; kept as the cog/tree host), auto-loads all `cogs/*.py`, calls `bot.tree.sync()` on ready to register slash commands globally.

**Database singleton:** `backend/database.py` exports a module-level `db` object with a 5-connection MySQL pool that connects at import time. All cogs import `from backend.database import db`.

- `db.fetch(query, *args)` — SELECT, returns list of dicts
- `db.execute(query, *args)` — INSERT/UPDATE/DELETE
- Both wrap args into a tuple for `cursor.execute` — pass values directly, not pre-wrapped.

**Shared helpers:** `backend/helpers.py` — imported by all cogs:
- `ensure_guild(guild_id)` / `ensure_registered(user_id, display_name)` — upsert and return PK
- `get_game_state(guild_id)` — returns `(state, game_row)` where state is one of `NO_GAME`, `REGISTRATION`, `ACTIVE`, `EXPIRED`
- `format_standings(game_id, game_name)` — builds the markdown leaderboard string
- `now_ct_naive()` / `to_unix(naive_ct_dt)` — datetime utilities; all DB datetimes are stored as naive Central Time

**Scoring:** `backend/score_calculator.py` — pure Python port of `ScoreManyPlayers.java`:
- `calculate_superscores(guesses: list[int]) -> list[float]` — placement points `[n-1, …, 0]`, tied players split their pool
- `get_places(guesses)` — lower guess count = better place, ties share the same place number

**Cog loading:** `load_cogs()` auto-discovers every `.py` in `cogs/`. Each file must define `async def setup(bot)`.

## Cogs

| File | Slash commands | Notes |
|---|---|---|
| `cogs/GameCog.py` | `/game status`, `/game new`, `/game end` | Also runs a 5-min background task to auto-end expired games and assign the Wordle Champion role |
| `cogs/PlayerCog.py` | `/join`, `/invite` | Only work during REGISTRATION state |
| `cogs/ScoreCog.py` | — | `on_message` listener; parses Wordle results, auto-tallies, runs 11 PM reminder and midnight tally tasks |
| `cogs/SettingsCog.py` | `/set-channel` | Configures per-guild results channel |

## Database schema

Defined in `schema.sql` (run to wipe and recreate). Key tables:

- **Guilds** — `ID`, `GuildID` (Discord snowflake), `ChannelID` (results channel snowflake)
- **Players** — `ID`, `UserID` (snowflake), `DisplayName`
- **Games** — `ID`, `GuildID` (FK), `Name`, `StartDateTime`, `EndDateTime`, `Current` (bool), `WinnerID` (FK, nullable)
- **GamesPlayers** — `(GameID, PlayerID)` composite PK
- **DailyScores** — `GameID`, `PlayerID`, `WordleNumber`, `Guesses` (1–6; 7 = fail or no-submit), `SuperScore`, `Tallied` (bool)

## Game state model

State is derived at runtime — no status column in the DB:

| State | Condition | Join/invite? | Submit scores? |
|---|---|---|---|
| `REGISTRATION` | `Current=1`, `StartDateTime > now` | ✅ | ❌ |
| `ACTIVE` | `Current=1`, `StartDateTime ≤ now ≤ EndDateTime` | ❌ | ✅ |
| `EXPIRED` | `Current=1`, `EndDateTime < now` | ❌ | ❌ |
| `NO_GAME` | No `Current=1` row | ❌ | ❌ |

## Key conventions

- All datetimes in the DB are naive Central Time (`America/Chicago`). Use `now_ct_naive()` for comparisons and `to_unix()` to convert to Unix timestamps for Discord `<t:...>` formatting.
- Guild IDs and user IDs are stored as `VARCHAR(20)` strings (Discord snowflakes).
- `GuildID` in `Guilds` is the Discord string snowflake; `GuildID` in `Games` is the integer FK — don't confuse them in queries.
- The bot token controls a real Discord bot already present in several servers. Running locally brings that bot online and syncs slash commands globally. Use `bot.tree.sync(guild=discord.Object(id=...))` during development to limit command sync to a test server.
- `PlayerCog.py` at the repo root is a leftover stub — it is not loaded by the bot. The active version is `cogs/PlayerCog.py`.
