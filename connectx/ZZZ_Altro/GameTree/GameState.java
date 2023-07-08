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

import javax.swing.text.html.HTMLDocument.Iterator;

import connectx.CXCellState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class GameState extends TreeSet {
    private CXBoard board;
    private boolean evaluated;
    private int eval;
    private GameState[] children;
    private int depth;
    private GameState parent;
    private boolean[][] blockedCells;
    private boolean firstPlayer;
    private int prevCol;


    public GameState(CXBoard B, int DEPTH, GameState parent, boolean firstPlayer, int prevCol) {

        this.prevCol = prevCol;
        this.board = B;
        this.evaluated = false;
        this.eval = 0;
        this.children = new GameState[B.N];
        this.depth = DEPTH;
        this.parent = parent;
        this.firstPlayer = firstPlayer;
        this.blockedCells = new boolean[B.M][B.N];
        if (this.parent == null) {
            for (int i = 0; i < B.M; i++) {
                for (int j = 0; i < B.M; i++) {
                    this.blockedCells[i][j] = false;
                }
            }
        } else {
            this.blockedCells = parent.blockedCells;
        }       
    
    }

    public int getBestMove() {
        int bestCol = 0; 

        Integer[] arr = this.board.getAvailableColumns();
        int bestScore = this.isFirst() ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        int i = 0;
        for (int move : arr) {

            if (this.children[move] != null) {
                // System.err.println("punteggio " + this.children[move].eval + " per la mossa " + move);
                // System.err.println("yeah a profondità " + this.depth + " valuto il punteggio pari a " + this.children[move].eval + "per la mossa " + move);
                if (this.isFirst() && this.children[move].eval > bestScore) {
                    bestScore = this.children[move].eval; 
                    bestCol = i;
                } else if (!this.isFirst() && this.children[move].eval < bestScore) {
                    bestScore = this.children[move].eval; 
                    bestCol = i;
                }
            }
            i++;
        }

        return bestCol;

    }

    // Crea l'albero fino a profondità d
    // public void generateGameState(int d) {
    //     if (d == 0) {
    //         return;
    //     }
        
    //     Integer[] M = this.board.getAvailableColumns();

    //     for (int col : M) {
    //         CXBoard newB = this.board.copy();
    //         newB.markColumn(col);
    //         this.children[col] = new GameState(newB, d - 1, this, !this.firstPlayer);
    //     }
    // }
    
    public void updateTree() {
        // Questa funzione ricalcola i figli di un gametree dopo aver aggiornato la 
        // profondità massima di valutazione (ad esempio, quando una mossa è stata 
        // fatta e l'albero di quella mossa è già stato calcolato)
    }


    public int selectColumn() {
        // Questa funzione ritorna la colonna migliore
        return new Random(System.currentTimeMillis()).nextInt(this.board.N);

    }


    private int evaluatePosition(CXBoard B) {

		// Inizializzazione dei punteggi dei giocatori
		int p1Score = 0;
		int p2Score = 0;

		// Parametri di assegnazione punteggio
		int emptyVal = 5;
		int columnVal = 30;
		int rowVal = 20;
		int diagVal= 20;

		// Versione migliorata di valutazione prendendo le celle.
		for (CXCell C : B.getMarkedCells()) {
			// checktime();
			int x = C.i;
			int y = C.j;
			int cellPoints = 0;
			int counter = 0;

			// Valutazione della colonna (verso l'alto)
			for (int index =  - this.board.X + 1 ; index < this.board.X; index++) {
				
				// Arrivo oltre il limite superiore della matrice. Interrompo la ricerca per colonna.
				if ( x - index  <= -1) {
					// Zero punti perchè non posso fare molto.
					counter = 0;
					break; 
				}

				// Se non sono al di sotto della matrice, considero le pedine entro il range di valutazione.
				if (!(x - index >= this.board.M)) {
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
			for (int index =  - this.board.X + 1 ; index < this.board.X; index++) {
				
				// Arrivo oltre il limite destro della matrice. Interrompo la ricerca.
				if ( y + index  >= this.board.N) {
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
			for (int index = - this.board.X + 1; index < this.board.X; index++) {
				
				// Arrivo oltre il limite sinistro della matrice. Interrompo la ricerca.
				if ( y - index  <= -1) {
					// Counter non è azzerato perchè posso avere avuto punteggio nelle celle precedenti.
					break; 
				}

				// Se non parto oltre a destra della matrice, oppure nella cella stessa, procedo alla valutazione.
				if (!(y - index >= this.board.N)) {
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
			for (int index = - this.board.X + 1; index < this.board.X; index ++ ) {
				
				// Arrivo oltre il limite destro o fondo della matrice. Interrompo la ricerca.
				if ( y + index  >= this.board.N || x + index >= this.board.M) {
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
			for (int index = - this.board.X + 1; index < this.board.X; index ++ ) {
				
				// Arrivo oltre il limite sinistro o inferiore della matrice. Interrompo la ricerca.
				if ( y - index  < 0 || x + index >= this.board.M) {
					// Counter non è azzerato perchè posso avere avuto punteggio nelle celle precedenti.
					break; 
				}

				// Se non parto oltre la destra o sopra della matrice, oppure nella cella stessa, procedo alla valutazione.
				if (!((y - index >= this.board.N) || ( x + index < 0))) {
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
			for (int index = - this.board.X + 1; index < this.board.X; index ++ ) {
				
				// Arrivo oltre il limite superiore o destro della matrice. Interrompo la ricerca.
				if ( y + index  >= this.board.N || x - index < 0 ) {
					// Counter non è azzerato perchè posso avere avuto punteggio nelle celle precedenti.
					break; 
				}

				// Se non parto oltre la sinistra o il basso della matrice, oppure nella cella stessa, procedo alla valutazione.
				if (!((y + index < 0) || ( x - index >= this.board.M))) {
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
			for (int index = - this.board.X + 1; index < this.board.X; index ++ ) {
				
				// Arrivo oltre il limite sinistro o superiore della matrice. Interrompo la ricerca.
				if ( y - index  < 0 || x - index < 0) {
					// Counter non è azzerato perchè posso avere avuto punteggio nelle celle precedenti.
					break; 
				}

				// Se non parto da oltre la destra o il basso della matrice, oppure nella cella stessa, procedo alla valutazione.
				if (!((y - index >= this.board.N) || ( x - index >= this.board.M))) {
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
			
			if (C.state == CXCellState.P1) {
				p1Score += cellPoints;
			} else {
				p2Score += cellPoints;
			}
		}

        this.evaluated = true;

		if (this.firstPlayer) {
			return p1Score - p2Score;
		} else {
			return p2Score - p1Score;
		}
	}


    private int alphaBetaPruning(CXBoard B, int column, boolean maxPlayer, int depth, int alpha, int beta) throws TimeoutException {
		// checktime();
		if (B.gameState() == (this.firstPlayer ? CXGameState.WINP1 : CXGameState.WINP2)) {
			return Integer.MAX_VALUE; // Vittoria immediata
		}
		if (B.gameState() == (this.firstPlayer ? CXGameState.WINP2 : CXGameState.WINP1)) {
			return Integer.MIN_VALUE; // Sconfitta immediata
		}
		if (depth == 0) {
			// Raggiunto il limite di profondità della ricerca, valuta la posizione corrente
            if (!this.evaluated) {
                return evaluatePosition(B);
            } else {
                return this.eval;
            }
			
		}
	
		int bestScore;
		if (maxPlayer) {
			bestScore = Integer.MIN_VALUE;
			Integer[] moves = B.getAvailableColumns();
			for (int move : moves) {
				CXBoard newB = B.copy();
				newB.markColumn(move);
				int score = alphaBetaPruning(newB, move, false, depth - 1, alpha, beta);
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
				int score = alphaBetaPruning(newB, move, true, depth - 1, alpha, beta);
				bestScore = Math.min(bestScore, score);
				beta = Math.min(beta, score);
				if (beta <= alpha) {
					break; // Alpha cut-off
				}
			}
		}
	
		return bestScore;
	}


    public void printEvals(GameState G, int d, int col) {
        if (G != null)  {
            System.out.println(eval + " per la mossa " + col + " a profondità " + d);
            for (int i = 0; i < G.children.length; i++) {
                G.printEvals(G.children[i], d + 1, i);
            }
        } 
    }

    // public void getGameStateAtPosition(GameState G, int d, int[] arr) {
    //     int i = 0;
    //     GameState newG = new GameState(G.board, d, this, !this.firstPlayer);
    //     while (G.children[arr[i]] != null && i < arr.length - 1) {
    //         newG = G.children[arr[i]];
    //         i = i + 1;
    //     }
    //     CXCell[] sequence = newG.board.getMarkedCells();
    //     int j = 0;
    //     for (CXCell cell : sequence) {
    //         System.out.println("Mossa n° " + j + " : " + "( " + cell.i + ", " + cell.j+ ")");
    //         j = j + 1;
    //     }
    // }

        
    
    
    // GETTERS

    public CXBoard getBoard() {
        return this.board;
    }

    public GameState[] getChildren() {
        return this.children;
    }

    public int getEval() {
        return this.eval;
    }

    public boolean isEvaluated() {
        return this.evaluated;
    }

    public boolean isFirst() {
        return this.firstPlayer;
    }

    public int getDepth() {
        return this.depth;
    }


    public boolean isBlocked(int i, int j) {
        return this.blockedCells[i][j];
    }

    // SETTERS
    public void setEval(int value) {
        this.eval = value;
    }
    public void setEvaluated(boolean b) {
        this.evaluated = b;
    }

    public void createChild(int col) {
        if (!this.board.fullColumn(col)) {
            CXBoard newB = this.board.copy();
            newB.markColumn(col);
            this.children[col] = new GameState(newB, this.depth - 1, this, !this.firstPlayer, col);
        } 
        else {
            this.children[col] = null;
        }
    }

    public void setBlockedCell(int i, int j, boolean b) {
        this.blockedCells[i][j] = b;
    }
}
