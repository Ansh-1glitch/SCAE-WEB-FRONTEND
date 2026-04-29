// bellman_ford.c — Bellman-Ford Shortest Path Algorithm
// SCAE Road Closure Simulation — road band hone pe alternate route dhundo
// Time: O(VE) | Space: O(V)

#include "scae.h"
#include <string.h>

void bellman_ford(Edge edges[], int E, int V, int src, int dst,
                  int skip_u, int skip_v, AlgoResult *result) {

    int dist[MAX_V], prev[MAX_V];

    for (int i = 0; i < V; i++) { dist[i] = INF; prev[i] = -1; }
    dist[src] = 0;

    // V-1 relaxation passes karo
    for (int pass = 0; pass < V - 1; pass++) {
        for (int i = 0; i < E; i++) {
            int u = edges[i].u, v = edges[i].v, w = edges[i].weight;

            // ye edge skip karo — road closure simulate karne ke liye
            if ((u == skip_u && v == skip_v) || (u == skip_v && v == skip_u)) continue;

            // forward relaxation
            if (dist[u] != INF && dist[u] + w < dist[v]) { dist[v] = dist[u] + w; prev[v] = u; }
            // backward relaxation — undirected graph hai
            if (dist[v] != INF && dist[v] + w < dist[u]) { dist[u] = dist[v] + w; prev[u] = v; }
        }
    }

    result->distance = dist[dst];
    result->path_len = 0;

    if (dist[dst] != INF) {
        int stack[MAX_V], top = 0;
        int curr = dst;
        while (curr != -1) { stack[top++] = curr; curr = prev[curr]; }
        for (int i = top - 1; i >= 0; i--) result->path[result->path_len++] = stack[i];
    }
}
