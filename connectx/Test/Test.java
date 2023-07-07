
package connectx.Test;
import connectx.CXBoard;
import connectx.Leo.GameTree;

public class Test {
    public static void main(String[] args) {

        int DEPTH = 3;
        // Create a sample board
        CXBoard board = new CXBoard(6, 7, 4);

        // Create a game tree
        GameTree gameTree = new GameTree(board, DEPTH);

        // Set some values and children for the game tree
        gameTree.setValue(10);

        // Print the evaluation values of the game tree
        // gameTree.printEvals(gameTree, 0, -1);

        int[] arr = {1,1,1,3,4};
        gameTree.getGameStateAtPosition(gameTree, DEPTH, arr);
    }
}