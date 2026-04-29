// scae.h — SCAE ke saare shared structs yahan define hai
// ye header file saare C files mein include hogi

#ifndef SCAE_H
#define SCAE_H

#define MAX_V 50   // maximum vertices city graph mein
#define MAX_E 200  // maximum edges city graph mein
#define INF 99999  // infinity represent karne ke liye

// ek edge ko represent karta hai
typedef struct {
    int u;      // source node
    int v;      // destination node
    int weight; // road ka weight (km mein)
} Edge;

// algorithm ka result store karta hai
typedef struct {
    int path[MAX_V];      // shortest path nodes
    int path_len;         // path mein kitne nodes hai
    int distance;         // total distance
    int visited[MAX_V];   // BFS/DFS ke liye visited array
    int visited_len;      // kitne nodes visited hue
    int components[MAX_V][MAX_V]; // DFS components ke liye
    int comp_count;               // total components
    int mst_edges_u[MAX_V];       // MST edges source
    int mst_edges_v[MAX_V];       // MST edges destination
    int mst_edges_w[MAX_V];       // MST edges weight
    int mst_count;                // MST mein kitne edges hai
    int mst_total;                // MST ka total cost
    int matrix[MAX_V][MAX_V];     // Floyd-Warshall matrix ke liye
    long time_ns;                  // execution time nanoseconds mein
} AlgoResult;

#endif
