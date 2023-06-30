/*
 *  Copyright (C) 2022 Lamberto Colazzo
 *  
 *  This file is part of the ConnectX software developed for the
 *  Intern ship of the course "Information technology", University of Bologna
 *  A.Y. 2021-2022.
 *
 *  ConnectX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This  is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details; see <https://www.gnu.org/licenses/>.
 */

package connectx.ALPlayer;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCell;
import java.util.TreeSet;
import java.util.Random;
import java.sql.Time;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

/**
 * Software player only a bit smarter than random.
 * <p>
 * It can detect a single-move win or loss. In all the other cases behaves
 * randomly.
 * </p>
 */
public class ALPlayer implements CXPlayer {
	private Random rand;
	private CXGameState myWin;
	private CXGameState yourWin;
	private int  TIMEOUT;
	private long START;

	// Aggiunto attributo, non sono sicuro che vada bene
	private int MAX_DEPTH;

	/* Default empty constructor */
	public ALPlayer() {
	}

	public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
		// New random seed for each game
		rand    = new Random(System.currentTimeMillis());
		myWin   = first ? CXGameState.WINP1 : CXGameState.WINP2;
		yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
		TIMEOUT = timeout_in_secs;

		// Aggiunto il parametro di profondità massimo per il giocatore (da testare)
		MAX_DEPTH = 2;
	}

	/**
	 * Selects a free colum on game board.
	 */
	public int selectColumn(CXBoard B) {
		START = System.currentTimeMillis(); // Save starting time

		Integer[] L = B.getAvailableColumns();
		int save    = L[rand.nextInt(L.length)]; // Save a random column 

		try {
			// Codice modificato: la logica di selezione della mossa è gestita dal metodo getBestMove(Board).
			int col = getBestMove(B, MAX_DEPTH);
			return col;

		} catch (TimeoutException e) {
			System.err.println("Timeout!!! Random column selected");
			return save;
		}
	}

	private void checktime() throws TimeoutException {
		if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
			throw new TimeoutException();
	}

	// Funzione evaluateMove() usata per stabilire il punteggio dello stato della partita
	private int evaluatePosition(CXBoard B) {
		return rand.nextInt(10);
	}

	// Ritorna la colonna dove giocare che garantisce un punteggio maggiore.
	public int getBestMove(CXBoard B, int Depth) throws TimeoutException {
		// Prendo un array di colonne disponibili per la prossima mossa
        Integer[] Q = B.getAvailableColumns();

		// Valuto ogni mossa possibile e inserisco il valore nell'array corrispondente
		Integer[] vals = new Integer[Q.length];

        for (int i = 0; i < Q.length; i++) {
                vals[i] = evaluateMove(B, i, true, Depth);
        }
		// Trovo la mossa (bestCol) che permette di ottenere il miglior punteggio (bestScore)
		int bestCol = -1;
        int bestScore = Integer.MIN_VALUE;
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] > bestScore) {
				bestScore = vals[i];
                bestCol = Q[i];
            }
			System.out.print(bestCol);
        }

		// Ritorno la colonna da giocare
        return bestCol;
    }

	// Algoritmo MiniMax con Iterative Deepening fino a profondità D presa in input.
	private int evaluateMove(CXBoard B, int column, boolean maxPlayer, int depth) throws TimeoutException {

		// Caso base: ho raggiunto la profondità massima, o ho trovato una posizione a una mossa vincente. 
		// NOTA: probabilmente questo if non è necessario e ha un costo computazionale importante (direi O(n))
		if (B.gameState() == myWin || B.gameState() == yourWin) {
			return column;
		}
		if (singleMoveWin(B, B.getAvailableColumns()) > 0) {
			return singleMoveWin(B, B.getAvailableColumns());
		}
        if (depth == 0 || (singleMoveWin(B, B.getAvailableColumns()) < 0)) {
			CXBoard newB = B.copy();
            return evaluatePosition(newB);
		}
		// Caso giocatore che massimizza
        if (maxPlayer) {
            int bestScore = Integer.MIN_VALUE;

			// Creo nuova Board copiata sulla quale simulare le mosse
            CXBoard newB = B.copy();

			// Assegno la mossa alla board
			newB.markColumn(column);

			// Calcolo il punteggio migliore
			Integer[] moves = newB.getAvailableColumns();
            for (int i = 0; i < moves.length; i++) {
				int score = evaluateMove(newB, moves[i], false, depth - 1);
				bestScore = Math.max(bestScore, score);
            }
            return bestScore;
        } 
		// Caso giocatore che minimizza
		else {
            int bestScore = Integer.MAX_VALUE;

			// Creo nuova Board copiata sulla quale simulare le mosse
            CXBoard newB = B.copy();

			// Assegno la mossa alla board
            newB.markColumn(column);

			// Calcolo il punteggio migliore
			Integer[] moves = newB.getAvailableColumns();
            for (int i = 0; i < moves.length; i++) {
				int score = evaluateMove(newB, moves[i], true, depth - 1);
				
				bestScore = Math.min(bestScore, score);
            }
            return bestScore;
        }
    }

	/**
	 * Check if we can win in a single move
	 *
	 * Returns the winning column if there is one, otherwise -1
	 */	
	private int singleMoveWin(CXBoard B, Integer[] L) throws TimeoutException {
    for(int i : L) {
			checktime(); // Check timeout at every iteration
      CXGameState state = B.markColumn(i);
      if (state == myWin)
        return i; // Winning column found: return immediately
      B.unmarkColumn();
    }
		return -1;
	}

	/**
	 * Check if we can block adversary's victory 
	 *
	 * Returns a blocking column if there is one, otherwise a random one
   	 */
	private int singleMoveBlock(CXBoard B, Integer[] L) throws TimeoutException {
		TreeSet<Integer> T = new TreeSet<Integer>(); // We collect here safe column indexes

		for(int i : L) {
			checktime();
			T.add(i); // We consider column i as a possible move
			B.markColumn(i);

			int j;
			boolean stop;

			for(j = 0, stop=false; j < L.length && !stop; j++) {
				//try {Thread.sleep((int)(0.2*1000*TIMEOUT));} catch (Exception e) {} // Uncomment to test timeout
				checktime();
				if(!B.fullColumn(L[j])) {
					CXGameState state = B.markColumn(L[j]);
					if (state == yourWin) {
						T.remove(i); // We ignore the i-th column as a possible move
						stop = true; // We don't need to check more
					}
					B.unmarkColumn(); // 
				}
			}
			B.unmarkColumn();
		}

		if (T.size() > 0) {
			Integer[] X = T.toArray(new Integer[T.size()]);
 			return X[rand.nextInt(X.length)];
		} else {
			return L[rand.nextInt(L.length)];
		}
	}

	public String playerName() {
		return "ALPlayer";
	}
}
