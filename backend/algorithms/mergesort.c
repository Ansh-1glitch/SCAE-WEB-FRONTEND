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

void Sort( int arr[], int left ,int mid,int right ){
    int n1 = mid-left+1;
    int n2 = right-mid;
    int l[n1], r[n2];

    for (int i = 0; i < n1; i++)   {l[i] = arr[left+i];}
    for (int i = 0; i < n2; i++)   {r[i] = arr[mid + 1 +i ];}

    int k=left,j,i;                i=j=0;

    while (  i<n1 && j<n2    ){
        if (l[i]<r[j])    {arr[k++] = l[i++];}
        else {arr[k++] = r[j++];}
    }
    
    while (i<n1){arr[k++] = l[i++];}
    while (j <n2){arr[k++] = r[j++];}
}


void mergeSort(int arr[] ,int left, int right){
if (left >= right)return;
int mid =(right+left)/2;
mergeSort(arr,left,mid );
mergeSort(arr,mid +1,right);
Sort(arr,left,mid,right);
}


int main(){
    int arr1[]= {9,8,7,6,5,4,3,2,1};
    int arr2[]={1,2,3,4,5,6,7,8,9};
    int n = sizeof(arr1)/sizeof(arr1[0]);
    int c = 0;


    printt(arr1 , n);
    printf("\n");
    mergeSort(arr1,0,n-1);
    printt(arr1 , n);

    return 0;
}




// // function with comments. to know what is happening in the function
// // (with mistake prevention notes)

// void Sort(int arr[], int left, int mid, int right){

//     // ⚠️ IMPORTANT:
//     // left part = left → mid  (inclusive)
//     // right part = mid+1 → right

//     // Size of left subarray → include mid (+1)
//     int n1 = mid - left + 1;

//     // Size of right subarray
//     int n2 = right - mid;

//     int l[n1], r[n2];

//     // Copy left subarray
//     // ⚠️ Always start from left
//     for (int i = 0; i < n1; i++){
//         l[i] = arr[left + i];
//     }

//     // Copy right subarray
//     // ⚠️ Always start from mid+1
//     for (int i = 0; i < n2; i++){
//         r[i] = arr[mid + 1 + i];
//     }

//     int i = 0, j = 0;

//     // k → index for original array
//     int k = left;

//     // Merge both arrays
//     while (i < n1 && j < n2){

//         // ⚠️ Compare l[i] with r[j] (NOT r[i])
//         if (l[i] < r[j]){
//             arr[k++] = l[i++]; // take from left
//         }
//         else{
//             arr[k++] = r[j++]; // take from right
//         }
//     }

//     // Copy remaining left elements (if any)
//     while (i < n1){
//         arr[k++] = l[i++];
//     }

//     // Copy remaining right elements (if any)
//     // ⚠️ Don't forget j++
//     while (j < n2){
//         arr[k++] = r[j++];
//     }
// }


// // Merge Sort function

// void mergeSort(int arr[], int left, int right){

//     // ⚠️ Base condition → must be >= (not >)
//     // when only one element → already sorted
//     if (left >= right) return;

//     // Find middle index
//     int mid = (left + right) / 2;

//     // ⚠️ Correct splitting:
//     // left → mid
//     // mid+1 → right

//     // Sort left half
//     mergeSort(arr, left, mid);

//     // Sort right half
//     mergeSort(arr, mid + 1, right);

//     // Merge both sorted halves
//     Sort(arr, left, mid, right);
// }

