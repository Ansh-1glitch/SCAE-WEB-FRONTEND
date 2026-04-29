@echo off
echo ==========================================
echo    SCAE Backend Windows Build ^& Run
echo ==========================================

echo [1/3] Compiling Java classes...
javac -encoding UTF-8 ScaeServer.java handlers\*.java middleware\*.java utils\*.java
if %errorlevel% neq 0 (
    echo [ERROR] Java compilation failed!
    pause
    exit /b %errorlevel%
)
echo [OK] Java classes compiled successfully.

echo [2/3] Compiling C algorithms to scae.dll...
gcc -O2 -Wall -fPIC -I"%JAVA_HOME%\include" -I"%JAVA_HOME%\include\win32" -shared -o scae.dll algorithms\ScaeJNI.c algorithms\dijkstra.c algorithms\bellman_ford.c algorithms\floyd_warshall.c algorithms\bfs.c algorithms\dfs.c algorithms\kruskal.c algorithms\prim.c algorithms\knapsack_01.c algorithms\knapsack_frac.c algorithms\huffman.c algorithms\kmp.c algorithms\rabin_karp.c algorithms\tsp_greedy.c algorithms\tsp_brute.c algorithms\job_seq.c algorithms\activity_sel.c algorithms\heap_sort.c algorithms\lcs.c algorithms\benchmark.c
if %errorlevel% neq 0 (
    echo [ERROR] C compilation failed!
    pause
    exit /b %errorlevel%
)
echo [OK] scae.dll compiled successfully.

echo [3/3] Starting SCAE Server on Port 8000...
echo.
java ScaeServer
pause
