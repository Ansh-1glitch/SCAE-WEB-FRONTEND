// rabin_karp.c — Rabin-Karp Rolling Hash String Matching
// SCAE Pattern Detection — complaint keywords fast search karo rolling hash se
// Time: O(n+m) avg | Space: O(1)

#include "scae.h"
#include <string.h>

#define RK_BASE  256    // character set size
#define RK_MOD   101    // prime modulus — hash collisions kam karne ke liye

// Rabin-Karp search — text mein pattern dhundo
// returns match count aur positions[] mein positions
int rabin_karp(const char *text, int n, const char *pattern, int m,
               int positions[]) {
    if (m > n || m == 0) return 0;

    int h = 1;
    // h = BASE^(m-1) % MOD — highest power calculate karo
    for (int i = 0; i < m - 1; i++) h = (h * RK_BASE) % RK_MOD;

    // pattern aur text ke pehle window ka hash calculate karo
    int patHash = 0, winHash = 0;
    for (int i = 0; i < m; i++) {
        patHash = (RK_BASE * patHash + pattern[i]) % RK_MOD;
        winHash = (RK_BASE * winHash + text[i]) % RK_MOD;
    }

    int matchCount = 0;
    for (int i = 0; i <= n - m; i++) {
        // hash match check karo
        if (patHash == winHash) {
            // hash match — character by character verify karo (collision handle)
            int match = 1;
            for (int k = 0; k < m; k++) {
                if (text[i+k] != pattern[k]) { match = 0; break; }
            }
            if (match) positions[matchCount++] = i; // actual match
        }

        // sliding window — hash update karo
        if (i < n - m) {
            winHash = (RK_BASE * (winHash - text[i] * h) + text[i+m]) % RK_MOD;
            if (winHash < 0) winHash += RK_MOD; // negative ko positive karo
        }
    }
    return matchCount;
}
