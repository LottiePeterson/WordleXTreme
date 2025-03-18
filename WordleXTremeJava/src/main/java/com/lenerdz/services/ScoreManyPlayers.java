package com.lenerdz.services;

import java.util.Arrays;

public class ScoreManyPlayers {
	/**
	 * Scores for a variable number of player game using the playerArr
	 * 
	 * @param subScores   subScores of the players to be scored
	 * @param placeValues array of super score point values in order for highest to
	 *                    lowest. For a four player game the array looks like 
	 * 					  [3.0, 2.0, 1.0, 0.0].
	 * @return an array of the super scores for the given sub scores, in order.
	 */
	public static double[] scoreVariable(int[] subScores, double[] placeValues) {
		int[] places = getVariablePlaces(subScores);
		int[] numPlaces = getVariableNumPlaces(places);

		double[] playerScores = new double[subScores.length];
		double scoreGiven = 0;
		int playersScored = 0;
		int currentPlaceScoring = 0;

		Arrays.fill(playerScores, 0);

		// The jank loop that scores everything
		while (playersScored != subScores.length) {
			scoreGiven = 0;
			int playersWithCurrentScore = 0;
			for (int i = 0; i < numPlaces[currentPlaceScoring]; i++) {
				scoreGiven += placeValues[i + currentPlaceScoring];
				playersScored++;
				playersWithCurrentScore++;
			}
			scoreGiven /= playersWithCurrentScore;
			for (int i = 0; i < places.length; i++) {
				if (places[i] == currentPlaceScoring + 1) {
					playerScores[i] = scoreGiven;
				}
			}
			currentPlaceScoring++;
		}
		return playerScores;
	}

	/**
	 * Converts an array of guesses to an array of places
	 * 
	 * @param guessCounts an array of variable size of player guesses
	 * @return an array where value 1 is st, 2 is 2nd, and so on.
	 */
	private static int[] getVariablePlaces(int[] guessCounts) {
		int[] places = new int[guessCounts.length];
		int[] temp = new int[guessCounts.length];

		for (int i = 0; i < guessCounts.length; i++) {
			temp[i] = guessCounts[i];
			places[i] = 0;
		}

		Arrays.sort(temp);
		int lowest = temp[0];
		int highest = temp[guessCounts.length - 1];

		int currPlace = 1;
		int repeats = 0;
		for (int i = lowest; i <= highest; i++) {
			int reps = 0;
			boolean placed = false;
			for (int j = 0; j < guessCounts.length; j++) {
				if (guessCounts[j] == i) {
					places[j] = currPlace;
					placed = true;
					if (reps > 0) {
						repeats++;
					}
					reps++;
				}
			}
			if (placed) {
				currPlace += repeats + 1;
				repeats = 0;
			}
		}
		return places;
	}

	/**
	 * 
	 * @param places an array of a variable size of player places
	 * @return the number of guesses between 1 and a variable last place
	 */
	private static int[] getVariableNumPlaces(int[] places) {
		int[] numPlaces = new int[places.length];

		for (int i = 0; i < numPlaces.length; ++i) {
			numPlaces[i] = 0;
		}

		for (int i = 0; i < places.length; i++) {
			// minus one because the index for guessing 1 is index 0
			numPlaces[places[i] - 1]++;
		}
		return numPlaces;
	}

}
