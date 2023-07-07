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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class GameTree {
    private CXBoard board;
    private int eval;
    private GameTree[] children;
    private int depth;

    public GameTree(CXBoard B, int DEPTH) {
        this.board = B;
        this.eval = evaluatePosition();
        this.children = new GameTree[B.N];
        this.depth = DEPTH;
        if (DEPTH > 0) {
            for (int i = 0; i < this.children.length; i++) {
                if (this.board.gameState() == CXGameState.OPEN) {
                    if (B.fullColumn(i)) {
                        // Mossa non possibile: figlio messo null
                        this.children[i] = null;
                    } else if (this.children[i] == null && !B.fullColumn(i)) {
                        // Mossa ancora non valutata e colonna disponibile: creo una nuova valutazione
                        CXBoard newB = this.board.copy();
                        newB.markColumn(i);
                        this.children[i] = new GameTree(newB, this.depth - 1);
                        getGameStateAtPosition(this.children[i], this.depth - 1, new int[]{i});
                    } else {
                        // Mossa già valutata e da aggiornare come profondità
                        this.children[i].updateTree();
                    }
                } else {
                    // Stato di gioco WIN o DRAW: non c'è il sottoramo
                    this.children[i] = null;
                }
            }
        }
    }


    public void updateTree() {
        // Questa funzione ricalcola i figli di un gametree dopo aver aggiornato la 
        // profondità massima di valutazione (ad esempio, quando una mossa è stata 
        // fatta e l'albero di quella mossa è già stato calcolato)
    }


    public int selectColumn() {
        // Questa funzione ritorna la colonna migliore
        return new Random(System.currentTimeMillis()).nextInt(this.board.N);

    }


    public int evaluatePosition() {
        // Questa funzione valuta la posizione attuale di gioco
        return 0;
    }


    public int minimax() {
        // Questa funzione ritorna la colonna migliore valutando le mosse secondo un algoritmo minimax. 
        // IDEALMENTE questa funzione è integrata nella creazione del gameTree.
        return 0;
    }


    public void printEvals(GameTree G, int d, int col) {
        if (G != null)  {
            System.out.println(eval + " per la mossa " + col + " a profondità " + d);
            for (int i = 0; i < G.children.length; i++) {
                G.printEvals(G.children[i], d + 1, i);
            }
        } 
    }

    public void getGameStateAtPosition(GameTree G, int d, int[] arr) {
        int i = 0;
        GameTree newG = new GameTree(G.board, d);
        while (G.children[arr[i]] != null && i < arr.length - 1) {
            newG = G.children[arr[i]];
            i = i + 1;
        }
        CXCell[] sequence = newG.board.getMarkedCells();
        int j = 0;
        for (CXCell cell : sequence) {
            System.out.println("Mossa n° " + j + " : " + "( " + cell.i + ", " + cell.j+ ")");
            j = j + 1;
        }
    }

        
    
    
    // GETTERS
    public int getValue() {
        return this.eval;
    }

    public GameTree[] getChildren() {
        return this.children;
    }

    // SETTERS
    public void setValue(int value) {
        this.eval = value;
    }


    public void setChild(CXBoard B, int col, int depth) {
        if (!B.fullColumn(col)) {
            this.children[col] = new GameTree(B, depth);
        } else {
            this.children[col] = null;
        }
    }


}
