// kruskal.c — Kruskal's Minimum Spanning Tree Algorithm
// SCAE Power Grid Optimizer — minimum cable se saare zones connect karo
// Time: O(E log E) | Space: O(V)

#include "scae.h"
#include <stdlib.h>
#include <string.h>

static int parent[MAX_V], rnk[MAX_V];

static int find(int x) {
    if (parent[x] != x) parent[x] = find(parent[x]);
    return parent[x];
}

static void unite(int a, int b) {
    a = find(a); b = find(b);
    if (a == b) return;
    if (rnk[a] < rnk[b]) { int t = a; a = b; b = t; }
    parent[b] = a;
    if (rnk[a] == rnk[b]) rnk[a]++;
}

static int cmpEdge(const void *a, const void *b) {
    return ((Edge*)a)->weight - ((Edge*)b)->weight;
}

void kruskal(Edge edges[], int E, int V, AlgoResult *result) {

    for (int i = 0; i < V; i++) { parent[i] = i; rnk[i] = 0; }

    // edges ko weight ke order mein sort karo
    qsort(edges, E, sizeof(Edge), cmpEdge);

    result->mst_count = 0;
    result->mst_total = 0;

    for (int i = 0; i < E; i++) {
        int u = edges[i].u, v = edges[i].v, w = edges[i].weight;
        if (find(u) != find(v)) {
            unite(u, v);
            result->mst_edges_u[result->mst_count] = u;
            result->mst_edges_v[result->mst_count] = v;
            result->mst_edges_w[result->mst_count] = w;
            result->mst_total += w;
            result->mst_count++;
            if (result->mst_count == V - 1) break; // MST complete
        }
    }
}
