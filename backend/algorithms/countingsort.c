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

void CountingSort(int arr[], int n   ){
    int maxx=maxxx(arr,n);
    int count[maxx+1];
    for (int i = 0; i <=maxx; i++)count[i]=0;
    for (int i = 0; i < n; i++)count[arr[i]]++;

    int k=0;
    for (int i = 0; i <=maxx; i++){
        while (count[i]>0){
            arr[k++]=i;
            count[i]--;
        }

    }

}

int main(){
    int arr1[]= {9,8,7,6,5,4,3,2,1};
    int arr2[]={1,2,3,4,5,6,7,8,9};
    int n = sizeof(arr1)/sizeof(arr1[0]);
    int key;
    int c = 0;
    printf("Enter key to search: ");
    scanf("%d",&key);

    aabbccSearch(arr2, n , key);
    // printt(arr , n);
    return 0;
}
