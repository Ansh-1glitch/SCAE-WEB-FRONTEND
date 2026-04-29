// activity_sel.c — Activity Selection Greedy Algorithm
// SCAE Maintenance Scheduling — non-overlapping activities maximum select karo
// Time: O(n log n) | Space: O(n)

#include "scae.h"
#include <stdlib.h>
#include <string.h>

typedef struct { int id; int start; int end; } Activity;

static int cmpActivity(const void *a, const void *b) {
    return ((Activity*)a)->end - ((Activity*)b)->end; // end time ke ascending order
}

// Activity Selection ka main function
// start_times[], end_times[] = activities ke time ranges
// selected[] mein selected activity IDs store hote hain
int activity_selection(int start_times[], int end_times[], int n,
                       int selected[], int *selected_count) {
    Activity acts[MAX_V];
    for (int i = 0; i < n; i++) { acts[i].id = i; acts[i].start = start_times[i]; acts[i].end = end_times[i]; }
    qsort(acts, n, sizeof(Activity), cmpActivity); // end time se sort karo

    *selected_count = 0;
    int lastEnd = -1;

    for (int i = 0; i < n; i++) {
        // ye activity pichli se overlap nahi karti
        if (acts[i].start >= lastEnd) {
            selected[(*selected_count)++] = acts[i].id;
            lastEnd = acts[i].end;
        }
    }
    return *selected_count;
}
