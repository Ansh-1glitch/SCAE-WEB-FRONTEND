// floyd_warshall.c — Floyd-Warshall All-Pairs Shortest Path
// SCAE Admin Panel — city ke saare node pairs ke beech shortest distance
// Time: O(V^3) | Space: O(V^2)

#include "scae.h"
#include <string.h>

void floyd_warshall(int graph[MAX_V][MAX_V], int V, AlgoResult *result) {

    // distance matrix copy karo graph se
    for (int i = 0; i < V; i++) {
        for (int j = 0; j < V; j++) {
            if (i == j)             result->matrix[i][j] = 0;
            else if (graph[i][j])   result->matrix[i][j] = graph[i][j];
            else                    result->matrix[i][j] = INF;
        }
    }

    // teen nested loops — ye Floyd-Warshall ka core hai
    for (int k = 0; k < V; k++) {
        for (int i = 0; i < V; i++) {
            for (int j = 0; j < V; j++) {
                if (result->matrix[i][k] != INF &&
                    result->matrix[k][j] != INF &&
                    result->matrix[i][k] + result->matrix[k][j] < result->matrix[i][j]) {
                    result->matrix[i][j] = result->matrix[i][k] + result->matrix[k][j];
                }
            }
        }
    }

    result->distance = 0; // success indicator
}
