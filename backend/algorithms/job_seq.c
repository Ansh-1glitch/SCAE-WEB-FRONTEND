// job_seq.c — Job Sequencing with Deadlines (Greedy)
// SCAE Emergency Dispatch — deadline ke andar maximum profit jobs schedule karo
// Time: O(n^2) | Space: O(n)

#include "scae.h"
#include <stdlib.h>
#include <string.h>

typedef struct { int id; int deadline; int profit; } Job;

static int cmpJob(const void *a, const void *b) {
    return ((Job*)b)->profit - ((Job*)a)->profit; // descending profit
}

// Job Sequencing main function
// scheduled[] mein selected job IDs store hote hain (0-indexed)
// returns scheduledCount aur *total_profit
int job_sequencing(int deadlines[], int profits[], int n,
                   int scheduled[], int *total_profit) {
    Job jobs[MAX_V];
    for (int i = 0; i < n; i++) { jobs[i].id = i; jobs[i].deadline = deadlines[i]; jobs[i].profit = profits[i]; }
    qsort(jobs, n, sizeof(Job), cmpJob); // profit ke descending order mein sort karo

    int maxDeadline = 0;
    for (int i = 0; i < n; i++) if (jobs[i].deadline > maxDeadline) maxDeadline = jobs[i].deadline;

    int slots[MAX_V];
    for (int i = 0; i < maxDeadline; i++) slots[i] = -1; // khaali slots

    *total_profit = 0;
    int scheduledCount = 0;

    for (int i = 0; i < n; i++) {
        // deadline se pehle latest khaali slot dhundo
        for (int j = jobs[i].deadline - 1; j >= 0; j--) {
            if (slots[j] == -1) {
                slots[j] = jobs[i].id;
                scheduled[scheduledCount++] = jobs[i].id;
                *total_profit += jobs[i].profit;
                break;
            }
        }
    }
    return scheduledCount;
}
