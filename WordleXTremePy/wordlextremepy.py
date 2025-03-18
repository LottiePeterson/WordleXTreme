import os

import mysql.connector

import discord
from discord.ext import commands
from dotenv import load_dotenv


load_dotenv()
TOKEN = os.getenv('DISCORD_TOKEN')
HOST = os.getenv('DB_HOST')
USER = os.getenv('DB_USERNAME')
PASSWORD = os.getenv('DP_PASSWORD')
DATABASE = os.getenv('DATABASE_NAME')

intents = discord.Intents.default()
intents.message_content = True
bot = commands.Bot(command_prefix='WordleX ', intents=intents)

try:
    conn = mysql.connector.connect(
        host=HOST,
        user=USER,
        password=PASSWORD,
        database=DATABASE
    )
    print("Connection successful!")
    cursor = conn.cursor()

    @bot.command(aliases=['game', 'curr', 'g'])
    async def _game(ctx):
        # guildId = str(ctx.guild.id)
        guildId = '3'
        query = "SELECT p.WordleName, gp.PlayerID, g.Name, SUM(s.SubScore) AS totalSubScore, SUM(s.SuperScore) AS totalSuperScore FROM GamesPlayers gp JOIN Games g ON gp.GameID = g.ID JOIN Scores s ON gp.PlayerID = s.PlayerID AND gp.GameID = s.GameID JOIN Players p on gp.PlayerID = p.ID JOIN Guilds gd ON gd.ID = g.GuildID WHERE g.Current = 1 AND g.GuildID = " + guildId + " GROUP BY p.WordleName, gp.PlayerID, gp.GameID ORDER BY p.WordleName;"
        cursor.execute(query)
        summary = cursor.fetchall()
        if not summary:
            await ctx.send("No current games in this server!")
        else:
            status = ''
            for r_idx, row in enumerate(summary):
                if r_idx == 0:
                    header = '===== **' + str(row[2]) + '** =====\n'
                    status += header
                status += row[0] + ': ' + str(row[4]) + ' ' + str(row[3]) + '\n'
            for _ in range(len(header) - 4):
                status += '='
            await ctx.send(status)

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

    # @bot.command()
    # async def user(ctx):
    #     cursor.execute("SELECT ID FROM Players WHERE UserID = 594351021736853505")
    #     row = cursor.fetchone()
    #     print(row)
    #     await ctx.send(row)
    bot.run(TOKEN)
    conn.close()
except mysql.connector.Error as e:
    print(f"Error: {e}")


# @bot.command()
# async def user(ctx):
#     try: 
#         conn = mysql.connector.connect(
#             host=HOST,
#             user=USER,
#             password=PASSWORD,
#             database=DATABASE
#         )
#         print("Connection successful!")
#         cursor = conn.cursor()
#         cursor.execute("SELECT ID FROM Players WHERE UserID = 594351021736853505")
#         row = cursor.fetchone()
#         print(row)
#         await ctx.send(row)
#         conn.close()

#     except mysql.connector.Error as e:
#         print(f"Error: {e}")

# bot.run(TOKEN)

# try:
#     # Establish the connection
#     conn = mysql.connector.connect(
#         host='HOST',
#         user='USER',
#         password='PASSWORD',
#         database='DATABASE'
#     )
#     print("Connection successful!")

#     # Example query
#     cursor = conn.cursor()
#     cursor.execute("SHOW TABLES;")
#     for table in cursor.fetchall():
#         print(table)

#     # Close the connection
#     conn.close()

    
# except mysql.connector.Error as e:
#     print(f"Error: {e}")
