// huffman.c — Huffman Coding Greedy Algorithm
// SCAE Sensor Logger — sensor data compress karta hai
// Time: O(n log n) | Space: O(n)

#include "scae.h"
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#define MAX_CHAR 256
#define MAX_NODES 512

// Huffman tree node
typedef struct {
    char ch;         // character (leaf node ke liye)
    int freq;        // frequency
    int left, right; // child indices (-1 = leaf)
} HuffNode;

static HuffNode nodes[MAX_NODES];
static int nodeCount;

// codes array — har char ke liye binary string
static char huffCodes[MAX_CHAR][MAX_CHAR];
static int frequencies[MAX_CHAR];

// recursive code generation — tree traverse karo
static void generateCodes(int idx, char code[], int depth) {
    if (idx == -1) return;
    if (nodes[idx].left == -1 && nodes[idx].right == -1) {
        // leaf node — ye character ka code hai
        code[depth] = '\0';
        strncpy(huffCodes[(unsigned char)nodes[idx].ch], code, MAX_CHAR - 1);
        return;
    }
    // left child = '0', right child = '1'
    code[depth] = '0'; generateCodes(nodes[idx].left,  code, depth + 1);
    code[depth] = '1'; generateCodes(nodes[idx].right, code, depth + 1);
}

// Huffman main function
// output: codes[], original_bits, compressed_bits, freq[] via JNI array
// returns compressed_bits
int huffman_encode(const char *input, int out_original[], int out_compressed[],
                   int out_freq[], char out_chars[], int out_code_lens[], int *uniqueCount) {

    int n = strlen(input);
    *out_original = n * 8;

    // character frequencies count karo
    memset(frequencies, 0, sizeof(frequencies));
    for (int i = 0; i < n; i++) frequencies[(unsigned char)input[i]]++;

    // heap (sorted array) banao
    nodeCount = 0;
    int heap[MAX_NODES], heapSize = 0;

    for (int i = 0; i < MAX_CHAR; i++) {
        if (frequencies[i] > 0) {
            nodes[nodeCount] = (HuffNode){(char)i, frequencies[i], -1, -1};
            heap[heapSize++] = nodeCount++;
        }
    }

    *uniqueCount = heapSize;

    // edge case — sirf ek unique character hai
    if (heapSize == 1) {
        int onlyChar = (unsigned char)nodes[heap[0]].ch;
        out_freq[0] = frequencies[onlyChar];
        out_chars[0] = (char)onlyChar;
        out_code_lens[0] = 1;
        *out_compressed = n; // 1 bit per character
        return n;
    }

    // bubble sort — min-heap simulate karo
    for (int i = 0; i < heapSize - 1; i++)
        for (int j = 0; j < heapSize - i - 1; j++)
            if (nodes[heap[j]].freq > nodes[heap[j+1]].freq) {
                int t = heap[j]; heap[j] = heap[j+1]; heap[j+1] = t;
            }

    // Huffman tree build karo
    while (heapSize > 1) {
        int l = heap[0], r = heap[1];
        heapSize -= 2;
        for (int i = 0; i < heapSize; i++) heap[i] = heap[i+2];

        // parent node banao
        nodes[nodeCount] = (HuffNode){'\0', nodes[l].freq + nodes[r].freq, l, r};
        // sorted position pe insert karo
        int pos = heapSize;
        heap[heapSize++] = nodeCount;
        while (pos > 0 && nodes[heap[pos]].freq < nodes[heap[pos-1]].freq) {
            int t = heap[pos]; heap[pos] = heap[pos-1]; heap[pos-1] = t; pos--;
        }
        nodeCount++;
    }

    // codes generate karo
    memset(huffCodes, 0, sizeof(huffCodes));
    char codeBuf[MAX_CHAR];
    generateCodes(heap[0], codeBuf, 0);

    // compressed bits calculate karo aur output fill karo
    int compBits = 0;
    int outIdx = 0;
    for (int i = 0; i < MAX_CHAR; i++) {
        if (frequencies[i] > 0) {
            int codeLen = strlen(huffCodes[i]);
            compBits += frequencies[i] * codeLen;
            out_freq[outIdx] = frequencies[i];
            out_chars[outIdx] = (char)i;
            out_code_lens[outIdx] = codeLen;
            outIdx++;
        }
    }
    *out_compressed = compBits;
    return compBits;
}
