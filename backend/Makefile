# Makefile — C algorithm files compile karne ke liye
# make         → macOS pe libscae.dylib banata hai (local dev)
# make linux   → Linux/Render pe libscae.so banata hai
# make windows → scae.dll banata hai

CC = gcc
CFLAGS = -O2 -Wall -fPIC
ALGOS_DIR = algorithms

# macOS darwin JNI headers
JNI_INCLUDE_MAC   = -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/darwin
# Linux JNI headers
JNI_INCLUDE_LINUX = -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux

# saare C files ko ek shared library mein compile karo
SRCS = $(ALGOS_DIR)/ScaeJNI.c \
       $(ALGOS_DIR)/dijkstra.c \
       $(ALGOS_DIR)/bellman_ford.c \
       $(ALGOS_DIR)/floyd_warshall.c \
       $(ALGOS_DIR)/bfs.c \
       $(ALGOS_DIR)/dfs.c \
       $(ALGOS_DIR)/kruskal.c \
       $(ALGOS_DIR)/prim.c \
       $(ALGOS_DIR)/knapsack_01.c \
       $(ALGOS_DIR)/knapsack_frac.c \
       $(ALGOS_DIR)/huffman.c \
       $(ALGOS_DIR)/kmp.c \
       $(ALGOS_DIR)/rabin_karp.c \
       $(ALGOS_DIR)/tsp_greedy.c \
       $(ALGOS_DIR)/tsp_brute.c \
       $(ALGOS_DIR)/job_seq.c \
       $(ALGOS_DIR)/activity_sel.c \
       $(ALGOS_DIR)/heap_sort.c \
       $(ALGOS_DIR)/lcs.c \
       $(ALGOS_DIR)/benchmark.c

# macOS ke liye dylib banao (local dev)
all:
	$(CC) $(CFLAGS) $(JNI_INCLUDE_MAC) -dynamiclib -o libscae.dylib $(SRCS)
	@echo "✅ libscae.dylib compiled successfully (macOS)"

# Linux/Render ke liye shared library banao
linux:
	$(CC) $(CFLAGS) $(JNI_INCLUDE_LINUX) -shared -o libscae.so $(SRCS)
	@echo "✅ libscae.so compiled successfully (Linux)"

# Windows ke liye DLL banao
windows:
	$(CC) $(CFLAGS) -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/win32 \
	      -shared -o scae.dll $(SRCS)
	@echo "✅ scae.dll compiled successfully"

# purani compiled files delete karo
clean:
	rm -f libscae.dylib libscae.so scae.dll
	@echo "🧹 Cleaned compiled files"
