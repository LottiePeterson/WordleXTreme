import datetime
import re
import zoneinfo

import discord
from discord.ext import commands, tasks

from backend import score_calculator
from backend.database import db
from backend.helpers import CT, ensure_guild, ensure_registered, format_standings, get_game_state

WORDLE_PATTERN = re.compile(r'Wordle ([\d,]+) ([1-6X])/6', re.IGNORECASE)


class ScoreCog(commands.Cog):
    def __init__(self, bot: commands.Bot):
        self.bot = bot
        self.midnight_tally.start()
        self.evening_reminder.start()

    def cog_unload(self):
        self.midnight_tally.cancel()
        self.evening_reminder.cancel()

    # ------------------------------------------------------------------ #
    # Message listener — parse Wordle results pasted in the results channel
    # ------------------------------------------------------------------ #

    @commands.Cog.listener()
    async def on_message(self, message: discord.Message):
        if message.author.bot or not message.guild:
            return

        guild_id = str(message.guild.id)
        guild_config = db.fetch("SELECT ChannelID FROM Guilds WHERE GuildID = %s", guild_id)
        if not guild_config or not guild_config[0].get('ChannelID'):
            return
        if str(message.channel.id) != guild_config[0]['ChannelID']:
            return

        match = WORDLE_PATTERN.search(message.content)
        if not match:
            return

        wordle_number = int(match.group(1).replace(',', ''))
        raw_guesses = match.group(2)
        guesses = int(raw_guesses) if raw_guesses.upper() != 'X' else 7

        state, game = get_game_state(guild_id)

        if state == 'REGISTRATION':
            await message.reply(
                f"**{game['Name']}** hasn't started yet — save that score for when the game begins!"
            )
            return
        if state in ('NO_GAME', 'EXPIRED'):
            return

        # ACTIVE — register the player if needed and validate they're in the game
        player_id = ensure_registered(str(message.author.id), message.author.display_name)

        in_game = db.fetch(
            "SELECT 1 FROM GamesPlayers WHERE GameID = %s AND PlayerID = %s",
            game['ID'],
            player_id,
        )
        if not in_game:
            await message.reply(
                "You're not registered for this game. "
                "You can join the next one with `/join` during the registration period!"
            )
            return

        # Reject submissions for already-tallied Wordle numbers
        already_tallied = db.fetch(
            "SELECT 1 FROM DailyScores "
            "WHERE GameID = %s AND WordleNumber = %s AND Tallied = TRUE LIMIT 1",
            game['ID'],
            wordle_number,
        )
        if already_tallied:
            await message.reply(
                f"Scores for Wordle #{wordle_number} have already been tallied. "
                "Late submissions aren't counted — submit before midnight CT next time!"
            )
            return

        # Reject duplicate submissions
        existing = db.fetch(
            "SELECT 1 FROM DailyScores WHERE GameID = %s AND PlayerID = %s AND WordleNumber = %s",
            game['ID'],
            player_id,
            wordle_number,
        )
        if existing:
            await message.reply(f"You already submitted for Wordle #{wordle_number}!")
            return

        db.execute(
            "INSERT INTO DailyScores (GameID, PlayerID, WordleNumber, Guesses) "
            "VALUES (%s, %s, %s, %s)",
            game['ID'],
            player_id,
            wordle_number,
            guesses,
        )
        await message.add_reaction("✅")

        # Auto-tally if everyone has submitted
        total_players = db.fetch(
            "SELECT COUNT(*) AS cnt FROM GamesPlayers WHERE GameID = %s", game['ID']
        )[0]['cnt']
        submitted_count = db.fetch(
            "SELECT COUNT(*) AS cnt FROM DailyScores WHERE GameID = %s AND WordleNumber = %s",
            game['ID'],
            wordle_number,
        )[0]['cnt']

        if submitted_count >= total_players:
            await self._tally(game, wordle_number, message.guild)

    # ------------------------------------------------------------------ #
    # Tally logic                                                         #
    # ------------------------------------------------------------------ #

    async def _tally(self, game: dict, wordle_number: int, guild: discord.Guild):
        """Calculate and store superscores for all players for one Wordle number.
        Non-submitters are penalised with Guesses=7 before scoring."""
        all_players = db.fetch(
            "SELECT PlayerID FROM GamesPlayers WHERE GameID = %s", game['ID']
        )
        player_ids = [r['PlayerID'] for r in all_players]

        submitted = db.fetch(
            "SELECT PlayerID, Guesses FROM DailyScores WHERE GameID = %s AND WordleNumber = %s",
            game['ID'],
            wordle_number,
        )
        submitted_map: dict[int, int] = {r['PlayerID']: r['Guesses'] for r in submitted}

        for pid in player_ids:
            if pid not in submitted_map:
                db.execute(
                    "INSERT IGNORE INTO DailyScores (GameID, PlayerID, WordleNumber, Guesses) "
                    "VALUES (%s, %s, %s, 7)",
                    game['ID'],
                    pid,
                    wordle_number,
                )
                submitted_map[pid] = 7

        ordered_ids = list(submitted_map.keys())
        superscores = score_calculator.calculate_superscores(
            [submitted_map[pid] for pid in ordered_ids]
        )

        for pid, ss in zip(ordered_ids, superscores):
            db.execute(
                "UPDATE DailyScores SET SuperScore = %s, Tallied = TRUE "
                "WHERE GameID = %s AND PlayerID = %s AND WordleNumber = %s",
                ss,
                game['ID'],
                pid,
                wordle_number,
            )

        guild_config = db.fetch("SELECT ChannelID FROM Guilds WHERE GuildID = %s", str(guild.id))
        channel_id = guild_config[0].get('ChannelID') if guild_config else None
        if channel_id:
            channel = guild.get_channel(int(channel_id))
            if channel:
                await channel.send(
                    f"Wordle #{wordle_number} scores tallied!\n"
                    f"{format_standings(game['ID'], game['Name'])}"
                )

    # ------------------------------------------------------------------ #
    # Daily background tasks (Central Time)                               #
    # ------------------------------------------------------------------ #

    @tasks.loop(time=datetime.time(hour=23, minute=0, tzinfo=zoneinfo.ZoneInfo('America/Chicago')))
    async def evening_reminder(self):
        """11 PM CT — ping players who haven't submitted for any open Wordle number."""
        now = datetime.datetime.now(zoneinfo.ZoneInfo('America/Chicago')).replace(tzinfo=None)
        active_games = db.fetch(
            "SELECT g.ID, g.Name, gu.GuildID, gu.ChannelID "
            "FROM Games g JOIN Guilds gu ON gu.ID = g.GuildID "
            "WHERE g.Current = TRUE AND g.StartDateTime <= %s AND g.EndDateTime > %s",
            now,
            now,
        )
        for game in active_games:
            if not game.get('ChannelID'):
                continue
            guild = self.bot.get_guild(int(game['GuildID']))
            if not guild:
                continue
            channel = guild.get_channel(int(game['ChannelID']))
            if not channel:
                continue

            untallied_numbers = db.fetch(
                "SELECT DISTINCT WordleNumber FROM DailyScores "
                "WHERE GameID = %s AND Tallied = FALSE",
                game['ID'],
            )
            if not untallied_numbers:
                continue

            all_players = db.fetch(
                "SELECT gp.PlayerID, p.UserID "
                "FROM GamesPlayers gp JOIN Players p ON p.ID = gp.PlayerID "
                "WHERE gp.GameID = %s",
                game['ID'],
            )

            for row in untallied_numbers:
                wn = row['WordleNumber']
                submitted_ids = {
                    r['PlayerID']
                    for r in db.fetch(
                        "SELECT PlayerID FROM DailyScores WHERE GameID = %s AND WordleNumber = %s",
                        game['ID'],
                        wn,
                    )
                }
                missing = [p for p in all_players if p['PlayerID'] not in submitted_ids]
                if not missing:
                    continue
                pings = ' '.join(f"<@{p['UserID']}>" for p in missing)
                await channel.send(
                    f"⏰ Reminder: {pings} — you haven't submitted your Wordle #{wn} score yet! "
                    "Midnight CT is coming up!"
                )

    @tasks.loop(time=datetime.time(hour=0, minute=0, tzinfo=zoneinfo.ZoneInfo('America/Chicago')))
    async def midnight_tally(self):
        """12:00 AM CT — tally any untallied Wordle numbers for all active games."""
        now = datetime.datetime.now(zoneinfo.ZoneInfo('America/Chicago')).replace(tzinfo=None)
        active_games = db.fetch(
            "SELECT g.ID, g.Name, gu.GuildID, gu.ChannelID "
            "FROM Games g JOIN Guilds gu ON gu.ID = g.GuildID "
            "WHERE g.Current = TRUE AND g.StartDateTime <= %s AND g.EndDateTime > %s",
            now,
            now,
        )
        for game in active_games:
            guild = self.bot.get_guild(int(game['GuildID']))
            if not guild:
                continue

            untallied = db.fetch(
                "SELECT DISTINCT WordleNumber FROM DailyScores "
                "WHERE GameID = %s AND Tallied = FALSE",
                game['ID'],
            )
            for row in untallied:
                await self._tally(game, row['WordleNumber'], guild)

    @evening_reminder.before_loop
    @midnight_tally.before_loop
    async def before_tasks(self):
        await self.bot.wait_until_ready()


async def setup(bot: commands.Bot):
    await bot.add_cog(ScoreCog(bot))
