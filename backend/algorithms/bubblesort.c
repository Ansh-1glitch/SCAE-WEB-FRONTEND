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


void BubbleSort(int arr[], int n ){
    for (int i = 0; i < n-1; i++){

        int optamise=0;
        for (int j = 0; j <n-1-i ; j++){
            if (arr[j]>arr[j+1]){
                optamise++;
                swap(&arr[j],&arr[j+1]);
            }
        }

        if (optamise == 0){
        printf("YES ");
        return;
        }
    }
}


int main(){
    int arr1[]= {9,8,7,6,5,4,3,2,1};
    int arr2[]={1,2,3,4,5,6,7,8,9};
    int n = sizeof(arr1)/sizeof(arr1[0]);
    int c = 0;

    BubbleSort(arr2, n );
    printt(arr2 , n);
    return 0;
}





//function with comments. to know what is happening in the function

void BubbleSort(int arr[], int n ){
    
    // Outer loop → controls number of passes
    // After each pass, one largest element goes to its correct position
    for (int i = 0; i < n-1; i++){

        // optamise → checks if any swap happened in this pass
        // If no swap → array already sorted → stop early
        int optamise = 0;

        // Inner loop → compares adjacent elements
        // n-1-i because last i elements are already sorted
        for (int j = 0; j < n-1-i; j++){

            // If current element is greater than next → swap
            if (arr[j] > arr[j+1]){

                optamise++;  // swap happened → mark it
                swap(&arr[j], &arr[j+1]); // swap elements
            }
        }

        // If no swaps happened in this pass
        // → array is already sorted → exit function early
        if (optamise == 0) return;
    }
}
