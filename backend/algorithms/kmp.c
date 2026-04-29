// kmp.c — Knuth-Morris-Pratt String Matching Algorithm
// SCAE Complaint Search — complaint text mein fast keyword search
// Time: O(n+m) | Space: O(m)

#include "scae.h"
#include <string.h>

// LPS (Longest Proper Prefix Suffix) array compute karo — O(m)
static void computeLPS(const char *pattern, int m, int lps[]) {
    lps[0] = 0;
    int len = 0, i = 1;
    while (i < m) {
        if (pattern[i] == pattern[len]) {
            lps[i++] = ++len;
        } else if (len != 0) {
            len = lps[len - 1]; // i increment nahi hoga
        } else {
            lps[i++] = 0;
        }
    }
}

// KMP search — text mein pattern ke saare occurrences dhundo
// positions[] mein 0-indexed match positions store hoti hain
// returns total match count
int kmp_search(const char *text, int n, const char *pattern, int m,
               int positions[], int *comparisons) {
    if (m == 0 || n == 0) { *comparisons = 0; return 0; }

    int lps[500];
    computeLPS(pattern, m, lps);

    int matchCount = 0;
    *comparisons = 0;
    int i = 0, j = 0;

    while (i < n) {
        (*comparisons)++;
        if (text[i] == pattern[j]) { i++; j++; }

        if (j == m) {
            positions[matchCount++] = i - j; // match position save karo
            j = lps[j - 1];
        } else if (i < n && text[i] != pattern[j]) {
            if (j != 0) j = lps[j - 1];
            else i++;
        }
    }
    return matchCount;
}
