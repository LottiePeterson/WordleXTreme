from discord.ext import commands
from database import db
class GameCog(commands.Cog):
    def __init__(self, bot):
        self.bot = bot

    @commands.group(aliases=['game', 'curr', 'g'])
    async def _game(self, ctx):
        if ctx.invoked_subcommand is None:
            # await ctx.send("Please use `!score submit` or `!score history`")
        
            guildId = str(ctx.guild.id)
            query = ("SELECT p.WordleName, gp.PlayerID, g.Name, "
                    "SUM(s.SubScore) AS totalSubScore, "
                    "SUM(s.SuperScore) AS totalSuperScore "
                    "FROM GamesPlayers gp JOIN Games g ON gp.GameID = g.ID "
                                        "JOIN Scores s ON gp.PlayerID = s.PlayerID AND gp.GameID = s.GameID "
                                        "JOIN Players p on gp.PlayerID = p.ID "
                                        "JOIN Guilds gd ON gd.ID = g.GuildID "
                    "WHERE g.Current = 1 AND g.GuildID = %s "
                    "GROUP BY p.WordleName, gp.PlayerID, gp.GameID "
                    "ORDER BY p.WordleName;")
            summary = db.fetch(query,(guildId))
            
            if not summary:
                await ctx.send("No current games in this server!")
            else:
                status = ''
                for r_idx, row in enumerate(summary):
                    if r_idx == 0:
                        header = '===== **' + str(row['Name']) + '** =====\n'
                        status += header
                    status += row['WordleName'] + ': ' + str(row['totalSuperScore']) + ' ' + str(row['totalSubScore']) + '\n'
                for _ in range(len(header) - 4):
                    status += '='
                await ctx.send(status)

    @_game.command(aliases=['new', 'n'])
    async def _game_new(self, ctx, name):
        # create a new game with a given name
        await ctx.send(f"Hey! New game to be made with the name {name}")

async def setup(bot):
    await bot.add_cog(GameCog(bot))