import os

import asyncio

import discord
from discord.ext import commands
from dotenv import load_dotenv

from database import db

# initial bot set up
load_dotenv()
intents = discord.Intents.default()
intents.message_content = True
bot = commands.Bot(command_prefix='WordleX ', intents=intents)

async def on_ready():
    print('WordleXTreme: Activate!')

async def shutdown():
    print('worldeXTreme: Deactivate!')
    db.close()

async def load_cogs():
    # Load cogs into bot via file name
    cog_directory = os.path.join(os.path.dirname(__file__), 'cogs')
    print("Current working directory:", os.getcwd())
    print(cog_directory)
    for filename in os.listdir(cog_directory):
        if filename.endswith('.py'):
            await bot.load_extension(f'cogs.{filename[:-3]}')

@bot.command(aliases=['assist', 'commands'])
async def _help_pls(ctx):
    message = "## WELCOME TO THE WORLDEXTREME!\n" \
    "This helpful bot is here to keep track of all your wordle scoring competition needs.\nEach command must start with 'Wordle' - remember, the capitalization matters!\n" \
    "### COMMAND LIST\n" \
    "`add player` - adds a player to this guild's current game.\n" \
    "`game` - displays the status of the current game in this guild.\n" \
    "`game end` - ends the current game in this guild.\n" \
    "`game new` - creates a new current game in this server.\n" \
    "`history` - displays the ID numbers and names of all previous games in this guild.\n" \
    "`score` - calculates and adds scores to the current game in this guild."
    await ctx.send(message)

async def run_me():
    await load_cogs()
    TOKEN = os.getenv('DISCORD_TOKEN')
    await bot.start(TOKEN)

# @bot.command()
# async def user(ctx):
#     cursor.execute("SELECT ID FROM Players WHERE UserID = 594351021736853505")
#     row = cursor.fetchone()
#     print(row)
#     await ctx.send(row)

asyncio.run(run_me())