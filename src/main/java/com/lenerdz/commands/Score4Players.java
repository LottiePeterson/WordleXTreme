package com.lenerdz.commands;

import java.util.Arrays;

public class  Score4Players{

    /**
	 * Scores for a 4 player game using the playerArr
	 */
	public static double[] score4Players(int[] guessCounts){
		int[] places = getPlaces(guessCounts);
		int[] numPlaces = getNumPlaces(places);
		
		double[] playerScores = {0, 0, 0, 0};
		double[] scoresPerPlace = {3.0, 2.0, 1.0, 0.0};
		double scoreGiven = 0;
		int playersScored = 0;
		int currentPlaceScoring = 0;
		
		//The jank loop that scores everything
		while(playersScored != 4) {
			scoreGiven = 0;
			int playersWithCurrentScore = 0;
			for(int i = 0; i < numPlaces[currentPlaceScoring]; i++) {
				scoreGiven += scoresPerPlace[i + currentPlaceScoring];
				playersScored++;
				playersWithCurrentScore++;
			}
			scoreGiven /= playersWithCurrentScore;
			for(int i = 0; i < places.length; i++) {
				if(places[i] == currentPlaceScoring + 1) {
					playerScores[i] = scoreGiven;
				}
			}
			currentPlaceScoring++;
		}
		return playerScores;
	}

	/**
	 * converts an array of guesses to an array of places
	 * @param guessCounts
	 * 	an array, of size 4, of player guesses
	 * @return
	 * 	an array where value 1 is 1st, value 2 is 2nd...
	 */
	private static int[] getPlaces(int[] guessCounts) {
		int[] places = {0, 0, 0, 0};
		int[] temp = new int[4];
		for(int i = 0; i < guessCounts.length; i++) {
			temp[i] = guessCounts[i];
		}

		Arrays.sort(temp);
		int lowest = temp[0];
		int highest = temp[3];

		int currPlace = 1;
		int repeats = 0;
		for(int i = lowest; i <= highest; i++) {
			int reps = 0;
			boolean placed = false;
			for(int j = 0; j < guessCounts.length; j++) {
				if(guessCounts[j] == i) {
					places[j] = currPlace;
					placed = true;
					if(reps > 0) {
						repeats++;
					}
					reps++;
				}
			}
			if(placed) {
				currPlace += repeats + 1;
				repeats = 0;
			}
		}
		return places;
	}

	/**
	 * 
	 * @param guessCounts
	 * 	an array, of size 4, of player places
	 * @return
	 * 	the number of guesses between 1 and 4 in places
	 */
	private static int[] getNumPlaces(int[] places) {
		int[] numPlaces = {0, 0, 0, 0};

		for(int i = 0; i < places.length; i++) {
			//minus one because the index for guessing 1 is index 0
			numPlaces[places[i] - 1]++;
		}
		return numPlaces;
	}
}
