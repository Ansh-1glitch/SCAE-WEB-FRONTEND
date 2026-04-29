// dijkstra.c — Dijkstra's Shortest Path Algorithm
// SCAE Navigation Engine — city map pe shortest path nikalta hai
// Time: O((V+E) log V) | Space: O(V)

#include "scae.h"
#include <limits.h>
#include <string.h>

// minimum distance wala unvisited node dhundo
static int minDist(int dist[], int visited[], int V) {
    int min = INF, idx = -1;
    for (int v = 0; v < V; v++) {
        if (!visited[v] && dist[v] <= min) {
            min = dist[v];
            idx = v;
        }
    }
    return idx;
}

// path traceback karo — destination se source tak
static int tracePath(int prev[], int src, int dst, AlgoResult *result) {
    if (prev[dst] == -1 && dst != src) return 0;
    int stack[MAX_V], top = 0;
    int curr = dst;
    while (curr != -1) {
        stack[top++] = curr;
        curr = prev[curr];
    }
    result->path_len = 0;
    for (int i = top - 1; i >= 0; i--) {
        result->path[result->path_len++] = stack[i];
    }
    return 1;
}

// Dijkstra ka main function — adjacency matrix se kaam karta hai
void dijkstra(int graph[MAX_V][MAX_V], int V, int src, int dst,
              int skip_edge_u, int skip_edge_v, AlgoResult *result) {

    int dist[MAX_V], visited[MAX_V], prev[MAX_V];

    // saare distances infinity se initialize karo
    for (int i = 0; i < V; i++) {
        dist[i] = INF;
        visited[i] = 0;
        prev[i] = -1;
    }

    dist[src] = 0; // source node ka distance 0 hai

    for (int c = 0; c < V - 1; c++) {
        int u = minDist(dist, visited, V);
        if (u == -1) break;
        visited[u] = 1;

        for (int v = 0; v < V; v++) {
            // skip closed edge — road closure simulation ke liye
            if ((u == skip_edge_u && v == skip_edge_v) ||
                (u == skip_edge_v && v == skip_edge_u)) continue;

            // edge hai aur v abhi visited nahi
            if (!visited[v] && graph[u][v] && dist[u] + graph[u][v] < dist[v]) {
                dist[v] = dist[u] + graph[u][v];
                prev[v] = u;
            }
        }
    }

    result->distance = dist[dst];
    tracePath(prev, src, dst, result);
}
