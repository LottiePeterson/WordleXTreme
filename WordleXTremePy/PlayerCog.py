import discord
from discord.ext import commands

class PlayerCog(commands.Cog):
    def __init__(self, bot, db_connection):
        self.bot = bot
        self.db = db_connection
        
    @commands.command()
    async def register(self, ctx, nickname=None):
        # Register a new player
        user_id = ctx.author.id
        nickname = nickname or ctx.author.name
        # Add to database logic
        await ctx.send(f"Registered {nickname} as a player!")
        
    @commands.command()
    async def profile(self, ctx, member: discord.Member = None):
        # Show player profile
        target = member or ctx.author
        # Query db for player stats
        await ctx.send(f"{target.name}'s Wordle stats: ...")