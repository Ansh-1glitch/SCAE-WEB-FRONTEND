#include <stdio.h>
#include <math.h>

int min(int a, int b){
    if(a < b)return a;
    else return b;
}

int max(int a, int b){
    if (a>b)return a;
    else return b;
}

void swap(int* a, int*  b){int temp=*a;*a=*b;*b=temp;}
void printt(int arr[], int n){for (int i = 0; i < n; i++)printf("%d", arr[i]);}


int QuickSortPartion( int arr[],int low , int high  ){
    int pivot = arr[high];

    int j= low-1;
    for (int i = low; i < high; i++){
        if (arr[i] < pivot){
            j++;
            swap(&arr[i],&arr[j]);
        }
    }
    swap(   &arr[j+1], &arr[high]  );

    return j+1;
}


void QuickSort(int arr[] ,int low, int high){
    if (low >=high)return;

    int pi = QuickSortPartion(arr, low ,high);

    QuickSort(arr,low,pi-1);
    QuickSort(arr,pi+1,high);
}





int main(){
    int arr1[]= {9,8,7,6,5,4,3,2,1};
    int arr2[]={1,2,3,4,5,6,7,8,9};
    int n = sizeof(arr1)/sizeof(arr1[0]);
    int c = 0;


    printt(arr1 , n);
    printf("\n");
    QuickSort(arr1, 0 , n-1);
    printt(arr1 , n);
    return 0;
}



// function with comments. to know what is happening in the function

int QuickSortPartion(int arr[], int low, int high){

    // Choose last element as pivot
    int pivot = arr[high];

    // j → keeps track of position for smaller elements
    int j = low - 1;

    // Traverse from low to high-1
    // and move elements smaller than pivot to left side
    for (int i = low; i < high; i++){

        // If current element is smaller than pivot
        if (arr[i] < pivot){

            j++; // expand smaller element region
            swap(&arr[i], &arr[j]); // place smaller element correctly
        }
    }

    // Place pivot at its correct sorted position
    swap(&arr[j+1], &arr[high]);

    return j+1; // return pivot index
}

// Main QuickSort function

void QuickSort(int arr[], int low, int high){

    // Base condition → if subarray has 0 or 1 element → already sorted
    if (low >= high) return;

    // Partition the array and get pivot index
    int pi = QuickSortPartion(arr, low, high);

    // Recursively sort left part (elements smaller than pivot)
    QuickSort(arr, low, pi-1);

    // Recursively sort right part (elements greater than pivot)
    QuickSort(arr, pi+1, high);
}

