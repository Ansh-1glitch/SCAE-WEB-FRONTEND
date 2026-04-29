// ScaeJNI.c — JNI bridge — Java se C functions call hoti hain yahan
// ye file Java ke native methods ko C implementations se connect karti hai

#include <jni.h>
#include "scae.h"
#include <string.h>
#include <stdlib.h>

// C algorithm function declarations — implementations .c files mein hain
void dijkstra(int graph[MAX_V][MAX_V], int V, int src, int dst,
              int skip_edge_u, int skip_edge_v, AlgoResult *result);
void bfs(int graph[MAX_V][MAX_V], int V, int src, int max_hops, AlgoResult *result);
void dfs_components(int graph[MAX_V][MAX_V], int V,
                    int disabled_nodes[], int disabled_count, AlgoResult *result);
void floyd_warshall(int graph[MAX_V][MAX_V], int V, AlgoResult *result);
void kruskal(Edge edges[], int E, int V, AlgoResult *result);
void prim(int graph[MAX_V][MAX_V], int V, AlgoResult *result);
void bellman_ford(Edge edges[], int E, int V, int src, int dst,
                  int skip_u, int skip_v, AlgoResult *result);
int  knapsack_01(int cost[], int benefit[], int n, int W, int selected[]);
int  huffman_encode(const char *input, int *out_original, int *out_compressed,
                    int out_freq[], char out_chars[], int out_code_lens[], int *uniqueCount);
int  kmp_search(const char *text, int n, const char *pattern, int m,
                int positions[], int *comparisons);
int  job_sequencing(int deadlines[], int profits[], int n,
                    int scheduled[], int *total_profit);
void heap_sort(int arr[], int n);
int  benchmark_sort(int n, long times[]);

// helper: flat int[] se graph[MAX_V][MAX_V] banao
static void buildGraph(jint *arr, int V, int graph[MAX_V][MAX_V]) {
    for (int i = 0; i < V; i++)
        for (int j = 0; j < V; j++)
            graph[i][j] = arr[i * V + j];
}

// =============================================
// DIJKSTRA JNI wrapper
// Java se: runDijkstra(graph[], V, src, dst, skipU, skipV)
// Returns: [distance, path_len, n0, n1, ...]
// =============================================
JNIEXPORT jintArray JNICALL Java_handlers_AlgoHandler_runDijkstra(
    JNIEnv *env, jclass cls,
    jintArray jGraph, jint V, jint src, jint dst, jint skipU, jint skipV) {

    jint *graphArr = (*env)->GetIntArrayElements(env, jGraph, NULL);
    int graph[MAX_V][MAX_V];
    buildGraph(graphArr, V, graph);
    (*env)->ReleaseIntArrayElements(env, jGraph, graphArr, 0);

    AlgoResult result;
    memset(&result, 0, sizeof(result));
    dijkstra(graph, V, src, dst, skipU, skipV, &result);

    int returnSize = 2 + result.path_len;
    jintArray jResult = (*env)->NewIntArray(env, returnSize);
    jint *arr = (jint*)malloc(returnSize * sizeof(jint));
    arr[0] = result.distance;
    arr[1] = result.path_len;
    for (int i = 0; i < result.path_len; i++) arr[2 + i] = result.path[i];
    (*env)->SetIntArrayRegion(env, jResult, 0, returnSize, arr);
    free(arr);
    return jResult;
}

// =============================================
// BFS JNI wrapper
// Returns: [count, n0, n1, ...]
// =============================================
JNIEXPORT jintArray JNICALL Java_handlers_AlgoHandler_runBFS(
    JNIEnv *env, jclass cls,
    jintArray jGraph, jint V, jint src, jint maxHops) {

    jint *graphArr = (*env)->GetIntArrayElements(env, jGraph, NULL);
    int graph[MAX_V][MAX_V];
    buildGraph(graphArr, V, graph);
    (*env)->ReleaseIntArrayElements(env, jGraph, graphArr, 0);

    AlgoResult result;
    memset(&result, 0, sizeof(result));
    bfs(graph, V, src, maxHops, &result);

    int sz = result.visited_len + 1;
    jintArray jResult = (*env)->NewIntArray(env, sz);
    jint *arr = (jint*)malloc(sz * sizeof(jint));
    arr[0] = result.visited_len;
    for (int i = 0; i < result.visited_len; i++) arr[i + 1] = result.path[i];
    (*env)->SetIntArrayRegion(env, jResult, 0, sz, arr);
    free(arr);
    return jResult;
}

// =============================================
// FLOYD-WARSHALL JNI wrapper
// Returns: flat V*V distance matrix
// =============================================
JNIEXPORT jintArray JNICALL Java_handlers_AlgoHandler_runFloydWarshall(
    JNIEnv *env, jclass cls,
    jintArray jGraph, jint V) {

    jint *graphArr = (*env)->GetIntArrayElements(env, jGraph, NULL);
    int graph[MAX_V][MAX_V];
    buildGraph(graphArr, V, graph);
    (*env)->ReleaseIntArrayElements(env, jGraph, graphArr, 0);

    AlgoResult result;
    memset(&result, 0, sizeof(result));
    floyd_warshall(graph, V, &result);

    jintArray jResult = (*env)->NewIntArray(env, V * V);
    jint *arr = (jint*)malloc(V * V * sizeof(jint));
    for (int i = 0; i < V; i++)
        for (int j = 0; j < V; j++)
            arr[i * V + j] = result.matrix[i][j];
    (*env)->SetIntArrayRegion(env, jResult, 0, V * V, arr);
    free(arr);
    return jResult;
}

// =============================================
// KRUSKAL JNI wrapper
// Returns: [total_cost, count, u0,v0,w0, u1,v1,w1, ...]
// =============================================
JNIEXPORT jintArray JNICALL Java_handlers_AlgoHandler_runKruskal(
    JNIEnv *env, jclass cls,
    jintArray jEdgesU, jintArray jEdgesV, jintArray jEdgesW, jint E, jint V) {

    jint *eu = (*env)->GetIntArrayElements(env, jEdgesU, NULL);
    jint *ev = (*env)->GetIntArrayElements(env, jEdgesV, NULL);
    jint *ew = (*env)->GetIntArrayElements(env, jEdgesW, NULL);
    Edge edges[MAX_E];
    for (int i = 0; i < E; i++) { edges[i].u = eu[i]; edges[i].v = ev[i]; edges[i].weight = ew[i]; }
    (*env)->ReleaseIntArrayElements(env, jEdgesU, eu, 0);
    (*env)->ReleaseIntArrayElements(env, jEdgesV, ev, 0);
    (*env)->ReleaseIntArrayElements(env, jEdgesW, ew, 0);

    AlgoResult result;
    memset(&result, 0, sizeof(result));
    kruskal(edges, E, V, &result);

    int sz = 2 + result.mst_count * 3;
    jintArray jResult = (*env)->NewIntArray(env, sz);
    jint *arr = (jint*)malloc(sz * sizeof(jint));
    arr[0] = result.mst_total;
    arr[1] = result.mst_count;
    for (int i = 0; i < result.mst_count; i++) {
        arr[2 + i*3]   = result.mst_edges_u[i];
        arr[2 + i*3+1] = result.mst_edges_v[i];
        arr[2 + i*3+2] = result.mst_edges_w[i];
    }
    (*env)->SetIntArrayRegion(env, jResult, 0, sz, arr);
    free(arr);
    return jResult;
}

// =============================================
// PRIM JNI wrapper
// Returns: [total_cost, count, u0,v0,w0, u1,v1,w1, ...]
// =============================================
JNIEXPORT jintArray JNICALL Java_handlers_AlgoHandler_runPrim(
    JNIEnv *env, jclass cls,
    jintArray jGraph, jint V) {

    jint *graphArr = (*env)->GetIntArrayElements(env, jGraph, NULL);
    int graph[MAX_V][MAX_V];
    buildGraph(graphArr, V, graph);
    (*env)->ReleaseIntArrayElements(env, jGraph, graphArr, 0);

    AlgoResult result;
    memset(&result, 0, sizeof(result));
    prim(graph, V, &result);

    int sz = 2 + result.mst_count * 3;
    jintArray jResult = (*env)->NewIntArray(env, sz);
    jint *arr = (jint*)malloc(sz * sizeof(jint));
    arr[0] = result.mst_total;
    arr[1] = result.mst_count;
    for (int i = 0; i < result.mst_count; i++) {
        arr[2 + i*3]   = result.mst_edges_u[i];
        arr[2 + i*3+1] = result.mst_edges_v[i];
        arr[2 + i*3+2] = result.mst_edges_w[i];
    }
    (*env)->SetIntArrayRegion(env, jResult, 0, sz, arr);
    free(arr);
    return jResult;
}

// =============================================
// BELLMAN-FORD JNI wrapper
// Returns: [distance, path_len, n0, n1, ...]
// =============================================
JNIEXPORT jintArray JNICALL Java_handlers_AlgoHandler_runBellmanFord(
    JNIEnv *env, jclass cls,
    jintArray jEdgesU, jintArray jEdgesV, jintArray jEdgesW,
    jint E, jint V, jint src, jint dst, jint skipU, jint skipV) {

    jint *eu = (*env)->GetIntArrayElements(env, jEdgesU, NULL);
    jint *ev = (*env)->GetIntArrayElements(env, jEdgesV, NULL);
    jint *ew = (*env)->GetIntArrayElements(env, jEdgesW, NULL);
    Edge edges[MAX_E];
    for (int i = 0; i < E; i++) { edges[i].u = eu[i]; edges[i].v = ev[i]; edges[i].weight = ew[i]; }
    (*env)->ReleaseIntArrayElements(env, jEdgesU, eu, 0);
    (*env)->ReleaseIntArrayElements(env, jEdgesV, ev, 0);
    (*env)->ReleaseIntArrayElements(env, jEdgesW, ew, 0);

    AlgoResult result;
    memset(&result, 0, sizeof(result));
    bellman_ford(edges, E, V, src, dst, skipU, skipV, &result);

    int sz = 2 + result.path_len;
    jintArray jResult = (*env)->NewIntArray(env, sz);
    jint *arr = (jint*)malloc(sz * sizeof(jint));
    arr[0] = result.distance;
    arr[1] = result.path_len;
    for (int i = 0; i < result.path_len; i++) arr[2 + i] = result.path[i];
    (*env)->SetIntArrayRegion(env, jResult, 0, sz, arr);
    free(arr);
    return jResult;
}

// =============================================
// DFS COMPONENTS JNI wrapper
// Returns: [comp_count, sz0, n0a,n0b,..., sz1, n1a,...]
// =============================================
JNIEXPORT jintArray JNICALL Java_handlers_AlgoHandler_runDFSComponents(
    JNIEnv *env, jclass cls,
    jintArray jGraph, jint V,
    jintArray jDisabled, jint disabledCount) {

    jint *graphArr = (*env)->GetIntArrayElements(env, jGraph, NULL);
    int graph[MAX_V][MAX_V];
    buildGraph(graphArr, V, graph);
    (*env)->ReleaseIntArrayElements(env, jGraph, graphArr, 0);

    jint *disArr = (*env)->GetIntArrayElements(env, jDisabled, NULL);
    int disabled[MAX_V];
    for (int i = 0; i < disabledCount; i++) disabled[i] = disArr[i];
    (*env)->ReleaseIntArrayElements(env, jDisabled, disArr, 0);

    AlgoResult result;
    memset(&result, 0, sizeof(result));
    dfs_components(graph, V, disabled, disabledCount, &result);

    // result pack karo: [comp_count, sz0, n0a, n0b, ..., sz1, n1a, ...]
    int totalSize = 1;
    for (int c = 0; c < result.comp_count; c++) totalSize += 1 + result.visited[c];
    jintArray jResult = (*env)->NewIntArray(env, totalSize);
    jint *arr = (jint*)malloc(totalSize * sizeof(jint));
    int pos = 0;
    arr[pos++] = result.comp_count;
    for (int c = 0; c < result.comp_count; c++) {
        int sz = result.visited[c];
        arr[pos++] = sz;
        for (int i = 0; i < sz; i++) arr[pos++] = result.components[c][i];
    }
    (*env)->SetIntArrayRegion(env, jResult, 0, totalSize, arr);
    free(arr);
    return jResult;
}

// =============================================
// KNAPSACK 01 JNI wrapper
// Returns: [max_benefit, n, selected0, selected1, ...]
// =============================================
JNIEXPORT jintArray JNICALL Java_handlers_AlgoHandler_runKnapsack01(
    JNIEnv *env, jclass cls,
    jintArray jCosts, jintArray jBenefits, jint n, jint W) {

    jint *costs    = (*env)->GetIntArrayElements(env, jCosts,    NULL);
    jint *benefits = (*env)->GetIntArrayElements(env, jBenefits, NULL);

    int cArr[51], bArr[51], selected[51];
    for (int i = 0; i < n; i++) { cArr[i] = costs[i]; bArr[i] = benefits[i]; }
    (*env)->ReleaseIntArrayElements(env, jCosts,    costs,    0);
    (*env)->ReleaseIntArrayElements(env, jBenefits, benefits, 0);

    int maxBenefit = knapsack_01(cArr, bArr, n, W, selected);

    int sz = 2 + n;
    jintArray jResult = (*env)->NewIntArray(env, sz);
    jint *arr = (jint*)malloc(sz * sizeof(jint));
    arr[0] = maxBenefit;
    arr[1] = n;
    for (int i = 0; i < n; i++) arr[2 + i] = selected[i];
    (*env)->SetIntArrayRegion(env, jResult, 0, sz, arr);
    free(arr);
    return jResult;
}

// =============================================
// HUFFMAN JNI wrapper
// Returns: [original_bits, compressed_bits, unique_count,
//           freq0, char0, codelen0, freq1, char1, codelen1, ...]
// =============================================
JNIEXPORT jintArray JNICALL Java_handlers_AlgoHandler_runHuffman(
    JNIEnv *env, jclass cls, jstring jInput) {

    const char *input = (*env)->GetStringUTFChars(env, jInput, NULL);

    int original = 0, compressed = 0, uniqueCount = 0;
    int out_freq[256];
    char out_chars[256];
    int out_code_lens[256];

    huffman_encode(input, &original, &compressed, out_freq, out_chars, out_code_lens, &uniqueCount);
    (*env)->ReleaseStringUTFChars(env, jInput, input);

    // pack result: [original, compressed, uniqueCount, freq0, char0, len0, ...]
    int sz = 3 + uniqueCount * 3;
    jintArray jResult = (*env)->NewIntArray(env, sz);
    jint *arr = (jint*)malloc(sz * sizeof(jint));
    arr[0] = original;
    arr[1] = compressed;
    arr[2] = uniqueCount;
    for (int i = 0; i < uniqueCount; i++) {
        arr[3 + i*3]     = out_freq[i];
        arr[3 + i*3 + 1] = (unsigned char)out_chars[i];
        arr[3 + i*3 + 2] = out_code_lens[i];
    }
    (*env)->SetIntArrayRegion(env, jResult, 0, sz, arr);
    free(arr);
    return jResult;
}

// =============================================
// KMP JNI wrapper
// Returns: [match_count, comparisons, pos0, pos1, ...]
// =============================================
JNIEXPORT jintArray JNICALL Java_handlers_AlgoHandler_runKMP(
    JNIEnv *env, jclass cls, jstring jText, jstring jPattern) {

    const char *text    = (*env)->GetStringUTFChars(env, jText,    NULL);
    const char *pattern = (*env)->GetStringUTFChars(env, jPattern, NULL);
    int n = strlen(text), m = strlen(pattern);

    int positions[1000], comparisons = 0;
    int count = kmp_search(text, n, pattern, m, positions, &comparisons);

    (*env)->ReleaseStringUTFChars(env, jText,    text);
    (*env)->ReleaseStringUTFChars(env, jPattern, pattern);

    int sz = 2 + count;
    jintArray jResult = (*env)->NewIntArray(env, sz);
    jint *arr = (jint*)malloc(sz * sizeof(jint));
    arr[0] = count;
    arr[1] = comparisons;
    for (int i = 0; i < count; i++) arr[2 + i] = positions[i];
    (*env)->SetIntArrayRegion(env, jResult, 0, sz, arr);
    free(arr);
    return jResult;
}

// =============================================
// JOB SCHEDULER JNI wrapper
// Returns: [scheduled_count, total_profit, job_id0, job_id1, ...]
// =============================================
JNIEXPORT jintArray JNICALL Java_handlers_AlgoHandler_runJobScheduler(
    JNIEnv *env, jclass cls,
    jintArray jDeadlines, jintArray jProfits, jint n) {

    jint *dl = (*env)->GetIntArrayElements(env, jDeadlines, NULL);
    jint *pr = (*env)->GetIntArrayElements(env, jProfits,   NULL);

    int deadlines[MAX_V], profits[MAX_V], scheduled[MAX_V];
    for (int i = 0; i < n; i++) { deadlines[i] = dl[i]; profits[i] = pr[i]; }
    (*env)->ReleaseIntArrayElements(env, jDeadlines, dl, 0);
    (*env)->ReleaseIntArrayElements(env, jProfits,   pr, 0);

    int total_profit = 0;
    int count = job_sequencing(deadlines, profits, n, scheduled, &total_profit);

    int sz = 2 + count;
    jintArray jResult = (*env)->NewIntArray(env, sz);
    jint *arr = (jint*)malloc(sz * sizeof(jint));
    arr[0] = count;
    arr[1] = total_profit;
    for (int i = 0; i < count; i++) arr[2 + i] = scheduled[i];
    (*env)->SetIntArrayRegion(env, jResult, 0, sz, arr);
    free(arr);
    return jResult;
}

// =============================================
// BENCHMARK JNI wrapper
// Returns: [n, us0, us1, us2, us3, us4, us5, us6] (7 sorting times in microseconds)
// =============================================
JNIEXPORT jlongArray JNICALL Java_handlers_AlgoHandler_runBenchmark(
    JNIEnv *env, jclass cls, jint n) {

    long times[7];
    benchmark_sort(n, times);

    jlongArray jResult = (*env)->NewLongArray(env, 8);
    jlong arr[8];
    arr[0] = n;
    for (int i = 0; i < 7; i++) arr[i+1] = times[i];
    (*env)->SetLongArrayRegion(env, jResult, 0, 8, arr);
    return jResult;
}
