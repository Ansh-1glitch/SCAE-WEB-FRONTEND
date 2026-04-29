// tsp_greedy.c — Travelling Salesman Problem (Nearest Neighbour Greedy)
// SCAE Patrol Route — city checkpoints ko minimum distance mein cover karo
// Time: O(n^2) | Space: O(n)

#include "scae.h"
#include <string.h>

// TSP Greedy — nearest neighbour heuristic
// checkpoints[] = visit karne wale nodes, cpCount = count
// result path route[] mein store hota hai
int tsp_greedy(int graph[MAX_V][MAX_V], int V,
               int checkpoints[], int cpCount, AlgoResult *result) {

    int visited[MAX_V];
    memset(visited, 0, sizeof(visited));

    result->path_len = 0;
    result->distance = 0;

    // pehle checkpoint se shuru karo
    int current = checkpoints[0];
    result->path[result->path_len++] = current;
    visited[current] = 1;

    int remaining = cpCount - 1;

    while (remaining > 0) {
        // nearest unvisited checkpoint dhundo
        int nearest = -1, minDist = INF;
        for (int i = 0; i < cpCount; i++) {
            int cp = checkpoints[i];
            if (!visited[cp] && graph[current][cp] && graph[current][cp] < minDist) {
                minDist = graph[current][cp];
                nearest = cp;
            }
        }

        if (nearest == -1) break; // koi reachable checkpoint nahi

        // nearest pe jao
        visited[nearest] = 1;
        result->path[result->path_len++] = nearest;
        result->distance += minDist;
        current = nearest;
        remaining--;
    }

    // starting point pe wapas aao (circular route)
    if (result->path_len > 1 && graph[current][checkpoints[0]]) {
        result->distance += graph[current][checkpoints[0]];
        result->path[result->path_len++] = checkpoints[0];
    }

    return result->distance;
}
