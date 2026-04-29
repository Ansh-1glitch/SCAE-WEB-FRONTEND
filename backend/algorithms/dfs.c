// dfs.c — Depth First Search Algorithm
// SCAE Disaster Simulation — disconnected zones detect karta hai
// Time: O(V+E) | Space: O(V)

#include "scae.h"
#include <string.h>

static int global_visited[MAX_V];
static int current_comp[MAX_V];
static int current_comp_size;

// recursive DFS helper
static void dfsHelper(int graph[MAX_V][MAX_V], int V, int u) {
    global_visited[u] = 1;
    current_comp[current_comp_size++] = u;
    for (int v = 0; v < V; v++) {
        if (graph[u][v] && !global_visited[v]) {
            dfsHelper(graph, V, v);
        }
    }
}

// DFS se connected components dhundo
// disabled_nodes[] = wo nodes jo disaster mein offline ho gaye
void dfs_components(int graph[MAX_V][MAX_V], int V,
                    int disabled_nodes[], int disabled_count,
                    AlgoResult *result) {

    for (int i = 0; i < V; i++) global_visited[i] = 0;

    // disabled nodes ko already visited mark karo
    for (int i = 0; i < disabled_count; i++) {
        if (disabled_nodes[i] < V) global_visited[disabled_nodes[i]] = 1;
    }

    result->comp_count = 0;

    for (int u = 0; u < V; u++) {
        if (!global_visited[u]) {
            current_comp_size = 0;
            dfsHelper(graph, V, u);
            for (int i = 0; i < current_comp_size; i++) {
                result->components[result->comp_count][i] = current_comp[i];
            }
            result->visited[result->comp_count] = current_comp_size;
            result->comp_count++;
        }
    }
}
