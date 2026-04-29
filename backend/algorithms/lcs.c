// lcs.c — Longest Common Subsequence (DP)
// SCAE Complaint Similarity — do complaint texts kitne similar hain
// Time: O(mn) | Space: O(mn)

#include "scae.h"
#include <string.h>

// LCS length calculate karo aur actual subsequence bhi nikalo
int lcs(const char *s1, int m, const char *s2, int n,
        char result[], int *result_len) {
    int dp[201][201];
    memset(dp, 0, sizeof(dp));

    // DP table fill karo
    for (int i = 1; i <= m; i++) {
        for (int j = 1; j <= n; j++) {
            if (s1[i-1] == s2[j-1]) {
                dp[i][j] = dp[i-1][j-1] + 1; // match mila
            } else {
                dp[i][j] = dp[i-1][j] > dp[i][j-1] ? dp[i-1][j] : dp[i][j-1]; // max lo
            }
        }
    }

    // traceback karo — actual LCS string nikalo
    int i = m, j = n;
    *result_len = dp[m][n];
    int pos = *result_len - 1;
    while (i > 0 && j > 0) {
        if (s1[i-1] == s2[j-1]) {
            result[pos--] = s1[i-1];
            i--; j--;
        } else if (dp[i-1][j] > dp[i][j-1]) {
            i--;
        } else {
            j--;
        }
    }
    result[*result_len] = '\0';
    return dp[m][n];
}
