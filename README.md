# CXgame-algoritmi

Per compilare i file java, lanciare il comando

```
javac -cp ".." *.java */*.java
```

all'interno della cartella connectx/.

Per lanciare il gioco, leggere il README.md presente nella cartella connectx/.

## To Do List

### Funzioni attualmente in sviluppo

#### Creare l'algoritmo Minimax con Iterative Deepening

L'algoritmo minimax prende in ingresso la posizione corrente e il giocatore che deve giocare, valuta la posizione attuale e assegna il valore in funzione dei valori migliori/peggiori delle posizioni successive.
Con Iterative Deepening, Minimax viene eseguito di volta in volta ad una profondità sempre maggiore
IDEA: Iterative Deepening troncato che valuta solamente al massimo le X mosse migliori per ogni livello, perchè la complesità diventa ingestibile.
IDEA: Per implementare la ricerca troncata, serve un algoritmo di ordinamento delle mosse dalla migliore alla peggiore (utile anche per alpha-beta pruning).

#### Sviluppare la funzione Evaluate per assegnare il punteggio alla posizione attuale della partita

Evaluate() prende in input la posizione attuale e ritorna un intero o un double che rappresenta lo stato della partita. Questo intero dovrebbe indicare che uno dei due giocatori è in vantaggio:

- se evaluate()==1 il giocatore 1 ha una mossa vincente
- se evaluate()==-1 il giocatore 2 ha una mossa vincente
- se 0 < evaluate() < 1 il giocatore 1 sta vincendo
- se -1 < evaluate < 0 il giocatore 2 sta vincendo
- se evaluate()==0 il gioco è in parità.

Se usiamo gli interi e non i double, il range si può portare a [-100, 100] o [-1000, 1000].

### Funzioni di miglioramento future

#### Integrare Minimax con un algoritmo di ordinamento per ordinare le mosse dalla migliore alla peggiore tra quelle valutate

#### Migliorare l'algoritmo Minimax portandolo ad essere un AlphaBeta Pruning

#### Ottimizzazione delle strategie di ricerca

## Completed Tasks

None
