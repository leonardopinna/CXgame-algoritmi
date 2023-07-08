package connectx.ZZZ_Altro.GameTree;
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

public class ALPlayerOld implements CXPlayer {
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
	private int best; // tiene traccia della best col fino a questo momento.

	// GameTree parametri
	private GameState gamestate;
	

    /* Default empty constructor */
    public ALPlayerOld() {
    }

    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        this.rand = new Random(System.currentTimeMillis());
        this.myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
        this.yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
        this.TIMEOUT = timeout_in_secs;


		// Parametri aggiuntivi per il player
        this.MAX_DEPTH = 5; 
        this.columns = N;
        this.k = K;
        this.rows = M;
		this.isFirstPlayer=first;
		this.best = rand.nextInt(N);

		// Parametri gametree
		this.gamestate = null;
    }

    private void checktime() throws TimeoutException {
        if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
            throw new TimeoutException();
    }

   //metodo principale di selezione di una colonna, da getbestmove partono gli altri metodi
    public int selectColumn(CXBoard B) {
        this.START = System.currentTimeMillis();

		this.best = rand.nextInt(B.getAvailableColumns().length);
        
		// gametree integration

		// Se è la prima mossa, genero il gametree
		if (this.gamestate == null) {
			// try {
				gamestate = new GameState(B, MAX_DEPTH, null, !isFirstPlayer, -1);
				//gamestate.generateGameState(MAX_DEPTH);
			// } catch (NullPointerException e) {
			// 	System.err.println("Ops");
			// }
			
		} else {
			// Se non è la prima mossa, scorro il gametree entro le ultime due mosse.
			CXCell[] arr = B.getMarkedCells();
			if (gamestate.getChildren()[arr[arr.length - 2].j] == null || gamestate.getChildren()[arr[arr.length - 1].j] == null) {
				gamestate = new GameState(B, MAX_DEPTH, null, !isFirstPlayer, -1);
				// gamestate.generateGameState(MAX_DEPTH);
				// System.err.println("Ops2 ");
			} else {
				gamestate = gamestate.getChildren()[arr[arr.length - 2].j].getChildren()[arr[arr.length - 1].j];
				// System.err.println("YEEEE ");
			}
			
			

			
		}


		// end of gametree integration

        try {
			if (B.getMarkedCells().length < 2) {
				return (B.N) / 2;
			}
			
            int col = iterativeDeepening(gamestate);
            return col;
        } catch (TimeoutException e) {
            System.err.println("Timeout!");
            return this.best;
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
		
	private int evaluatePosition(GameState G) throws TimeoutException {

		CXBoard B = G.getBoard();

		// Inizializzazione dei punteggi dei giocatori
		int p1Score = 0;
		int p2Score = 0;

		// Parametri di assegnazione punteggio
		int emptyVal = 5;
		int columnVal = 35;
		int rowVal = 20;
		int diagVal= 20;

		// Versione migliorata di valutazione prendendo le celle.
		for (CXCell C : B.getMarkedCells()) {
			checktime();
			int x = C.i;
			int y = C.j;
			int cellPoints = 0;
			int counter = 0;

			// if (G.isBlocked(x, y)) {System.out.println("Bloccata");continue;}
			// Valutazione della colonna (verso l'alto)
			for (int index =  - getK() + 1 ; index < getK(); index++) {
				
				// Arrivo oltre il limite superiore della matrice. Interrompo la ricerca per colonna.
				if ( x - index  <= -1) {
					// Zero punti perchè non posso fare molto.
					counter = 0;
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
			for (int index =  - getK() + 1 ; index < getK(); index++) {
				
				// Arrivo oltre il limite destro della matrice. Interrompo la ricerca.
				if ( y + index  >= getColumns()) {
					// Counter non è azzerato perchè posso avere avuto punteggio nelle celle precedenti.
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
						// allora è bloccata verso destra e interrompo il ciclo dopo aver azzerato il punteggio.
						// Zero punti.
						counter = 0;
						if (index > 0) {break;}
					}
				}
			}

			cellPoints += counter;
			counter = 0;

			// Valutazione della riga (dx verso sx)
			for (int index = - getK() + 1; index < getK(); index++) {
				
				// Arrivo oltre il limite sinistro della matrice. Interrompo la ricerca.
				if ( y - index  <= -1) {
					// Counter non è azzerato perchè posso avere avuto punteggio nelle celle precedenti.
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
						// allora è bloccata verso destra e interrompo il ciclo dopo aver azzerato il punteggio.
						// Zero punti.
						counter = 0;
						if (index > 0) {break;}
					}
				}
			}

			cellPoints += counter;
			counter = 0;

			// Valutazione della diagonale (alto-sx verso basso-dx)
			for (int index = - getK() + 1; index < getK(); index ++ ) {
				
				// Arrivo oltre il limite destro o fondo della matrice. Interrompo la ricerca.
				if ( y + index  >= getColumns() || x + index >= getRows()) {
					// Counter non è azzerato perchè posso avere avuto punteggio nelle celle precedenti.
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
						// allora è bloccata verso destra e interrompo il ciclo dopo aver azzerato il punteggio.
						// Zero punti.
						counter = 0;
						if (index > 0) {break;}
					}
				}
			}

			cellPoints += counter;
			counter = 0;

			// Valutazione della diagonale (alto-dx verso basso-sx)
			for (int index = - getK() + 1; index < getK(); index ++ ) {
				
				// Arrivo oltre il limite sinistro o inferiore della matrice. Interrompo la ricerca.
				if ( y - index  < 0 || x + index >= getRows()) {
					// Counter non è azzerato perchè posso avere avuto punteggio nelle celle precedenti.
					break; 
				}

				// Se non parto oltre la destra o sopra della matrice, oppure nella cella stessa, procedo alla valutazione.
				if (!((y - index >= getColumns()) || ( x + index < 0))) {
					if (B.cellState(x + index, y - index) == CXCellState.FREE) {
						// La cella è libera: vuol dire che potenzialmente posso continuare la streak
						counter += emptyVal;
					} else if (B.cellState(x + index, y - index) == B.cellState(x,y) ) {
						// La cella è dello stesso giocatore della cella valutata: ottengo molti punti.
						// Ottengo meno punti della colonna perchè valuto due direzioni.
						counter += diagVal;
					} else {
						// La cella trovata non è uguale alla cella attualmente valutata; se la cella è prima della cella
						// attualmente valutata, azzero il punteggio e riparto. Se la cella è dopo la cella attuale, 
						// allora è bloccata verso destra e interrompo il ciclo dopo aver azzerato il punteggio.
						// Zero punti.
						counter = 0;
						if (index > 0) {break;}
					}
				}
			}

			cellPoints += counter;
			counter = 0;

			// Valutazione della diagonale (basso-sx verso alto-dx)
			for (int index = - getK() + 1; index < getK(); index ++ ) {
				
				// Arrivo oltre il limite superiore o destro della matrice. Interrompo la ricerca.
				if ( y + index  >= getColumns() || x - index < 0 ) {
					// Counter non è azzerato perchè posso avere avuto punteggio nelle celle precedenti.
					break; 
				}

				// Se non parto oltre la sinistra o il basso della matrice, oppure nella cella stessa, procedo alla valutazione.
				if (!((y + index < 0) || ( x - index >= getRows()))) {
					if (B.cellState(x - index, y + index) == CXCellState.FREE) {
						// La cella è libera: vuol dire che potenzialmente posso continuare la streak
						counter += emptyVal;
					} else if (B.cellState(x - index, y + index) == B.cellState(x,y) ) {
						// La cella è dello stesso giocatore della cella valutata: ottengo molti punti.
						// Ottengo meno punti della colonna perchè valuto due direzioni.
						counter += diagVal;
					} else {
						// La cella trovata non è uguale alla cella attualmente valutata; se la cella è prima della cella
						// attualmente valutata, azzero il punteggio e riparto. Se la cella è dopo la cella attuale, 
						// allora è bloccata verso destra e interrompo il ciclo dopo aver azzerato il punteggio.
						// Zero punti.
						counter = 0;
						if (index > 0) {break;}
					}
				}
			}

			cellPoints += counter;
			counter = 0;

			// Valutazione della diagonale (basso-dx verso alto-sx)
			for (int index = - getK() + 1; index < getK(); index ++ ) {
				
				// Arrivo oltre il limite sinistro o superiore della matrice. Interrompo la ricerca.
				if ( y - index  < 0 || x - index < 0) {
					// Counter non è azzerato perchè posso avere avuto punteggio nelle celle precedenti.
					break; 
				}

				// Se non parto da oltre la destra o il basso della matrice, oppure nella cella stessa, procedo alla valutazione.
				if (!((y - index >= getColumns()) || ( x - index >= getRows()))) {
					if (B.cellState(x - index, y - index) == CXCellState.FREE) {
						// La cella è libera: vuol dire che potenzialmente posso continuare la streak
						counter += emptyVal;
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
			
			// Se non sono stati assegnati punti alla cella, la cella viene marcata come bloccata e non sarà più rivalutata.
			if (cellPoints == 0) {
				G.setBlockedCell(x,y, true);
			}

			if (C.state == CXCellState.P1) {
				p1Score += cellPoints;
			} else {
				p2Score += cellPoints;
			}
		}

		// Il nodo della posizione corrente è stato valutato e non sarà più rivalutato.
		G.setEvaluated(true);
		

		if (this.isFirstPlayer) {
			G.setEval(p1Score - p2Score);
			return G.getEval();
		} else {
			G.setEval(p2Score - p1Score);
			return G.getEval();
		}
	}

	public int iterativeDeepening(GameState G) throws TimeoutException {
		int alpha = Integer.MIN_VALUE;
		int beta = Integer.MAX_VALUE;
		
		alphaBeta(G, MAX_DEPTH, alpha, beta);

		return G.getBestMove();







		// }
		// // for (int d = 1; d <= depth; d++) {
		// 	try {
		// 		checktime();
	
		// 		// Ottengo un punteggio per ogni mossa, ordino le mosse in ordine di punteggio, e ritorno la mossa più efficace.
		// 	// 	List<Integer> moves = new ArrayList<>(Arrays.asList(Q));
		// 	// 	moves.sort(Comparator.comparingInt(col -> {
		// 	// 		G.createChild(col);
		// 	// 		int score;
		// 	// 		try {
		// 	// 			score = alphaBeta(gamestate.getChildren()[col], col, false, G.getDepth(), Integer.MIN_VALUE, Integer.MAX_VALUE);
		// 	// 		} catch (TimeoutException e) {
		// 	// 			score = -1000;
		// 	// 		} catch (NullPointerException e ) {
		// 	// System.err.println("orrore"); return 0;}
		// 	// 		return score;
		// 	// 	}));
		// 	// 	Collections.reverse(moves);

		// 	// 	orderedMoves = moves.stream().mapToInt(Integer::intValue).toArray();

		// 	// 	this.bestCol = orderedMoves[0];


		// 	} catch (TimeoutException e) {
		// 		return orderedMoves[0];
		// 	} catch (NullPointerException e ) {
		// 	System.err.println("eorrore");
		// 	return 0;
		// }
		// // }

		// return orderedMoves[0];

	}

	// Algoritmo alpha-beta pruning di ricerca della mossa migliore.
	// private int evaluateMove(CXBoard B, int column, boolean maxPlayer, int depth, int alpha, int beta) throws TimeoutException {
	// 	checktime();
	// 	if (B.gameState() == myWin) {
	// 		return Integer.MAX_VALUE; // Vittoria immediata
	// 	}
	// 	if (B.gameState() == yourWin) {
	// 		return Integer.MIN_VALUE; // Sconfitta immediata
	// 	}
	// 	if (depth == 0) {
	// 		// Raggiunto il limite di profondità della ricerca, valuta la posizione corrente
	// 		return evaluatePosition(B);
	// 	}
	
	// 	int bestScore;
	// 	if (maxPlayer) {
	// 		bestScore = Integer.MIN_VALUE;
	// 		Integer[] moves = B.getAvailableColumns();
	// 		for (int move : moves) {
	// 			CXBoard newB = B.copy();
	// 			newB.markColumn(move);
	// 			int score = evaluateMove(newB, move, false, depth - 1, alpha, beta);
	// 			bestScore = Math.max(bestScore, score);
	// 			alpha = Math.max(alpha, score);
	// 			if (beta <= alpha) {
	// 				break; // Beta cut-off
	// 			}
	// 		}
	// 	} else {
	// 		bestScore = Integer.MAX_VALUE;
	// 		Integer[] moves = B.getAvailableColumns();
	// 		for (int move : moves) {
	// 			CXBoard newB = B.copy();
	// 			newB.markColumn(move);
	// 			int score = evaluateMove(newB, move, true, depth - 1, alpha, beta);
	// 			bestScore = Math.min(bestScore, score);
	// 			beta = Math.min(beta, score);
	// 			if (beta <= alpha) {
	// 				break; // Alpha cut-off
	// 			}
	// 		}
	// 	}
	
	// 	return bestScore;
	// }


	// Algoritmo alpha-beta pruning di ricerca della mossa migliore.
	private int alphaBeta(GameState G, int depth, int alpha, int beta) throws TimeoutException {
		checktime();
		if (G.getBoard().gameState() == myWin) {
			return Integer.MAX_VALUE; // Vittoria immediata
		}
		if (G.getBoard().gameState() == yourWin) {
			return Integer.MIN_VALUE; // Sconfitta immediata
		}
		if (depth == 0) {

			// Raggiunto il limite di profondità della ricerca, valuta la posizione corrente
			if (G.isEvaluated()) {
				return G.getEval();
			} else {
				return evaluatePosition(G);}
		}
	
		int bestScore;
		if (G.isFirst()) {
			bestScore = Integer.MIN_VALUE;
			Integer[] moves = G.getBoard().getAvailableColumns();
			try 
			{for (int move : moves) {
				
				G.createChild(move);
				GameState currentG = G.getChildren()[move];
				currentG.setEval(alphaBeta(G.getChildren()[move], depth - 1, alpha, beta));
				if (currentG.getEval() > bestScore) {bestScore = currentG.getEval();}
				G.setEval(Math.max(bestScore, currentG.getEval()));
				// if (move == 6) System.out.println("Valuto la mossa: " + move + " a profondità " + depth + ". Il punteggio attuale del padre (che massimizza) è " + G.getEval());
				alpha = Math.max(alpha, currentG.getEval());
	
				if (beta <= alpha) {
					break; // Beta cut-off
				}
			}} catch (NullPointerException e) {
				System.err.println("Qui");
			}
		} else {
			bestScore = Integer.MAX_VALUE;
			Integer[] moves = G.getBoard().getAvailableColumns();
			try {for (int move : moves) {
				G.createChild(move);
				GameState currentG = G.getChildren()[move];
				currentG.setEval(alphaBeta(G.getChildren()[move], depth - 1, alpha, beta));
				if (currentG.getEval() < bestScore) {bestScore = currentG.getEval();}
				G.setEval(Math.min(bestScore, currentG.getEval()));
				// if (move == 6) System.out.println("Valuto la mossa: " + move + " a profondità " + depth + ". Il punteggio attuale del padre (che minimizza) è " + G.getEval());

				beta = Math.min(beta, currentG.getEval());
	
				if (beta <= alpha) {
					break; // Beta cut-off
				}
			}} catch (NullPointerException e) {
				System.err.println("Qui");
			}
		}
		
		// System.out.println("Aggiorno: " + bestScore + " a profondità " + depth);
		return bestScore;
	}
    public String playerName() {
        return "ALPlayer";
    }
}

