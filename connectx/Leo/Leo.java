package connectx.Leo;
import java.util.Comparator;
import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCell;
import java.util.TreeSet;
import java.util.Random;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;
import connectx.CXCellState;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class Leo implements CXPlayer {
    private Random rand;
    private CXGameState myWin;
    private CXGameState yourWin;
    private int TIMEOUT;

	// Parametri aggiuntivi per il player
    private int MAX_DEPTH;
    private long START;
    private int columns;
    private int k;
    private int rows;
	private boolean isFirstPlayer;
	private int bestCol; // tiene traccia della best col fino a questo momento.
	

    /* Default empty constructor */
    public Leo() {
    }

    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        this.rand = new Random(System.currentTimeMillis());
        this.myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
        this.yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
        this.TIMEOUT = timeout_in_secs;


		// Parametri aggiuntivi per il player
        this.MAX_DEPTH = 4; 
        this.columns = N;
        this.k = K;
        this.rows = M;
		this.isFirstPlayer=first;
		this.bestCol = rand.nextInt(N);
    }

    private void checktime() throws TimeoutException {
        if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
            throw new TimeoutException();
    }

   //metodo principale di selezione di una colonna, da getbestmove partono gli altri metodi
    public int selectColumn(CXBoard B) {
        this.START = System.currentTimeMillis();
        
        try {
            int col = iterativeDeepening(B, MAX_DEPTH);
            return col;
        } catch (TimeoutException e) {
            System.err.println("Timeout!!! Random column selected");
            return this.bestCol;
        }
    }

    public int getColumns() {
        return columns;
    }

    public int getRows() {
        return rows;
    }

    public int getK() {
        return k;
    }
		

	private int evaluatePosition(CXBoard B) {
		
		// Score: punteggio della posizione
		int score = 0;
	
		// Valutazione delle righe
		for (int row = 0; row < getRows(); row++) {

			// Per ogni riga, inizializzo i parametri che mi servono
			CXCellState firstPlayer = CXCellState.FREE;
			int emptyCount = 0;
			int countP1 = 0;
			int countP2 = 0;
			int freeHole1 = 0;
			int freeHole2 = 0;

			// Per ogni colonna, parto da sx e vado verso dx fino a quando non incontro una cella occupata.
			// Quando incontro una cella, l'unico giocatore che può vincere è il giocatore
			// che ha l'estremità libera.
			// In caso di cella libera tra due pedine dello stesso giocatore, la somma continua.

			for (int col = 0; col < getColumns(); col++) {
				
				CXCellState cellState = B.cellState(row, col);

				// Se non ho ancora trovato una cella occupata, proseguo
				if (cellState == CXCellState.FREE && firstPlayer == CXCellState.FREE) {
					emptyCount++;
					countP1 = 0;
					countP2 = 0;
					
				} else if (cellState != CXCellState.FREE && firstPlayer == CXCellState.FREE) {
					// Ho trovato la prima cella non occupata nella riga.
					// Assegno a first il player trovato.
					firstPlayer = cellState;
					countP1 = 0;
					countP2 = 0;
					if (firstPlayer == CXCellState.P1) {
						countP1++;
					} else { 
						countP2++;
					}
				
				} else if (cellState != CXCellState.FREE && firstPlayer == cellState) {
					// Ho trovato una cella occupata consecutiva ad una occupata: è dello stesso giocatore, aumenta il count.
					if (firstPlayer == CXCellState.P1) {
						countP1++;
					} else { 
						countP2++;
					}
					// if (countP1 == getK()) {

					// }

				} else if (cellState != CXCellState.FREE && firstPlayer != cellState) {
					// Ho trovato una cella occupata consecutiva ad una occupata: è dell'altro giocatore, azzero il player e inizio a contare quello dell'altro giocatore.
					if (cellState == CXCellState.P1) {
						countP1 = 1;
						// Salva il punteggio parziale ottenuto da p2 nell'array
						if (isFirstPlayer) {
								score -= calculateLineScore(countP2, 0);
							} else {
								score += calculateLineScore(countP2, 0);
							}
						countP2 = 0;
					} else { 
						countP2 = 1;
						// Salva il punteggio parziale ottenuto da p1 nell'array
						if (isFirstPlayer) {
							score += calculateLineScore(countP1, 0);
						} else {
							score -= calculateLineScore(countP1, 0);
						}
					}
					firstPlayer = cellState;


				} else if (cellState == CXCellState.FREE && firstPlayer != CXCellState.FREE) {
					// Trovo una cella libera dopo averne trovate di occupate.
					// La cella è la prima libera, ma ho già trovato nella riga una cella occupata.
					// Valuto quindi il "buco" lasciato tra le celle. Se supera la lunghezza massima, 
					// azzero il count per il giocatore e metto first = FREE.
					emptyCount++;

					if (firstPlayer == CXCellState.P1) { 
						
						if (freeHole1 + countP1 == getK()) {
							if (isFirstPlayer) {
								score += calculateLineScore(countP1, 0);
							} else {
								score -= calculateLineScore(countP1, 0);
							}
							countP1 = 0;
							countP2 = 0;
							freeHole1 = 0;
							freeHole2 = 0;
							firstPlayer = CXCellState.FREE;
						} else {
							freeHole1++;
						}

					} else { 
						if (freeHole2 + countP2 == getK()) {
							if (isFirstPlayer) {
								score -= calculateLineScore(countP2, 0);
							} else {
								score += calculateLineScore(countP2, 0);
							}
							countP1 = 0;
							countP2 = 0;
							freeHole1 = 0;
							freeHole2 = 0;
							firstPlayer = CXCellState.FREE;

						} else {
							freeHole2++;
						}
					}
				} 
			}
		}
	
		// Valutazione delle colonne
		for (int col = 0; col < getColumns(); col++) {
			// Se la colonna è piena, non valuto la colonna (non è possibile ottenere punti extra da quella colonna)
			if (!B.fullColumn(col)) {

				// Per ogni colonna, inizializzo i parametri che mi servono
				CXCellState firstPlayer = CXCellState.FREE;
				int emptyCount = 0;
				int countP1 = 0;
				int countP2 = 0;

				// Per ogni riga, parto dall'alto e scendo fino a quando non incontro una cella.
				// Quando incontro una cella, l'unico giocatore che può vincere è il giocatore
				// nella prima cella libera partendo dall'alto
				for (int row = 0; row < getRows(); row++) {

					
					CXCellState cellState = B.cellState(row, col);

					// Se non ho ancora trovato una cella occupata, proseguo
					if (cellState == CXCellState.FREE) {
						emptyCount++;
					} else {
						// Appena trovo la prima cella, assegno firstplayer al giocatore trovato
						if (firstPlayer == CXCellState.FREE) {
							firstPlayer = cellState;
						}
						// Se la cella è uguale a quella del primo giocatore, continuo a contare
						if (firstPlayer == cellState){
							// In funzione del giocatore assegno il punteggio
							if (firstPlayer == CXCellState.P1) {
								countP1++;
							} else { 
								countP2++;
							}
						
						} else {
							// Se la cella non è dello stesso giocatore incontrato per primo, 
							// calcolo il punteggio e interrompo la valutazione della riga
							if (isFirstPlayer)
							{score += calculateLineScore(countP1, emptyCount);
							score -= calculateLineScore(countP2, emptyCount);}
							else {
								score += calculateLineScore(countP2, emptyCount);
							score -= calculateLineScore(countP1, emptyCount);
							}
							break;
						}

					}
					
				}
			}
		}
	
		// Valutazione delle diagonali ascendenti
		for (int row = getK() - 1; row < getRows(); row++) {
			for (int col = 0; col <= getColumns() - getK(); col++) {
				int emptyCount = 0;
				int playerCount = 0;
				int opponentCount = 0;

				for (int i = 0; i < row; i++) {
					CXCellState cellState3= B.cellState(row - i, i);
	
					if (cellState3== CXCellState.FREE) {
						emptyCount++;
					} else if (cellState3== CXCellState.P1 && isFirstPlayer || cellState3== CXCellState.P2 && !isFirstPlayer) {
						playerCount++;
						opponentCount = 0;
					} else {
						playerCount = 0;
						opponentCount++;
					}
				}
	
				if (emptyCount > 0 && playerCount > 0) {
					score += calculateLineScore(playerCount, emptyCount);
				}
				if (emptyCount > 0 && opponentCount > 0) {
					score -= calculateLineScore(opponentCount, emptyCount);
				}
			}
		}
	
		// Valutazione delle diagonali discendenti
		for (int row = 0; row <= getRows() - getK(); row++) {
			for (int col = 0; col <= getColumns() - getK(); col++) {
				int emptyCount = 0;
				int playerCount = 0;
				int opponentCount = 0;


				for (int i = 0; i <= Math.min(getRows() - getK(),getColumns() - getK() ); i++) {
					CXCellState cellState4= B.cellState(row + i, col + i);
	
					if (cellState4== CXCellState.FREE) {
						emptyCount++;
					} else if (cellState4== CXCellState.P1 && isFirstPlayer || cellState4== CXCellState.P2 && !isFirstPlayer  ) {
						playerCount++;
						opponentCount = 0;

					} else {
						playerCount = 0;
						opponentCount++;

					}
				}
	
				if (playerCount > 0) {
					score += calculateLineScore(playerCount, emptyCount);
				}
				if (opponentCount > 0) {
					score -= calculateLineScore(opponentCount, emptyCount);
				}
			}
		}
	
		return score;
	}

	private int calculateLineScore(int count, int emptyCount) {
		return count * count * count * emptyCount;
	}


	public int iterativeDeepening(CXBoard B, int depth) throws TimeoutException {
		int[] orderedMoves = new int[B.N];
		Integer[] Q = B.getAvailableColumns();
		int bestCol = Q[rand.nextInt(Q.length)];
		for (int d = 1; d <= depth; d++) {
			try {
				checktime();
	
				// Ottengo un punteggio per ogni mossa, ordino le mosse in ordine di punteggio, e ritorno la mossa più efficace.
				List<Integer> moves = new ArrayList<>(Arrays.asList(Q));
				moves.sort(Comparator.comparingInt(col -> {
					CXBoard newB = B.copy();
					newB.markColumn(col);
					int score;
					try {
						score = evaluateMove(newB, col, false, depth, Integer.MIN_VALUE, Integer.MAX_VALUE);
					} catch (TimeoutException e) {
						score = -1000;
					}
					return score;
				}));
				Collections.reverse(moves);

				orderedMoves = moves.stream().mapToInt(Integer::intValue).toArray();

			} catch (TimeoutException e) {
				return orderedMoves[0];
			}
		}
		return orderedMoves[0];

	}

	// Algoritmo alpha-beta pruning di ricerca della mossa migliore.
	private int evaluateMove(CXBoard B, int column, boolean maxPlayer, int depth, int alpha, int beta) throws TimeoutException {
		checktime();
		if (B.gameState() == myWin) {
			return Integer.MAX_VALUE; // Vittoria immediata
		}
		if (B.gameState() == yourWin) {
			return Integer.MIN_VALUE; // Sconfitta immediata
		}
		if (depth == 0) {
			// Raggiunto il limite di profondità della ricerca, valuta la posizione corrente
			return evaluatePosition(B);
		}
	
		int bestScore;
		if (maxPlayer) {
			bestScore = Integer.MIN_VALUE;
			Integer[] moves = B.getAvailableColumns();
			for (int move : moves) {
				CXBoard newB = B.copy();
				newB.markColumn(move);
				int score = evaluateMove(newB, move, false, depth - 1, alpha, beta);
				bestScore = Math.max(bestScore, score);
				alpha = Math.max(alpha, score);
				if (beta <= alpha) {
					break; // Beta cut-off
				}
			}
		} else {
			bestScore = Integer.MAX_VALUE;
			Integer[] moves = B.getAvailableColumns();
			for (int move : moves) {
				CXBoard newB = B.copy();
				newB.markColumn(move);
				int score = evaluateMove(newB, move, true, depth - 1, alpha, beta);
				bestScore = Math.min(bestScore, score);
				beta = Math.min(beta, score);
				if (beta <= alpha) {
					break; // Alpha cut-off
				}
			}
		}
	
		return bestScore;
	}

    public String playerName() {
        return "Leo";
    }
}

