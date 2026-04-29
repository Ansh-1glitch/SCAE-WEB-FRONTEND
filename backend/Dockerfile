# Dockerfile — SCAE Backend ke liye
# gcc + make + Java 17 sab ek image mein

FROM eclipse-temurin:17-jdk

# gcc aur make install karo (C compile karne ke liye)
RUN apt-get update && apt-get install -y gcc make && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# saara source code copy karo
COPY . .

# C files compile karo → libscae.so banao
RUN make linux

# Java files compile karo
RUN javac -cp . ScaeServer.java handlers/*.java utils/*.java middleware/*.java

# PORT expose karo
EXPOSE 8000

# server start karo
CMD java -Djava.library.path=. ScaeServer
