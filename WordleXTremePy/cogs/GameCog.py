import datetime
import zoneinfo

import discord
from discord import app_commands
from discord.ext import commands, tasks

from backend.database import db
from backend.helpers import (
    CT,
    ensure_guild,
    format_standings,
    get_game_state,
    now_ct_naive,
    to_unix,
)


class GameCog(commands.Cog):
    def __init__(self, bot: commands.Bot):
        self.bot = bot
        self.check_expired_games.start()

    def cog_unload(self):
        self.check_expired_games.cancel()

    # ------------------------------------------------------------------ #
    # Slash commands                                                       #
    # ------------------------------------------------------------------ #

    game_group = app_commands.Group(name='game', description='Wordle competition commands')

    @game_group.command(name='status', description='Show the current game standings')
    async def game_status(self, interaction: discord.Interaction):
        state, game = get_game_state(str(interaction.guild_id))
        if game is None:
            await interaction.response.send_message(
                "There's no active game in this server right now.", ephemeral=True
            )
            return
        label = '(registration open)' if state == 'REGISTRATION' else ''
        await interaction.response.send_message(
            f"{format_standings(game['ID'], game['Name'])} {label}".strip()
        )

    @game_group.command(name='new', description='Create a new Wordle competition')
    @app_commands.describe(
        name='A name for this competition',
        start_date='Start date in Central Time — format: YYYY-MM-DD',
        start_time='Start time in Central Time (24h) — format: HH:MM',
        duration_days='How many days the competition runs',
    )
    async def game_new(
        self,
        interaction: discord.Interaction,
        name: str,
        start_date: str,
        start_time: str,
        duration_days: int,
    ):
        guild_id = str(interaction.guild_id)
        ensure_guild(guild_id)
        state, existing = get_game_state(guild_id)

        if state in ('REGISTRATION', 'ACTIVE'):
            await interaction.response.send_message(
                f"There's already an active game: **{existing['Name']}**. "
                "End it first with `/game end`.",
                ephemeral=True,
            )
            return

        try:
            start_dt = datetime.datetime.strptime(f"{start_date} {start_time}", "%Y-%m-%d %H:%M")
        except ValueError:
            await interaction.response.send_message(
                "Invalid format. Use `YYYY-MM-DD` for date and `HH:MM` (24h) for time.",
                ephemeral=True,
            )
            return

        if start_dt <= now_ct_naive():
            await interaction.response.send_message(
                "Start time must be in the future (all times are Central).", ephemeral=True
            )
            return

        if duration_days < 1:
            await interaction.response.send_message(
                "Duration must be at least 1 day.", ephemeral=True
            )
            return

        end_dt = start_dt + datetime.timedelta(days=duration_days)
        guild_db_id = db.fetch("SELECT ID FROM Guilds WHERE GuildID = %s", guild_id)[0]['ID']

        # Clear any expired game before creating the new one
        if state == 'EXPIRED':
            db.execute(
                "UPDATE Games SET Current = FALSE WHERE GuildID = %s AND Current = TRUE",
                guild_db_id,
            )

        db.execute(
            "INSERT INTO Games (GuildID, Name, StartDateTime, EndDateTime, Current) "
            "VALUES (%s, %s, %s, %s, TRUE)",
            guild_db_id,
            name,
            start_dt,
            end_dt,
        )
        await interaction.response.send_message(
            f"**{name}** has been created!\n"
            f"Registration is open — players can join with `/join` or be added with `/invite`.\n"
            f"Game starts: <t:{to_unix(start_dt)}:F>\n"
            f"Game ends:   <t:{to_unix(end_dt)}:F>"
        )

    @game_group.command(name='end', description='End the current game and declare a winner')
    async def game_end(self, interaction: discord.Interaction):
        state, game = get_game_state(str(interaction.guild_id))
        if game is None:
            await interaction.response.send_message("No active game to end.", ephemeral=True)
            return
        await interaction.response.defer()
        await self._end_game(game, interaction.guild)
        await interaction.followup.send("Game ended!")

    # ------------------------------------------------------------------ #
    # Game-end logic (shared by /game end and the expiry background task) #
    # ------------------------------------------------------------------ #

    async def _end_game(self, game: dict, guild: discord.Guild):
        rows = db.fetch(
            "SELECT PlayerID, "
            "COALESCE(SUM(SuperScore), 0.0) AS ss, "
            "COALESCE(SUM(Guesses), 0) AS sub "
            "FROM DailyScores WHERE GameID = %s AND Tallied = TRUE "
            "GROUP BY PlayerID ORDER BY ss DESC, sub ASC",
            game['ID'],
        )

        winner_db_id = None
        winner_player_ids: list[int] = []
        announce = ""

        if not rows:
            announce = f"**{game['Name']}** has ended with no scores recorded. No winner!"
        else:
            best = rows[0]
            tied_rows = [
                r for r in rows
                if float(r['ss']) == float(best['ss']) and int(r['sub']) == int(best['sub'])
            ]
            winner_player_ids = [r['PlayerID'] for r in tied_rows]

            if len(tied_rows) == 1:
                winner_db_id = best['PlayerID']
                name_rows = db.fetch("SELECT DisplayName FROM Players WHERE ID = %s", winner_db_id)
                winner_name = name_rows[0]['DisplayName'] if name_rows else "Unknown"
                announce = (
                    f"**{game['Name']}** is over! "
                    f"👑 **{winner_name}** is the Wordle Champion with "
                    f"{float(best['ss']):.1f} pts and {int(best['sub'])} total guesses!"
                )
            else:
                placeholders = ', '.join(['%s'] * len(winner_player_ids))
                name_rows = db.fetch(
                    f"SELECT DisplayName FROM Players WHERE ID IN ({placeholders})",
                    *winner_player_ids,
                )
                names = ', '.join(r['DisplayName'] for r in name_rows)
                announce = (
                    f"**{game['Name']}** has ended in a **tie**! "
                    f"🤝 {names} share the Wordle Champion title!"
                )

        db.execute(
            "UPDATE Games SET Current = FALSE, WinnerID = %s WHERE ID = %s",
            winner_db_id,
            game['ID'],
        )

        if winner_player_ids:
            await self._assign_champion_role(guild, winner_player_ids)

        guild_config = db.fetch("SELECT ChannelID FROM Guilds WHERE GuildID = %s", str(guild.id))
        channel_id = guild_config[0].get('ChannelID') if guild_config else None
        if channel_id:
            channel = guild.get_channel(int(channel_id))
            if channel:
                await channel.send(announce)
                if rows:
                    await channel.send(format_standings(game['ID'], game['Name']))

    async def _assign_champion_role(self, guild: discord.Guild, winner_player_ids: list[int]):
        role = discord.utils.get(guild.roles, name='Wordle Champion')
        if role is None:
            role = await guild.create_role(
                name='Wordle Champion',
                color=discord.Color.gold(),
                reason='Created by WordleXTreme',
            )

        for member in guild.members:
            if role in member.roles:
                await member.remove_roles(role, reason='New Wordle Champion being crowned')

        placeholders = ', '.join(['%s'] * len(winner_player_ids))
        user_rows = db.fetch(
            f"SELECT UserID FROM Players WHERE ID IN ({placeholders})",
            *winner_player_ids,
        )
        for row in user_rows:
            member = guild.get_member(int(row['UserID']))
            if member:
                await member.add_roles(role, reason='Wordle Champion!')

    # ------------------------------------------------------------------ #
    # Background task: auto-end expired games every 5 minutes            #
    # ------------------------------------------------------------------ #

    @tasks.loop(minutes=5)
    async def check_expired_games(self):
        now = now_ct_naive()
        expired = db.fetch(
            "SELECT g.ID, g.Name, g.StartDateTime, g.EndDateTime, g.WinnerID, "
            "gu.GuildID AS guild_discord_id "
            "FROM Games g "
            "JOIN Guilds gu ON gu.ID = g.GuildID "
            "WHERE g.Current = TRUE AND g.EndDateTime < %s",
            now,
        )
        for game in expired:
            guild = self.bot.get_guild(int(game['guild_discord_id']))
            if guild:
                await self._end_game(game, guild)

    @check_expired_games.before_loop
    async def before_check(self):
        await self.bot.wait_until_ready()


async def setup(bot: commands.Bot):
    await bot.add_cog(GameCog(bot))
