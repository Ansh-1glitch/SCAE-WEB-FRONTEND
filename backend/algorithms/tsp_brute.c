// tsp_brute.c — TSP Brute Force (exact solution for small n)
// Time: O(n!) | Space: O(n)
// Warning: sirf n <= 8 ke liye practical hai

#include "scae.h"
#include <string.h>

static int bestDist;
static int bestPath[MAX_V];
static int bestPathLen;

static void permute(int graph[MAX_V][MAX_V], int perm[], int start, int n,
                    int startNode, int *visited, AlgoResult *result) {
    if (start == n) {
        // puri permutation try karo
        int dist = 0, valid = 1;
        int prev = startNode;
        for (int i = 0; i < n; i++) {
            int curr = perm[i];
            if (!graph[prev][curr]) { valid = 0; break; }
            dist += graph[prev][curr];
            prev = curr;
        }
        // wapas start pe
        if (valid && graph[prev][startNode]) {
            dist += graph[prev][startNode];
            if (dist < bestDist) {
                bestDist = dist;
                bestPath[0] = startNode;
                for (int i = 0; i < n; i++) bestPath[i+1] = perm[i];
                bestPath[n+1] = startNode;
                bestPathLen = n + 2;
            }
        }
        return;
    }
    for (int i = start; i < n; i++) {
        int t = perm[start]; perm[start] = perm[i]; perm[i] = t;
        permute(graph, perm, start + 1, n, startNode, visited, result);
        t = perm[start]; perm[start] = perm[i]; perm[i] = t;
    }
}

int tsp_brute(int graph[MAX_V][MAX_V], int V,
              int checkpoints[], int cpCount, AlgoResult *result) {
    if (cpCount <= 1) { result->distance = 0; result->path_len = 0; return 0; }

    bestDist = INF;
    int perm[MAX_V];
    int visited[MAX_V] = {0};
    // starting node ke alawa baaki nodes permute karo
    int k = 0;
    for (int i = 1; i < cpCount; i++) perm[k++] = checkpoints[i];
    permute(graph, perm, 0, cpCount - 1, checkpoints[0], visited, result);

    result->distance = bestDist;
    result->path_len = bestPathLen;
    for (int i = 0; i < bestPathLen; i++) result->path[i] = bestPath[i];
    return bestDist;
}
