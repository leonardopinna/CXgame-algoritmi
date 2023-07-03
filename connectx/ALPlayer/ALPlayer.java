package connectx.ALPlayer;

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

    /* Default empty constructor */
    public ALPlayer() {
    }

    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        rand = new Random(System.currentTimeMillis());
        myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
        yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
        TIMEOUT = timeout_in_secs;
        MAX_DEPTH = 6;
        columns = N;
        k = K;
        rows = M;
    }

    private void checktime() throws TimeoutException {
        if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
            throw new TimeoutException();
    }

    /* Selects a random column */
    public int selectColumn(CXBoard B) {
        START = System.currentTimeMillis();
        Integer[] L = B.getAvailableColumns();
        int save = L[rand.nextInt(L.length)];

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

    public int getBestMove(CXBoard B, int depth) throws TimeoutException {
		Integer[] Q = B.getAvailableColumns();
		int bestCol = Q[rand.nextInt(Q.length)];
		int bestScore = Integer.MIN_VALUE;
	
		for (int col : Q) {
			CXBoard newB = B.copy();
			newB.markColumn(col);
			int score = evaluateMove(newB, col, false, depth - 1);
	
			if (score > bestScore) {
				bestScore = score;
				bestCol = col;
			}
		}
	
		return bestCol;
    }

    private int evaluateMove(CXBoard B, int column, boolean maxPlayer, int depth) throws TimeoutException {
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
				int score = evaluateMove(newB, move, false, depth - 1);
				bestScore = Math.max(bestScore, score);
			}
		} else {
			bestScore = Integer.MAX_VALUE;
			Integer[] moves = B.getAvailableColumns();
			for (int move : moves) {
				CXBoard newB = B.copy();
				newB.markColumn(move);
				int score = evaluateMove(newB, move, true, depth - 1);
				bestScore = Math.min(bestScore, score);
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
	public String playerName() {
		return "ALPlayer";
	}
}
