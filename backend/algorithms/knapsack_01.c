// knapsack_01.c — 0/1 Knapsack Dynamic Programming
// SCAE Budget Allocator — best infrastructure projects select karo budget mein
// Time: O(nW) | Space: O(nW)

#include "scae.h"
#include <string.h>

// 0/1 Knapsack ka main function
// cost[] = projects ka cost, benefit[] = projects ka benefit
// n = projects ki count, W = total budget
// returns optimal benefit aur selected[] mein selected projects (1=selected, 0=not)
int knapsack_01(int cost[], int benefit[], int n, int W, int selected[]) {

    // DP table — dp[i][w] = i items mein se W budget pe max benefit
    int dp[51][1001];
    memset(dp, 0, sizeof(dp));

    // DP table fill karo bottom-up
    for (int i = 1; i <= n; i++) {
        for (int w = 0; w <= W; w++) {
            dp[i][w] = dp[i-1][w]; // project i include nahi karo

            // project i include karo agar cost fit ho raha hai
            if (cost[i-1] <= w) {
                int withItem = dp[i-1][w - cost[i-1]] + benefit[i-1];
                if (withItem > dp[i][w]) dp[i][w] = withItem; // better option mila
            }
        }
    }

    // traceback karo — kaunse projects select hue
    int w = W;
    for (int i = n; i > 0; i--) {
        if (dp[i][w] != dp[i-1][w]) {
            selected[i-1] = 1; // ye project select hua
            w -= cost[i-1];    // budget reduce karo
        } else {
            selected[i-1] = 0;
        }
    }

    return dp[n][W]; // maximum benefit return karo
}

// Fractional Knapsack (greedy) — partial items allowed
// benefit/cost ratio ke order mein items select karo
double knapsack_frac(int cost[], int benefit[], int n, int W) {
    // ratio calculate karo
    double ratio[51];
    int idx[51];
    for (int i = 0; i < n; i++) { ratio[i] = (double)benefit[i] / cost[i]; idx[i] = i; }

    // bubble sort by ratio descending
    for (int i = 0; i < n - 1; i++)
        for (int j = 0; j < n - i - 1; j++)
            if (ratio[idx[j]] < ratio[idx[j+1]]) { int t = idx[j]; idx[j] = idx[j+1]; idx[j+1] = t; }

    double totalBenefit = 0.0;
    int remaining = W;
    for (int i = 0; i < n && remaining > 0; i++) {
        int k = idx[i];
        if (cost[k] <= remaining) {
            totalBenefit += benefit[k]; // pura item le lo
            remaining -= cost[k];
        } else {
            totalBenefit += (double)benefit[k] * remaining / cost[k]; // partial item
            remaining = 0;
        }
    }
    return totalBenefit;
}
