// heap_sort.c — Heap Sort Algorithm
// SCAE Emergency Triage — urgency ke order mein emergencies sort karo
// Time: O(n log n) | Space: O(1) in-place

#include "scae.h"

// max-heap property restore karo — arr[i] ke subtree pe
static void heapify(int arr[], int n, int i) {
    int largest = i;          // root sabse bada assume karo
    int left = 2 * i + 1;    // left child
    int right = 2 * i + 2;   // right child

    if (left < n && arr[left] > arr[largest])   largest = left;
    if (right < n && arr[right] > arr[largest])  largest = right;

    if (largest != i) {
        int t = arr[i]; arr[i] = arr[largest]; arr[largest] = t; // swap
        heapify(arr, n, largest); // recursively fix karo
    }
}

// Heap Sort main function — arr[] in-place sort karo ascending order mein
void heap_sort(int arr[], int n) {
    // max-heap build karo — bottom-up approach
    for (int i = n / 2 - 1; i >= 0; i--) heapify(arr, n, i);

    // ek ek element extract karo
    for (int i = n - 1; i > 0; i--) {
        // root (max) ko end mein bhejo
        int t = arr[0]; arr[0] = arr[i]; arr[i] = t;
        // remaining elements pe heapify karo
        heapify(arr, i, 0);
    }
}
