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


void InsertionSort( int arr[], int n ){
    for (int i = 1; i < n; i++){
        int key = arr[i];
        int j = i-1;
        while(j>=0 && arr[j]> key){
            arr[j+1] = arr[j];
            j--;
        }
        arr[j+1]=key;
    }
}



int main(){
    int arr1[]= {9,8,7,6,5,4,3,2,1};
    int arr2[]={1,2,3,4,5,6,7,8,9};
    int n = sizeof(arr1)/sizeof(arr1[0]);
    int c = 0;


    printt(arr1 , n);
    printf("\n");
    InsertionSort(arr1, n);
    printt(arr1 , n);
    return 0;
}




// function with comments. to know what is happening in the function

// void InsertionSort(int arr[], int n){

//     // Outer loop → starts from 1 because first element is already sorted
//     // We take one element (key) and insert it into the sorted part (left side)
//     for (int i = 1; i < n; i++){

//         int key = arr[i];   // element to be placed at correct position
//         int j = i - 1;      // start comparing from previous index

//         // Shift elements greater than key towards right
//         // j >= 0 → to avoid going out of bounds
//         // arr[j] > key → only shift larger elements
//         while (j >= 0 && arr[j] > key){

//             arr[j+1] = arr[j]; // move element one step right
//             j--;               // move to next left element
//         }

//         // Place key at its correct position
//         // j+1 because j stops at one position before correct index
//         arr[j+1] = key;
//     }
// }
