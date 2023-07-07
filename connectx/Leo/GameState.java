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

public class GameState {
    private CXBoard board;
    private int eval;
    private GameState[] children;
    private int depth;

    public GameState(CXBoard B, int DEPTH) {
        this.board = B;
        this.eval = depth == 0 ? evaluatePosition() : 0;
        this.children = new GameState[B.N];
        this.depth = DEPTH;
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


    public void printEvals(GameState G, int d, int col) {
        if (G != null)  {
            System.out.println(eval + " per la mossa " + col + " a profondità " + d);
            for (int i = 0; i < G.children.length; i++) {
                G.printEvals(G.children[i], d + 1, i);
            }
        } 
    }

    public void getGameStateAtPosition(GameState G, int d, int[] arr) {
        int i = 0;
        GameState newG = new GameState(G.board, d);
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

    public GameState[] getChildren() {
        return this.children;
    }

    // SETTERS
    public void setValue(int value) {
        this.eval = value;
    }


    public void setChild(CXBoard B, int col, int depth) {
        if (!B.fullColumn(col)) {
            this.children[col] = new GameState(B, depth);
        } else {
            this.children[col] = null;
        }
    }


}
