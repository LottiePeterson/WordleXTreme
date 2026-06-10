import zoneinfo
from datetime import datetime

from backend.database import db

CT = zoneinfo.ZoneInfo('America/Chicago')


def to_unix(naive_ct_dt: datetime) -> int:
    """Convert a naive CT datetime (as stored in DB) to a Unix timestamp for Discord <t:...> formatting."""
    return int(naive_ct_dt.replace(tzinfo=CT).timestamp())


def now_ct_naive() -> datetime:
    """Return the current time in CT as a naive datetime (matches how values are stored in DB)."""
    return datetime.now(CT).replace(tzinfo=None)


def ensure_guild(guild_id: str) -> int:
    """Upsert a guild row and return its integer PK."""
    db.execute(
        "INSERT INTO Guilds (GuildID) VALUES (%s) ON DUPLICATE KEY UPDATE GuildID = GuildID",
        guild_id,
    )
    return db.fetch("SELECT ID FROM Guilds WHERE GuildID = %s", guild_id)[0]['ID']


def ensure_registered(user_id: str, display_name: str) -> int:
    """Upsert a player row (updating DisplayName each time) and return its integer PK."""
    db.execute(
        "INSERT INTO Players (UserID, DisplayName) VALUES (%s, %s) "
        "ON DUPLICATE KEY UPDATE DisplayName = VALUES(DisplayName)",
        user_id,
        display_name,
    )
    return db.fetch("SELECT ID FROM Players WHERE UserID = %s", user_id)[0]['ID']


def get_active_game(guild_id: str) -> dict | None:
    """Return the Current=TRUE game row for this guild, or None."""
    rows = db.fetch(
        "SELECT g.* FROM Games g "
        "JOIN Guilds gu ON gu.ID = g.GuildID "
        "WHERE gu.GuildID = %s AND g.Current = TRUE "
        "LIMIT 1",
        guild_id,
    )
    return rows[0] if rows else None


def get_game_state(guild_id: str) -> tuple[str, dict | None]:
    """
    Return (state, game_row) where state is one of:
        'NO_GAME'      — no Current=TRUE game exists
        'REGISTRATION' — game exists but StartDateTime is in the future
        'ACTIVE'       — game is in its play period
        'EXPIRED'      — game period has elapsed but Current=TRUE not yet cleared
    """
    game = get_active_game(guild_id)
    if not game:
        return 'NO_GAME', None
    now = now_ct_naive()
    if game['StartDateTime'] > now:
        return 'REGISTRATION', game
    if game['EndDateTime'] >= now:
        return 'ACTIVE', game
    return 'EXPIRED', game


def format_standings(game_id: int, game_name: str) -> str:
    """Build a markdown leaderboard string for a game (only counts tallied scores)."""
    rows = db.fetch(
        "SELECT p.DisplayName, "
        "COALESCE(SUM(ds.SuperScore), 0.0) AS total_super, "
        "COALESCE(SUM(ds.Guesses), 0) AS total_guesses "
        "FROM GamesPlayers gp "
        "JOIN Players p ON p.ID = gp.PlayerID "
        "LEFT JOIN DailyScores ds "
        "  ON ds.PlayerID = gp.PlayerID AND ds.GameID = gp.GameID AND ds.Tallied = TRUE "
        "WHERE gp.GameID = %s "
        "GROUP BY p.ID, p.DisplayName "
        "ORDER BY total_super DESC, total_guesses ASC",
        game_id,
    )
    if not rows:
        return f"**{game_name}** — no scores yet."

    medals = ["🥇", "🥈", "🥉"]
    lines = [f"## {game_name}"]
    for i, row in enumerate(rows):
        prefix = medals[i] if i < 3 else f"{i + 1}."
        lines.append(
            f"{prefix} **{row['DisplayName']}**: "
            f"{float(row['total_super']):.1f} pts "
            f"({row['total_guesses']} total guesses)"
        )
    return "\n".join(lines)
