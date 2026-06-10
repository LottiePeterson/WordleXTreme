import asyncio
import os

import discord
from discord.ext import commands
from dotenv import load_dotenv

from backend.database import db

load_dotenv()

intents = discord.Intents.default()
intents.message_content = True
intents.members = True
bot = commands.Bot(command_prefix='!', intents=intents)


@bot.event
async def on_ready():
    await bot.tree.sync()
    print(f'WordleXTreme ready as {bot.user}')


async def load_cogs():
    cog_directory = os.path.join(os.path.dirname(__file__), 'cogs')
    for filename in os.listdir(cog_directory):
        if filename.endswith('.py'):
            await bot.load_extension(f'cogs.{filename[:-3]}')


async def main():
    try:
        await load_cogs()
        await bot.start(os.getenv('DISCORD_TOKEN'))
    finally:
        db.close()


asyncio.run(main())
