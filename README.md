## wiki-walker

Select two wikipages and watch the walker navigate through Wikipedia.

### commands : 

- **walk**      reads in the title of both start- and goalnode. Starts the walk.
- **pref**      prints out the current settings.
- **set**       allows changing settings.
- **stat**      outputs performance measures (currently only #http requests) regarding the last walk.

### settings : 

- **verbose**       : prints the title of the currently visited wiki page.
- **search_dir**    : unidirectional, bidirectional (uses multithreading).
- **search**        : BFS (Breadth-First-Search) and GBFS (Greedy-Best-First-Search).
- **max_links**      : limits the number of links going out from a given node.
- **max_categories**      : limits the number of categories that are considered at each node.
- **max_constraints**      : defines the maximum number of requests before the search is aborted.


### heuristics (need improvement) : 

- **hamming**       : consider nodes with a small hamming distance to the goal node first.
- **longest_substring** : explore nodes that share the longest common substring with the goal node.
- **most_categories** : prioritize exploring nodes that share the most Wikipedia categories with the goal node.
