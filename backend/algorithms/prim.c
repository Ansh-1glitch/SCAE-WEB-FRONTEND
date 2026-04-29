// prim.c — Prim's Minimum Spanning Tree Algorithm
// SCAE Power Grid Optimizer — Kruskal ka alternative, same result
// Time: O((V+E) log V) | Space: O(V)

#include "scae.h"
#include <string.h>

void prim(int graph[MAX_V][MAX_V], int V, AlgoResult *result) {

    int key[MAX_V], inMST[MAX_V], par[MAX_V];

    for (int i = 0; i < V; i++) { key[i] = INF; inMST[i] = 0; par[i] = -1; }

    key[0] = 0; // source node se shuru karo
    result->mst_count = 0;
    result->mst_total = 0;

    for (int c = 0; c < V - 1; c++) {
        // minimum key wala unvisited vertex dhundo
        int u = -1, minKey = INF;
        for (int v = 0; v < V; v++) {
            if (!inMST[v] && key[v] < minKey) { minKey = key[v]; u = v; }
        }
        if (u == -1) break;

        inMST[u] = 1;

        // u ka parent edge MST mein save karo
        if (par[u] != -1) {
            result->mst_edges_u[result->mst_count] = par[u];
            result->mst_edges_v[result->mst_count] = u;
            result->mst_edges_w[result->mst_count] = graph[par[u]][u];
            result->mst_total += graph[par[u]][u];
            result->mst_count++;
        }

        // u ke neighbors ke keys update karo
        for (int v = 0; v < V; v++) {
            if (graph[u][v] && !inMST[v] && graph[u][v] < key[v]) {
                key[v] = graph[u][v];
                par[v] = u;
            }
        }
    }
}
