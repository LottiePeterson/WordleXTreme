def get_places(guesses: list[int]) -> list[int]:
    """
    Convert a list of guess counts into place rankings.
    Lower guesses = better place. Tied values share the same place number,
    and the next place skips accordingly (e.g. two tied for 1st → next is 3rd).
    """
    sorted_unique = sorted(set(guesses))
    curr_place = 1
    place_map: dict[int, int] = {}
    for val in sorted_unique:
        count = guesses.count(val)
        place_map[val] = curr_place
        curr_place += count
    return [place_map[g] for g in guesses]


def calculate_superscores(guesses: list[int]) -> list[float]:
    """
    Calculate placement-based superscores from a list of guess counts.

    Points available: [n-1, n-2, ..., 1, 0] for n players.
    Tied players split the points they would have occupied (averaged).

    Returns floats in the same order as the input list.

    Example (4 players, guesses=[2, 3, 3, 5]):
        places      = [1, 2, 2, 4]
        place_values = [3.0, 2.0, 1.0, 0.0]
        player 1 (place 1, alone):    3.0
        players 2&3 (place 2, tied):  (2.0 + 1.0) / 2 = 1.5 each
        player 4 (place 4, alone):    0.0
    """
    n = len(guesses)
    if n == 0:
        return []
    if n == 1:
        return [0.0]

    place_values = [float(n - 1 - i) for i in range(n)]
    places = get_places(guesses)

    count_at_place: dict[int, int] = {}
    for p in places:
        count_at_place[p] = count_at_place.get(p, 0) + 1

    scores = [0.0] * n
    for i, place in enumerate(places):
        count = count_at_place[place]
        total = sum(place_values[place - 1 + j] for j in range(count))
        scores[i] = round(total / count, 4)

    return scores
