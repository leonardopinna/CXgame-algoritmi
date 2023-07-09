package connectx.ALPlayer;
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

public class ALPlayer implements CXPlayer {
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
	private boolean isFirst ;
	private int currentBestMove; // tiene traccia della best col fino a questo momento.

	private int maxDepthReached;
	
    /* Default empty constructor */
    public ALPlayer() {
    }

    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        this.rand = new Random(System.currentTimeMillis());
        this.myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
        this.yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
        this.TIMEOUT = timeout_in_secs;

		// Parametri aggiuntivi per il player
        this.MAX_DEPTH 	= 30; 
        this.columns 	= N;
        this.k 			= K;
        this.rows 		= M;
		this.isFirst 	= first;

		this.maxDepthReached = 0;
		
    }

    private void checktime() throws TimeoutException {
        if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
            throw new TimeoutException();
    }

	private int singleMoveBlock(CXBoard B) throws TimeoutException {
		Integer[] Q = B.getAvailableColumns(); 

		for(int i : Q) {
			checktime();
			B.markColumn(i);

			if (B.gameState() == yourWin) return (i);

			B.unmarkColumn();
		}

		return -1;
	}

   //metodo principale di selezione di una colonna, da getbestmove partono gli altri metodi
    public int selectColumn(CXBoard B) {
        this.START = System.currentTimeMillis();
		Integer[] Q = B.getAvailableColumns();
		this.currentBestMove = Q[rand.nextInt(Q.length)];
        
        try {

			if (B.getMarkedCells().length < 2) {
				return (B.N) / 2;
			} 
		
			if (singleMoveBlock(B) >= 0) return singleMoveBlock(B);

            int col = iterativeDeepening(B, MAX_DEPTH);
            return col;

        } catch (TimeoutException e) {
            System.err.println("Timeout!!! Random column selected");
            return this.currentBestMove;
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
		
	private int evaluatePosition(CXBoard B) throws TimeoutException {

		int p1Score = 0;
		int p2Score = 0;

		// Versione migliorata di valutazione prendendo le celle.
		for (CXCell C : B.getMarkedCells()) {
			checktime();
			int x = C.i;
			int y = C.j;
			int cellPoints 	= 0;
			int counter 	= 0;

			// Parametri di assegnazione punteggio
			int emptyVal 	= 10;
			int columnVal 	= 15;
			int rowVal 		= 25;
			int diagVal		= 25;

			// Valutazione della colonna (verso l'alto)
			for (int index = - getK() ; index <= getK(); index++) {
				
				// Arrivo oltre il limite superiore della matrice. Interrompo la ricerca per colonna.
				if ( x - index  <= -1) {
					// Zero punti perchè non posso fare molto.
					break; 
				}

				// Se non sono al di sotto della matrice, considero le pedine entro il range di valutazione.
				if (!(x - index >= getRows())) {
					if (B.cellState(x - index, y) == CXCellState.FREE) {
						// La cella sopra è libera: vuol dire che potenzialmente posso continuare la streak
						counter += emptyVal;
					} else if (B.cellState(x - index, y) == B.cellState(x,y) ) {
						// La cella sopra è uguale a questa cella: ottengo molti punti.
						counter += columnVal;
					} else {
						// La cella sopra non è uguale alla cella attualmente valutata; la cella è bloccata verso l'alto.
						// Zero punti.
						counter = 0;
						if (index > 0) {break;}
						break;
					}
				}
			}

			// Salvo i punti ottenuti dalla cella nella precedente valutazione. 
			// Riazzero il conteggio per la successiva valutazione.
			cellPoints += counter;
			counter = 0;

			// Valutazione della riga (sx verso dx)
			for (int index =  - getK() ; index <= getK(); index++) {
				
				// Arrivo oltre il limite destro della matrice. Interrompo la ricerca.
				if ( y + index  >= getColumns()) {
					// Counter non è azzerato perchè posso avere avuto punteggio nelle celle precedenti.
					// counter = 0;
					break; 
				}

				// Se non sono sinistra della matrice, oppure nella cella stessa, procedo alla valutazione.
				if (!(y + index < 0) && (index != 0)) {
					if (B.cellState(x, y + index) == CXCellState.FREE) {
						// La cella è libera: vuol dire che potenzialmente posso continuare la streak
						counter += emptyVal;
					} else if (B.cellState(x, y + index) == B.cellState(x,y) ) {
						// La cella è dello stesso giocatore della cella valutata: ottengo molti punti.
						// Ottengo meno punti della colonna perchè valuto due direzioni.
						counter += rowVal;
					} else {
						// La cella trovata non è uguale alla cella attualmente valutata; se la cella è prima della cella
						// attualmente valutata, azzero il punteggio e riparto. Se la cella è dopo la cella attuale, 
						// allora è bloccata e interrompo il ciclo dopo aver azzerato il punteggio.
						// Zero punti.
						counter = 0;
						if (index > 0) {break;}
					}
				}
			}

			cellPoints += counter;
			counter = 0;

			// Valutazione della riga (dx verso sx)
			for (int index =  - getK() ; index <= getK(); index++) {				
				// Arrivo oltre il limite sinistro della matrice. Interrompo la ricerca.
				if ( y - index  <= -1) {
					// Counter non è azzerato perchè posso avere avuto punteggio nelle celle precedenti.
					// counter = 0;
					break; 
				}

				// Se non parto oltre a destra della matrice, oppure nella cella stessa, procedo alla valutazione.
				if (!(y - index >= getColumns())) {
					if (B.cellState(x, y - index) == CXCellState.FREE) {
						// La cella è libera: vuol dire che potenzialmente posso continuare la streak
						counter += emptyVal;
					} else if (B.cellState(x, y - index) == B.cellState(x,y) ) {
						// La cella è dello stesso giocatore della cella valutata: ottengo molti punti.
						// Ottengo meno punti della colonna perchè valuto due direzioni.
						counter += rowVal;
					} else {
						// La cella trovata non è uguale alla cella attualmente valutata; se la cella è prima della cella
						// attualmente valutata, azzero il punteggio e riparto. Se la cella è dopo la cella attuale, 
						// allora è bloccata e interrompo il ciclo dopo aver azzerato il punteggio.
						// Zero punti.
						counter = 0;
						if (index > 0) {break;}
					}
				}
			}

			cellPoints += counter;
			counter = 0;

			// Valutazione della diagonale (alto-sx verso basso-dx)
			for (int index =  - getK() ; index <= getK(); index++) {				
				// Arrivo oltre il limite destro o fondo della matrice. Interrompo la ricerca.
				if ( y + index  >= getColumns() || x + index >= getRows()) {
					// Counter non è azzerato perchè posso avere avuto punteggio nelle celle precedenti.
					// counter = 0;

					break; 
				}

				// Se non parto oltre la sinistra o l'alto della matrice, oppure nella cella stessa, procedo alla valutazione.
				if (!((y + index < 0) || ( x + index < 0))) {
					if (B.cellState(x + index, y + index) == CXCellState.FREE) {
						// La cella è libera: vuol dire che potenzialmente posso continuare la streak
						counter += emptyVal;
					} else if (B.cellState(x + index, y + index) == B.cellState(x,y) ) {
						// La cella è dello stesso giocatore della cella valutata: ottengo molti punti.
						// Ottengo meno punti della colonna perchè valuto due direzioni.
						counter += diagVal;
					} else {
						// La cella trovata non è uguale alla cella attualmente valutata; se la cella è prima della cella
						// attualmente valutata, azzero il punteggio e riparto. Se la cella è dopo la cella attuale, 
						// allora è bloccata e interrompo il ciclo dopo aver azzerato il punteggio.
						// Zero punti.
						counter = 0;
						if (index > 0) {
							break;
						}
					}
				}
			}

			cellPoints += counter;
			counter = 0;

			// Valutazione della diagonale (alto-dx verso basso-sx)
			for (int index =  - getK() ; index <= getK(); index++) {
				// Arrivo oltre il limite sinistro o inferiore della matrice. Interrompo la ricerca.
				if ( y - index  < 0 || x + index >= getRows()) {
					// Counter non è azzerato perchè posso avere avuto punteggio nelle celle precedenti.					
					// counter = 0;

					break; 
				}

				// Se non parto oltre la destra o sopra della matrice, oppure nella cella stessa, procedo alla valutazione.
				if (!((y - index >= getColumns()) || ( x + index < 0))) {
					if (B.cellState(x + index, y - index) == CXCellState.FREE) {
						// La cella è libera: vuol dire che potenzialmente posso continuare la streak
						counter += 5;
					} else if (B.cellState(x + index, y - index) == B.cellState(x,y) ) {
						// La cella è dello stesso giocatore della cella valutata: ottengo molti punti.
						// Ottengo meno punti della colonna perchè valuto due direzioni.
						counter += diagVal;
					} else {
						// La cella trovata non è uguale alla cella attualmente valutata; se la cella è prima della cella
						// attualmente valutata, azzero il punteggio e riparto. Se la cella è dopo la cella attuale, 
						// allora è bloccata e interrompo il ciclo dopo aver azzerato il punteggio.
						// Zero punti.
						counter = 0;
						if (index > 0) {break;}
					}
				}
			}

			cellPoints += counter;
			counter = 0;

			// Valutazione della diagonale (basso-sx verso alto-dx)
			for (int index =  - getK() ; index <= getK(); index++) {
				
				// Arrivo oltre il limite superiore o destro della matrice. Interrompo la ricerca.
				if ( y + index  >= getColumns() || x - index < 0 ) {
					// Counter non è azzerato perchè posso avere avuto punteggio nelle celle precedenti.				
					// counter = 0;

					break; 
				}

				// Se non parto oltre la sinistra o il basso della matrice, oppure nella cella stessa, procedo alla valutazione.
				if (!((y + index < 0) || ( x - index >= getRows()))) {
					if (B.cellState(x - index, y + index) == CXCellState.FREE) {
						// La cella è libera: vuol dire che potenzialmente posso continuare la streak
						counter += 5;
					} else if (B.cellState(x - index, y + index) == B.cellState(x,y) ) {
						// La cella è dello stesso giocatore della cella valutata: ottengo molti punti.
						// Ottengo meno punti della colonna perchè valuto due direzioni.
						counter += diagVal;
					} else {
						// La cella trovata non è uguale alla cella attualmente valutata; se la cella è prima della cella
						// attualmente valutata, azzero il punteggio e riparto. Se la cella è dopo la cella attuale, 
						// allora è bloccata e interrompo il ciclo dopo aver azzerato il punteggio.
						// Zero punti.
						counter = 0;
						if (index > 0) {break;}
					}
				}
			}

			cellPoints += counter;
			counter = 0;

			// Valutazione della diagonale (basso-dx verso alto-sx)
			for (int index =  - getK() ; index <= getK(); index++) {
				
				// Arrivo oltre il limite sinistro o superiore della matrice. Interrompo la ricerca.
				if ( y - index  < 0 || x - index < 0) {
					// Counter non è azzerato perchè posso avere avuto punteggio nelle celle precedenti.
					// counter = 0;
					
					break; 
				}

				// Se non parto da oltre la destra o il basso della matrice, oppure nella cella stessa, procedo alla valutazione.
				if (!((y - index >= getColumns()) || ( x - index >= getRows()))) {
					if (B.cellState(x - index, y - index) == CXCellState.FREE) {
						// La cella è libera: vuol dire che potenzialmente posso continuare la streak
						counter += 5;
					} else if (B.cellState(x - index, y - index) == B.cellState(x,y) ) {
						// La cella è dello stesso giocatore della cella valutata: ottengo molti punti.
						// Ottengo meno punti della colonna perchè valuto due direzioni.
						counter += diagVal;
					} else {
						// La cella trovata non è uguale alla cella attualmente valutata; se la cella è prima della cella
						// attualmente valutata, azzero il punteggio e riparto. Se la cella è dopo la cella attuale, 
						// allora è bloccata verso destra e interrompo il ciclo dopo aver azzerato il punteggio.
						// Zero punti.
						counter = 0;
						if (index < 0) {break;}
					}
				}
			}

			cellPoints += counter;
			counter = 0;
			
			if (C.state == CXCellState.P1) {
				p1Score += cellPoints;
			} else {
				p2Score += cellPoints;
			}
		}

		if (this.isFirst ) {
			return p1Score - p2Score;
		} else {
			return p2Score - p1Score;
		}
	}

	// Ottengo un punteggio per ogni mossa, ordino le mosse in ordine di punteggio, e ritorno la mossa più efficace.
					// List<Integer> moves = new ArrayList<>(Arrays.asList(Q));
					// moves.sort(Comparator.comparingInt(col -> {
					// 	CXBoard newB = B.copy();
					// 	newB.markColumn(col);
					// 	int score;
					// 	try {
					// 		score = evaluateMove(newB, col, false, depth, Integer.MIN_VALUE, Integer.MAX_VALUE);
					// 	} catch (TimeoutException e) {
					// 		score = -1000;
					// 	}
					// 	return score;
					// }));
					// Collections.reverse(moves);

					// orderedMoves = moves.stream().mapToInt(Integer::intValue).toArray();

					// this.bestCol = orderedMoves[0];


	public class Pair<K, V> {
		private final K key;
		private final V value;
	
		public Pair(K key, V value) {
			this.key = key;
			this.value = value;
		}
	
		public K getKey() {
			return key;
		}
	
		public V getValue() {
			return value;
		}
	}


	public int iterativeDeepening(CXBoard B, int depth) throws TimeoutException {

		// Io sono il giocatore che massimizza
		int bestScore = Integer.MIN_VALUE;
		
		Integer[] Q = B.getAvailableColumns();
		int nextMove = Q[rand.nextInt(Q.length)];
		int[] scores = new int[Q.length];

		try {

			long MoveStartTime = System.currentTimeMillis();

			for (int d = 1; d <= depth; d++) {
				// Ci sono due implmenetazioni dell'array: array classico e array-lista
				// (preferibile) che ordina già le mosse dalla migliore alla peggiore.
				// Commentare una delle due parti perchè fanno la stessa cosa.

				// INIZIO IMPLEMENTAZIONE PSEUDO-ARRAY ORDINATO AUTOMATICAMENTE
				// checktime();

				// int dd = d;
				// List<Integer> moves = new ArrayList<>(Arrays.asList(Q));
				// moves.sort(Comparator.comparingInt(col -> {
				// 	B.markColumn(col);
				// 	int score;

				// 	try {
				// 		score = evaluateMove(B, false, dd - 1, Integer.MIN_VALUE, Integer.MAX_VALUE);
				// 		B.unmarkColumn();
				
				// 	} catch (TimeoutException e) {
				// 		score = -1000;
				// 	}
				// 	return this.currentBestMove;
				// }));
				// Collections.reverse(moves);

				// this.currentBestMove = moves.get(0);

				// FINE IMPLEMENTAZIONE PSEUDO-ARRAY ORDINATO AUTOMATICAMENTE


				// INIZIO IMPLEMENTAZIONE ARRAY CLASSICO NON ORDINATO

				checktime();
				for (int i = 0; i < Q.length; i++) {
					B.markColumn(Q[i]);
					// Metto falso perchè avendo giocato la mia mossa simulata, è il turno dell'avversario.
					scores[i] = evaluateMove(B, false, d, Integer.MIN_VALUE, Integer.MAX_VALUE);
					B.unmarkColumn();
				}

				
				// Creare una lista di coppie (Q[i], scores[i])
				List<Pair<Integer, Integer>> pairList = new ArrayList<>();
				for (int i = 0; i < Q.length; i++) {
					pairList.add(new Pair<>(Q[i], scores[i]));
				}
	
				// Ordinare la lista in base al punteggio in ordine decrescente
				pairList.sort((a, b) -> b.getValue().compareTo(a.getValue()));
	
				// Aggiornare gli array Q e scores con gli elementi ordinati
				for (int i = 0; i < pairList.size(); i++) {
					Pair<Integer, Integer> pair = pairList.get(i);
					Q[i] = pair.getKey();
					scores[i] = pair.getValue();
				}
	
				// Aggiornare il valore di nextMove e bestScore
				/*if (scores[0] > bestScore) {
					nextMove = Q[0];
					bestScore = scores[0];
				}*/

				if ( d > this.maxDepthReached) {
					this.maxDepthReached = d;
					System.out.println("Raggiunta " + this.maxDepthReached + " dopo " + (System.currentTimeMillis() - MoveStartTime));

				}

				this.currentBestMove = Q[0];

				// FINE IMPLEMENTAZIONE ARRAY CLASSICO NON ORDINATO
					
			}

		} catch (TimeoutException e) {
			return this.currentBestMove; //orderedMoves[0];
		}

		return this.currentBestMove;
		// return nextMove; //orderedMoves[0];

	}


	// Algoritmo alpha-beta pruning di ricerca della mossa migliore.
	private int evaluateMove(CXBoard B, boolean maxPlayer, int depth, int alpha, int beta) throws TimeoutException {
		checktime();
		if (B.gameState() == myWin) {
			return Integer.MAX_VALUE; // Vittoria immediata
		}
		if (B.gameState() == yourWin) {
			return Integer.MIN_VALUE; // Sconfitta immediata
		}
		if (B.gameState() == CXGameState.DRAW) {
			return 0;
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
				B.markColumn(move);
				int score = evaluateMove(B, false, depth - 1, alpha, beta);
				B.unmarkColumn();
				bestScore = Math.max(bestScore, score);
				if (bestScore == Integer.MAX_VALUE) break;
				alpha = Math.max(alpha, score);
				if (beta <= alpha) {
					break; // Beta cut-off
				}
			}
		} else {
			bestScore = Integer.MAX_VALUE;
			Integer[] moves = B.getAvailableColumns();
			for (int move : moves) {
				B.markColumn(move);
				int score = evaluateMove(B, true, depth - 1, alpha, beta);
				B.unmarkColumn();
				bestScore = Math.min(bestScore, score);
				if (bestScore == Integer.MIN_VALUE) break;

				beta = Math.min(beta, score);
				if (beta <= alpha) {
					break; // Alpha cut-off
				}
			}
		}
	
		return bestScore;
	}

    public String playerName() {
        return "ALPlayer";
    }
}

