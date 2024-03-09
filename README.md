# CXgame-algoritmi

To compile the project, run

```
javac -cp ".." *.java */*.java
```

inside the 'connectx/' folder.

To launch the game, read instructions inside the 'connectx/README.md' file.

## Description
This is a group project completed for the Algorithm course of the UNIBO Computer Science Degree, Bologna, Italy.

The goal is to optimize the choiche of the moves in a Connect-x game.

There are 3 parameters that define a game:
- m: number of rows
- n: number of columns
- x: number of consecutive pieces to connect to win the game.

We used an optimized iterative deepening strategy to search the next best move evaluating the reached positions using a custom algorithm.
 
