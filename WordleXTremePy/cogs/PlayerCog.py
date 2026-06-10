import discord
from discord import app_commands
from discord.ext import commands

from backend.database import db
from backend.helpers import ensure_guild, ensure_registered, get_game_state, to_unix


class PlayerCog(commands.Cog):
    def __init__(self, bot: commands.Bot):
        self.bot = bot

    @app_commands.command(name='join', description='Join the current game during the registration period')
    async def join(self, interaction: discord.Interaction):
        guild_id = str(interaction.guild_id)
        ensure_guild(guild_id)
        state, game = get_game_state(guild_id)

        if state == 'NO_GAME':
            await interaction.response.send_message(
                "There's no active game right now. Someone can start one with `/game new`.",
                ephemeral=True,
            )
            return
        if state == 'ACTIVE':
            await interaction.response.send_message(
                f"**{game['Name']}** has already started! "
                "Hang tight — you can join the next game once this play period ends.",
                ephemeral=True,
            )
            return
        if state == 'EXPIRED':
            await interaction.response.send_message(
                "The game period has ended. Someone can start a new one with `/game new`.",
                ephemeral=True,
            )
            return

        # REGISTRATION state
        player_id = ensure_registered(str(interaction.user.id), interaction.user.display_name)

        already_in = db.fetch(
            "SELECT 1 FROM GamesPlayers WHERE GameID = %s AND PlayerID = %s",
            game['ID'],
            player_id,
        )
        if already_in:
            await interaction.response.send_message(
                f"You're already registered for **{game['Name']}**!",
                ephemeral=True,
            )
            return

        db.execute(
            "INSERT INTO GamesPlayers (GameID, PlayerID) VALUES (%s, %s)",
            game['ID'],
            player_id,
        )
        await interaction.response.send_message(
            f"You've joined **{game['Name']}**! "
            f"The game starts <t:{to_unix(game['StartDateTime'])}:R>. "
            "Post your Wordle results in the results channel once it begins."
        )

    @app_commands.command(name='invite', description='Invite another player to join the current game')
    @app_commands.describe(member='The server member to invite')
    async def invite(self, interaction: discord.Interaction, member: discord.Member):
        guild_id = str(interaction.guild_id)
        ensure_guild(guild_id)
        state, game = get_game_state(guild_id)

        if state == 'NO_GAME':
            await interaction.response.send_message(
                "There's no active game to invite someone to.",
                ephemeral=True,
            )
            return
        if state in ('ACTIVE', 'EXPIRED'):
            await interaction.response.send_message(
                "The registration period is over — players can only join before the game starts.",
                ephemeral=True,
            )
            return

        if member.bot:
            await interaction.response.send_message("Bots can't play Wordle!", ephemeral=True)
            return

        player_id = ensure_registered(str(member.id), member.display_name)

        already_in = db.fetch(
            "SELECT 1 FROM GamesPlayers WHERE GameID = %s AND PlayerID = %s",
            game['ID'],
            player_id,
        )
        if already_in:
            await interaction.response.send_message(
                f"{member.display_name} is already registered for **{game['Name']}**!",
                ephemeral=True,
            )
            return

        db.execute(
            "INSERT INTO GamesPlayers (GameID, PlayerID) VALUES (%s, %s)",
            game['ID'],
            player_id,
        )
        await interaction.response.send_message(
            f"{member.mention} has been added to **{game['Name']}**! "
            f"The game starts <t:{to_unix(game['StartDateTime'])}:R>."
        )


async def setup(bot: commands.Bot):
    await bot.add_cog(PlayerCog(bot))
