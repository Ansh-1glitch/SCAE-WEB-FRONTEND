// benchmark.c — Algorithm Benchmark (Sorting Algorithms Comparison)
// SCAE Performance Dashboard — 7 sorting algorithms timed karo
// Time: O(n log n) avg | Space: O(n)

#include "scae.h"
#include <stdlib.h>
#include <string.h>
#include <time.h>

// Merge Sort helper
static void merge(int arr[], int l, int m, int r) {
    int n1 = m - l + 1, n2 = r - m;
    int L[MAX_V * 10], R[MAX_V * 10];
    for (int i = 0; i < n1; i++) L[i] = arr[l + i];
    for (int j = 0; j < n2; j++) R[j] = arr[m + 1 + j];
    int i = 0, j = 0, k = l;
    while (i < n1 && j < n2) arr[k++] = (L[i] <= R[j]) ? L[i++] : R[j++];
    while (i < n1) arr[k++] = L[i++];
    while (j < n2) arr[k++] = R[j++];
}

static void merge_sort(int arr[], int l, int r) {
    if (l < r) { int m = (l + r) / 2; merge_sort(arr, l, m); merge_sort(arr, m+1, r); merge(arr, l, m, r); }
}

// Quick Sort helper
static int partition(int arr[], int low, int high) {
    int pivot = arr[high], i = low - 1;
    for (int j = low; j < high; j++) if (arr[j] <= pivot) { i++; int t = arr[i]; arr[i] = arr[j]; arr[j] = t; }
    int t = arr[i+1]; arr[i+1] = arr[high]; arr[high] = t;
    return i + 1;
}
static void quick_sort(int arr[], int low, int high) {
    if (low < high) { int pi = partition(arr, low, high); quick_sort(arr, low, pi-1); quick_sort(arr, pi+1, high); }
}

// Insertion Sort
static void insertion_sort(int arr[], int n) {
    for (int i = 1; i < n; i++) { int key = arr[i], j = i-1; while (j >= 0 && arr[j] > key) { arr[j+1] = arr[j]; j--; } arr[j+1] = key; }
}

// Selection Sort
static void selection_sort(int arr[], int n) {
    for (int i = 0; i < n-1; i++) { int mi = i; for (int j = i+1; j < n; j++) if (arr[j] < arr[mi]) mi = j; int t = arr[i]; arr[i] = arr[mi]; arr[mi] = t; }
}

// Bubble Sort
static void bubble_sort(int arr[], int n) {
    for (int i = 0; i < n-1; i++) for (int j = 0; j < n-i-1; j++) if (arr[j] > arr[j+1]) { int t = arr[j]; arr[j] = arr[j+1]; arr[j+1] = t; }
}

// Shell Sort
static void shell_sort(int arr[], int n) {
    for (int gap = n/2; gap > 0; gap /= 2) for (int i = gap; i < n; i++) { int t = arr[i], j; for (j = i; j >= gap && arr[j-gap] > t; j -= gap) arr[j] = arr[j-gap]; arr[j] = t; }
}

// Counting Sort (for non-negative ints)
static void counting_sort(int arr[], int n) {
    if (n <= 0) return;
    int mx = arr[0]; for (int i = 1; i < n; i++) if (arr[i] > mx) mx = arr[i];
    if (mx < 0) return;
    int *count = (int*)calloc(mx + 1, sizeof(int));
    for (int i = 0; i < n; i++) count[arr[i]]++;
    int k = 0; for (int i = 0; i <= mx; i++) while (count[i]-- > 0) arr[k++] = i;
    free(count);
}

// Benchmark main function
// n = array size
// times[] = 7 algorithm times in microseconds (long)
// returns total algorithms run
int benchmark_sort(int n, long times[]) {

    // random data generate karo
    int *original = (int*)malloc(n * sizeof(int));
    int *arr      = (int*)malloc(n * sizeof(int));
    srand(42); // fixed seed — reproducible results ke liye
    for (int i = 0; i < n; i++) original[i] = rand() % 10000;

    struct timespec t0, t1;

    // 1. Merge Sort
    memcpy(arr, original, n * sizeof(int));
    clock_gettime(CLOCK_MONOTONIC, &t0);
    merge_sort(arr, 0, n-1);
    clock_gettime(CLOCK_MONOTONIC, &t1);
    times[0] = (t1.tv_sec - t0.tv_sec) * 1000000 + (t1.tv_nsec - t0.tv_nsec) / 1000;

    // 2. Quick Sort
    memcpy(arr, original, n * sizeof(int));
    clock_gettime(CLOCK_MONOTONIC, &t0);
    quick_sort(arr, 0, n-1);
    clock_gettime(CLOCK_MONOTONIC, &t1);
    times[1] = (t1.tv_sec - t0.tv_sec) * 1000000 + (t1.tv_nsec - t0.tv_nsec) / 1000;

    // 3. Heap Sort
    memcpy(arr, original, n * sizeof(int));
    clock_gettime(CLOCK_MONOTONIC, &t0);
    { void heap_sort(int[], int); heap_sort(arr, n); }
    clock_gettime(CLOCK_MONOTONIC, &t1);
    times[2] = (t1.tv_sec - t0.tv_sec) * 1000000 + (t1.tv_nsec - t0.tv_nsec) / 1000;

    // 4. Shell Sort
    memcpy(arr, original, n * sizeof(int));
    clock_gettime(CLOCK_MONOTONIC, &t0);
    shell_sort(arr, n);
    clock_gettime(CLOCK_MONOTONIC, &t1);
    times[3] = (t1.tv_sec - t0.tv_sec) * 1000000 + (t1.tv_nsec - t0.tv_nsec) / 1000;

    // 5. Insertion Sort (small n ke liye theek hai)
    int benchN = n > 5000 ? 5000 : n; // insertion/selection/bubble slow hain large n pe
    memcpy(arr, original, benchN * sizeof(int));
    clock_gettime(CLOCK_MONOTONIC, &t0);
    insertion_sort(arr, benchN);
    clock_gettime(CLOCK_MONOTONIC, &t1);
    times[4] = (t1.tv_sec - t0.tv_sec) * 1000000 + (t1.tv_nsec - t0.tv_nsec) / 1000;

    // 6. Selection Sort
    memcpy(arr, original, benchN * sizeof(int));
    clock_gettime(CLOCK_MONOTONIC, &t0);
    selection_sort(arr, benchN);
    clock_gettime(CLOCK_MONOTONIC, &t1);
    times[5] = (t1.tv_sec - t0.tv_sec) * 1000000 + (t1.tv_nsec - t0.tv_nsec) / 1000;

    // 7. Counting Sort
    memcpy(arr, original, n * sizeof(int));
    clock_gettime(CLOCK_MONOTONIC, &t0);
    counting_sort(arr, n);
    clock_gettime(CLOCK_MONOTONIC, &t1);
    times[6] = (t1.tv_sec - t0.tv_sec) * 1000000 + (t1.tv_nsec - t0.tv_nsec) / 1000;

    free(original);
    free(arr);
    return 7;
}
