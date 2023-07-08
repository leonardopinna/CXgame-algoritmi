package connectx.MonteCarlo;
import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCell;
import java.util.Random;
import java.util.concurrent.TimeoutException;
import connectx.CXCellState;

public class MonteCarlo implements CXPlayer {
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
	
    /* Default empty constructor */
    public MonteCarlo() {
    }

    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        this.rand = new Random(System.currentTimeMillis());
        this.myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
        this.yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
        this.TIMEOUT = timeout_in_secs;


		// Parametri aggiuntivi per il player
        this.MAX_DEPTH 	= 50; 
        this.columns 	= N;
        this.k 			= K;
        this.rows 		= M;
		this.isFirst 	= first;
		
    }

    private void checktime() throws TimeoutException {
        if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
            throw new TimeoutException();
    }

   //metodo principale di selezione di una colonna, da getbestmove partono gli altri metodi
    public int selectColumn(CXBoard B) {
        this.START = System.currentTimeMillis();
		Integer[] Q = B.getAvailableColumns();
		this.currentBestMove = Q[rand.nextInt(Q.length)];
        
        try {
			// La prima mossa viene giocata al centro
			if (B.getMarkedCells().length < 2) {
				return (B.N) / 2;
			}

			// Algoritmo di tipo Monte Carlo per ricerca della migliore mossa
            int col = monteCarloSearch(B, MAX_DEPTH);
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
			int columnVal 	= 35;
			int rowVal 		= 25;
			int diagVal		= 25;

			// Valutazione della colonna (verso l'alto)
			for (int index = - getK() + 1 ; index < getK(); index++) {
				
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
						// allora è bloccata e interrompo il ciclo dopo aver azzerato il punteggio.
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

	
	public int monteCarloSearch(CXBoard B, int depth) throws TimeoutException {
		// Inizializzo i parametri: mosse possibili e punteggio.
		int[] scores = new int[B.N];
		int bestScore = Integer.MIN_VALUE;
		Integer[] Q = B.getAvailableColumns();
		int nextMove = Q[rand.nextInt(Q.length)];

		try {
				checktime();

				// Per ogni mossa possibile, lancio l'algoritmo Monte Carlo di simulazione
				for (int i = 0; i < Q.length; i++) {
					B.markColumn(Q[i]);
					scores[i] = simulateMonteCarlo(B, false, depth); 
					B.unmarkColumn();
				}

				// Trovo la mossa migliore in base al punteggio calcolato
				for (int i = 0; i < Q.length; i++) {
					if (scores[i] > bestScore) {
						nextMove = Q[i];
						bestScore = scores[i];
					}
				}

				this.currentBestMove = nextMove;

		} catch (TimeoutException e) {
			// Se va in timeout, ritorna una mossa casuale
			return this.currentBestMove;
		}

		return this.currentBestMove;
	}	

	// Simula un numero elevato di partite facendo mosse casuali e ritorna il punteggio complessivo
	private int simulateMonteCarlo(CXBoard B, boolean isFirst, int depth)  throws TimeoutException{

		// Parametro: numero di simulazioni per partita. Più è alto, meglio è.
		int simulations = 2000; // Number of Monte Carlo simulations to perform

		// Inizializzo
		int totalScore = 0;

		for (int i = 0; i < simulations; i++) {
			// Perform a random playout from the current game state
			int score = performRandomPlayout(B, isFirst, depth);
			totalScore += score;
		}

		// Return the average score of the Monte Carlo simulations
		return totalScore / simulations;
	}

	private int performRandomPlayout(CXBoard B, boolean isFirst, int depth) throws TimeoutException {
		// Perform a random playout from the current game state until reaching the desired depth or a terminal state
		// Use random moves or any other playout strategy suitable for your game
		// Return the final score of the playout
		// return 0; // Placeholder; replace with your implementation
		// Create a copy of the game board to perform the playout
		CXBoard playoutBoard = B.copy();

		boolean isMaxPlayer = isFirst;

		while (depth > 0 && playoutBoard.gameState() == CXGameState.OPEN) {
			// Get all available legal moves in the current state
			Integer[] availableMoves = playoutBoard.getAvailableColumns();

			if (availableMoves.length == 0) {
				// No legal moves available, the game is a draw
				break;
			}

			// Choose a random move from the available legal moves
			int randomMove = availableMoves[new Random().nextInt(availableMoves.length)];

			// Make the selected move on the board
			playoutBoard.markColumn(randomMove);

			// Switch to the next player
			isMaxPlayer = !isMaxPlayer;

			depth--;
		}

		// Calculate the score based on the terminal state
		int winPoints = 50;
		
		if (playoutBoard.gameState() != CXGameState.OPEN) {
			if (playoutBoard.gameState() == CXGameState.WINP1) {
				if (!this.isFirst) return -winPoints; else return winPoints;
			} else if (playoutBoard.gameState() == CXGameState.WINP2) {
				if (!this.isFirst) return winPoints; else return -winPoints;
			} else {
				// The game is a draw
				return 0;
			}
		} else {
			// If the playout was stopped before reaching a terminal state, use a heuristic to estimate the score
			return evaluatePosition(playoutBoard); // Implement your own heuristic evaluation function}
		}
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
        return "MonteCarlo";
    }
}

