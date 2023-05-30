package com.lenerdz.commands;

import java.util.Arrays;

public class Temp {

	public static double[] ScoreVariable(int[] subScores, double[] placeValues) {
		int[] places = getVariablePlaces(subScores);
		int[] numPlaces = getVariableNumPlaces(places);
		
		double[] playerScores = new double[subScores.length];
		double scoreGiven = 0;
		int playersScored = 0;
		int currentPlaceScoring = 0;
		
		Arrays.fill(playerScores, 0);
		
		//The jank loop that scores everything
				while(playersScored != subScores.length) {
					scoreGiven = 0;
					int playersWithCurrentScore = 0;
					for(int i = 0; i < numPlaces[currentPlaceScoring]; i++) {
						scoreGiven += placeValues[i + currentPlaceScoring];
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
	
	private static int[] getVariablePlaces(int[] guessCounts) {
		int[] places = new int[guessCounts.length];
		int[] temp = new int[guessCounts.length];
		
		for(int i = 0; i < guessCounts.length; i++) {
			temp[i] = guessCounts[i];
			places[i] = 0;
		}

		Arrays.sort(temp);
		int lowest = temp[0];
		int highest = temp[guessCounts.length - 1];

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
	
	private static int[] getVariableNumPlaces(int[] places) {
		int[] numPlaces = new int[places.length];
		
		for(int i = 0; i < numPlaces.length; ++i) {
			numPlaces[i] = 0;
		}

		for(int i = 0; i < places.length; i++) {
			//minus one because the index for guessing 1 is index 0
			numPlaces[places[i] - 1]++;
		}
		return numPlaces;
	}

}

