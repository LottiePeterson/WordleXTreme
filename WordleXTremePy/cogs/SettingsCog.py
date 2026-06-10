import discord
from discord import app_commands
from discord.ext import commands

from backend.database import db
from backend.helpers import ensure_guild


class SettingsCog(commands.Cog):
    def __init__(self, bot: commands.Bot):
        self.bot = bot

    @app_commands.command(
        name='set-channel',
        description='Set the channel where Wordle results are posted and scores are tracked',
    )
    @app_commands.describe(channel='The text channel to watch for Wordle results')
    async def set_channel(self, interaction: discord.Interaction, channel: discord.TextChannel):
        ensure_guild(str(interaction.guild_id))
        db.execute(
            "UPDATE Guilds SET ChannelID = %s WHERE GuildID = %s",
            str(channel.id),
            str(interaction.guild_id),
        )
        await interaction.response.send_message(
            f"Results channel set to {channel.mention}. "
            "Paste your Wordle results there and the bot will track them automatically!",
            ephemeral=True,
        )


async def setup(bot: commands.Bot):
    await bot.add_cog(SettingsCog(bot))
