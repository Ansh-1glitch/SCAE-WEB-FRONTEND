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


