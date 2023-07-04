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

/**
 * Totally random software player.
 */
public class ALPlayer implements CXPlayer {
    private Random rand;
    private CXGameState myWin;
    private CXGameState yourWin;
    private int TIMEOUT;
    private int MAX_DEPTH;
    private long START;
    private int columns;
    private int k;
    private int rows;
	private boolean isFirstPlayer; //booleano che tiene traccia della prima mossa

    /* Default empty constructor */
    public ALPlayer() {
    }

    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        rand = new Random(System.currentTimeMillis());
        myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
        yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
        TIMEOUT = timeout_in_secs;
        MAX_DEPTH = 8; //se fissata a 7, sembra non dare alcun timeout
        columns = N;
        k = K;
        rows = M;
		isFirstPlayer=first;
    }

    private void checktime() throws TimeoutException {
        if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
            throw new TimeoutException();
    }

   //metodo principale di selezione di una colonna, da getbestmove partono gli altri metodi
    public int selectColumn(CXBoard B) {
        START = System.currentTimeMillis();
        Integer[] L = B.getAvailableColumns();
        int save = L[rand.nextInt(L.length)];


        if (isFirstPlayer && columns % 2 == 0) {  // Controlla se è il primo a giocare e il numero di colonne è pari
            int middleCol = columns / 2;
            if (Arrays.asList(L).contains(middleCol)) {
                isFirstPlayer = false;  // Aggiorna 'isFirstPlayer' a 'false' dopo la prima mossa
                return middleCol;
            }
        }
		else if (isFirstPlayer && columns % 2 != 0){ //controlla se se è il primo giocatore e il numero di colonne è dispari
			int middleCol = (columns / 2)+1;
            if (Arrays.asList(L).contains(middleCol)) {
                isFirstPlayer = false;  // Aggiorna 'isFirstPlayer' a 'false' dopo la prima mossa
                return middleCol;
		}
	}
        try {
            int col = getBestMove(B, MAX_DEPTH);
            return col;
        } catch (TimeoutException e) {
            System.err.println("Timeout!!! Random column selected");
            return save;
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
	/* 
		//2 nuovi metodi di valutazione, il primo considera i "gettoni bloccati", il secondo valuta nuove potenziali linee
		//di vittoria
		private int evaluateBlockedTokens(CXBoard B) {
			int score = 0;
		
			for (int col = 0; col < getColumns(); col++) {
				int row = getRows() - 1; // Partenza dal basso
		
				// Controlla se la colonna è completamente piena
				if (B.cellState(row, col).state != CXCellState.FREE) {
					continue; // Ignora la colonna se è piena
				}
		
				int blockedTokens = 0;
		
				// Conta i gettoni bloccati nella colonna
				while (row > 0 && B.cellState(row - 1, col).state != CXCellState.FREE) {
					blockedTokens++;
					row--;
				}
		
				// Assegna un punteggio in base al numero di gettoni bloccati
				if (blockedTokens > 0) {
					score -= blockedTokens * blockedTokens;
				}
			}
		
			return score;
		}
//DA CONTROLLARE SE CON CXCELLSTATE.P1 SI RIFERISCE AL NOSTRO GIOCATORE, OPPURE AL PRIMO CHE GIOCA
		private int evaluatePotentialWinLines(CXBoard B) {
			int score = 0;
		
			// Valutazione delle righe
			for (int row = 0; row < getRows(); row++) {
				for (int col = 0; col <= getColumns() - getK(); col++) {
					int emptyCount = 0;
					int playerCount = 0;
		
					for (int i = 0; i < getK(); i++) {
		
						if (B.cellState(row, col + i).state == CXCellState.FREE) {
							emptyCount++;
						} else if (B.cellState(row, col + i).state == CXCellState.P1) {
							playerCount++;
						} else {
							playerCount = 0;
							break;
						}
					}
		
					if (emptyCount > 0 && playerCount > 0) {
						score += calculateLineScore(playerCount, emptyCount);
					}
				}
			}
		
			// Valutazione delle colonne
			for (int col = 0; col < getColumns(); col++) {
				for (int row = 0; row <= getRows() - getK(); row++) {
					int emptyCount = 0;
					int playerCount = 0;
		
					for (int i = 0; i < getK(); i++) {
		
						if (B.cellState(row + i, col).state == CXCellState.FREE) {
							emptyCount++;
						} else if (B.cellState(row + i, col).state == CXCellState.P1) {
							playerCount++;
						} else {
							playerCount = 0;
							break;
						}
					}
		
					if (emptyCount > 0 && playerCount > 0) {
						score += calculateLineScore(playerCount, emptyCount);
					}
				}
			}
		
			// Valutazione delle diagonali ascendenti
			for (int row = getK() - 1; row < getRows(); row++) {
				for (int col = 0; col <= getColumns() - getK(); col++) {
					int emptyCount = 0;
					int playerCount = 0;
		
					for (int i = 0; i < getK(); i++) {
		
						if (B.cellState(row - i, col + i).state == CXCellState.FREE) {
							emptyCount++;
						} else if (B.cellState(row - i, col + i).state == CXCellState.P1) {
							playerCount++;
						} else {
							playerCount = 0;
							break;
						}
					}
		
					if (emptyCount > 0 && playerCount > 0) {
						score += calculateLineScore(playerCount, emptyCount);
					}
				}
			}
		
			// Valutazione delle diagonali discendenti
			for (int row = 0; row <= getRows() - getK(); row++) {
				for (int col = 0; col <= getColumns() - getK(); col++) {
					int emptyCount = 0;
					int playerCount = 0;
		
					for (int i = 0; i < getK(); i++) {
		
						if (B.cellState(row + i, col + i).state == CXCellState.FREE) {
							emptyCount++;
						} else if (B.cellState(row + i, col + i).state == CXCellState.P1) {
							playerCount++;
						} else {
							playerCount = 0;
							break;
						}
					}
		
					if (emptyCount > 0 && playerCount > 0) {
						score += calculateLineScore(playerCount, emptyCount);
					}
				}
			}
		
			return score;
		}
		
		private int calculateLineScore(int playerCount, int emptyCount) {
			// Calcola il punteggio in base al numero di gettoni del giocatore
			// e al numero di celle vuote nella linea.
			// QUESTO E'SOLO UN ESEMPIO CHE UTILIZZA UNA FORMULA SEMPLICE, MA RENDENDO
			//PIU' COMPLESSA LA FORMULA, SI POTREBBE MIGLIORARE IL RISULTATO
		
			// Ad esempio, si può assegnare un punteggio maggiore se il giocatore
			// ha più gettoni nella linea o se ci sono più celle vuote.
		
			return playerCount * playerCount * emptyCount;
		}

*/
    private int evaluatePosition(CXBoard B) {
        int score = 0;

        // Valutazione delle righe
        for (int row = 0; row < getRows(); row++) {
            for (int col = 0; col <= getColumns() - getK(); col++) {
                CXCell[] cells = getRowCells(row, col, getK(), B);
                score += evaluateSequence(cells);
            }
        }

        // Valutazione delle colonne
        for (int col = 0; col < getColumns(); col++) {
            for (int row = 0; row <= getRows() - getK(); row++) {
                CXCell[] cells = getColumnCells(col, row, getK(), B);
                score += evaluateSequence(cells);
            }
        }

        // Valutazione delle diagonali principali
        for (int row = 0; row <= getRows() - getK(); row++) {
            for (int col = 0; col <= getColumns() - getK(); col++) {
                CXCell[] cells = getDiagonalCells(row, col, getK(), 1, B);
                score += evaluateSequence(cells);
            }
        }

        // Valutazione delle diagonali secondarie
        for (int row = getK() - 1; row < getRows(); row++) {
            for (int col = 0; col <= getColumns() - getK(); col++) {
                CXCell[] cells = getDiagonalCells(row, col, getK(), -1, B);
                score += evaluateSequence(cells);
            }
        }

		    // Valutazione dei gettoni bloccati
			//score += evaluateBlockedTokens(B);

			// Valutazione delle potenziali linee di vittoria
			//score += evaluatePotentialWinLines(B);


        return score;
    }
	public CXCell[] getRowCells(int row, int col, int k, CXBoard B) {
		CXCell[] cells = new CXCell[k];
		for (int i = 0; i < k; i++) {
			int currentCol = col + i;
			CXCell cell = new CXCell(row, currentCol, B.cellState(row, currentCol));
			cells[i] = cell;
		}
		return cells;
	}
	
	public CXCell[] getColumnCells(int col, int row, int k, CXBoard B) {
		CXCell[] cells = new CXCell[k];
		for (int i = 0; i < k; i++) {
			int currentRow = row + i;
			CXCell cell = new CXCell(currentRow, col, B.cellState(currentRow, col));
			cells[i] = cell;
		}
		return cells;
	}
	
	public CXCell[] getDiagonalCells(int row, int col, int k, int direction, CXBoard B) {
		CXCell[] cells = new CXCell[k];
		for (int i = 0; i < k; i++) {
			int currentRow = row + i * direction;
			int currentCol = col + i;
			CXCell cell = new CXCell(currentRow, currentCol, B.cellState(currentRow, currentCol));
			cells[i] = cell;
		}
		return cells;
	}

    private int evaluateSequence(CXCell[] cells) {
        int score = 0;
        int myCount = 0;  // Numero di celle del giocatore corrente
        int yourCount = 0;  // Numero di celle dell'avversario

        for (CXCell cell : cells) {
            if (cell.state.equals(myWin)) {
                myCount++;
            } else if (cell.state.equals(yourWin)) {
                yourCount++;
            }
        }

        // Assegna un punteggio in base al numero di celle del giocatore corrente e dell'avversario
        if (myCount > 0 && yourCount == 0) {
            // Ci sono solo le celle del giocatore corrente
            score += Math.pow(10, myCount);
        } else if (yourCount > 0 && myCount == 0) {
            // Ci sono solo le celle dell'avversario
            score -= Math.pow(10, yourCount);
        }

        return score;
    }
//implementato un ordinamento delle mosse, così da esplorare prima le mosse migliori
//implementato un alpha-beta pruning
public int getBestMove(CXBoard B, int depth) throws TimeoutException {
    Integer[] Q = B.getAvailableColumns();
    int bestCol = Q[rand.nextInt(Q.length)];
    int bestScore = Integer.MIN_VALUE;

    boolean isFirstMoveDone = !isFirstPlayer || (isFirstPlayer && (columns % 2 == 0 || B.numOfMarkedCells() > 0));

    Arrays.sort(Q, Comparator.comparingInt(col -> {
        CXBoard newB = B.copy();
        newB.markColumn(col);
        int score;

        try {
            if (isFirstMoveDone && col == columns / 2) {
                score = evaluateMove(newB, col, false, depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE) / 2;
            } else {
                score = evaluateMove(newB, col, false, depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE);
            }
        } catch (TimeoutException e) {
            score = -1000;
        }

        return score;
    }));

    int alpha = Integer.MIN_VALUE;
    int beta = Integer.MAX_VALUE;

    for (int col : Q) {
        CXBoard newB = B.copy();
        newB.markColumn(col);
        int score;

        try {
            if (isFirstMoveDone && col == columns / 2) {
                score = evaluateMove(newB, col, false, depth - 1, alpha, beta) / 2;
            } else {
                score = evaluateMove(newB, col, false, depth - 1, alpha, beta);
            }
        } catch (TimeoutException e) {
            score = -1000;
        }

        if (score >= 100) {
            return col;
        }

        if (score > bestScore) {
            bestScore = score;
            bestCol = col;
        }

        // Aggiornamento di alpha e beta
        alpha = Math.max(alpha, bestScore);
        if (beta <= alpha) {
            break; // Effettua il pruning, interrompi il ciclo
        }
    }

    return bestCol;
}
	
	
	
	
	
	
	
//evaluateMove ora, implementa un alpha-beta pruning
	private int evaluateMove(CXBoard B, int column, boolean maxPlayer, int depth, int alpha, int beta) throws TimeoutException {
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

    private int singleMoveWin(CXBoard B, Integer[] L) throws TimeoutException {
        for (int i : L) {
            checktime(); // Check timeout at every iteration
            CXGameState state = B.markColumn(i);
            if (state == myWin)
                return i; // Winning column found: return immediately
            B.unmarkColumn();
        }
        return -1;
    }

    public String playerName() {
        return "ALPlayer";
    }
}