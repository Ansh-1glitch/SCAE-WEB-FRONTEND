

#include "scae.h"
#include <string.h>

// BFS ka main function — src se max_hops distance ke andar saare nodes dhundo
void bfs(int graph[MAX_V][MAX_V], int V, int src, int max_hops, AlgoResult *result) {

    int visited[MAX_V], hops[MAX_V];
    int queue[MAX_V], front = 0, rear = 0;

    // initialization — saab unvisited hai
    for (int i = 0; i < V; i++) {
        visited[i] = 0;
        hops[i] = -1;
    }

    visited[src] = 1;
    hops[src] = 0;
    queue[rear++] = src;

    result->visited_len = 0;
    result->path[result->visited_len++] = src;

    while (front < rear) {
        int u = queue[front++];
        if (hops[u] >= max_hops) continue; // max hops ho gaye toh aage mat jao

        for (int v = 0; v < V; v++) {
            if (graph[u][v] && !visited[v]) {
                visited[v] = 1;
                hops[v] = hops[u] + 1;
                queue[rear++] = v;
                result->path[result->visited_len++] = v;
            }
        }
    }

    result->distance = result->visited_len; // total reachable count
}
