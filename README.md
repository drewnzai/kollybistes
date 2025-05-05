# Kollybistes Crypto Exchange

A modular crypto exchange platform for atomic swaps between **Bitcoin (BTC)** and **Ethereum (ETH)**.
While currently running on private test networks, one can also use it on the main networks with a tweaking of the respective crypto nodes.

**The project only has the Java backend for now but the React frontend will come soon.**

**I am developing on a Linux PC, since Windows development is a pain**

---

## 📦 Modules
The sub-modules within the ```backend``` module
- `core` – Core logic: wallet handling, blockchain interaction, trading services.
- `notification` – Kafka consumer service that sends email notifications.
- `common` – Shared DTOs and model classes.

---

## 🧰 Required Software

| Software              | Purpose                          | Setup Guide |
|-----------------------|----------------------------------|-------------|
| **Java 17+**           | Backend runtime                 | [AdoptOpenJDK](https://adoptium.net) |
| **Maven**              | Backend Build Tool              | [Maven](https://maven.apache.org/install.html) |
| **MySQL**              | Database                        | [MySQL](https://dev.mysql.com/downloads/installer/) |
| **Redis**              | Cache layer                     | [Redis Docs](https://redis.io/docs/getting-started/) |
| **Kafka + Zookeeper**  | Message broker                  | [Kafka Quickstart](https://kafka.apache.org/quickstart) |
| **Geth (Ethereum)**    | Private ETH blockchain          | [Ethereum Setup Guide](https://github.com/LifnaJos/Private-Ethereum-Blockchain-setup-using-Geth/blob/main/README.md) |
| **Bitcoin Core**       | BTC regtest network             | [YouTube Guide](https://www.youtube.com/watch?v=6tIshGsVb5c), [Gist](https://gist.github.com/System-Glitch/cb4e87bf1ae3fec9925725bb3ebe223a) |

---
*I have provided a sample ```bitcoin.conf``` file that I used to setup my bitcoin regtest node and a ```genesis.json``` for my Ethereum private network node.*


## ⚙️ Configuration

### 1. Clone and Prepare

```bash
git clone https://github.com/drewnzai/kollybistes.git
cd kollybistes/backend
cp core/src/main/resources/sample.properties core/src/main/resources/application.properties
cp notification/src/main/resources/sample.properties notification/src/main/resources/application.properties
```

Edit each application.properties file to set up:

- MySQL credentials
- Redis host/port
- JWT secret
- Wallet paths and credentials

### 2. Launch Infrastructure Services

#### 2.1 **Redis**
```bash
redis-server
```

#### 2.2 **MySQL**
Start MySQL and create the schema:
```bash
CREATE DATABASE kollybistes;
```

#### 2.3 **Kafka + Zookeeper**
In one terminal after moving to the directory where you stored your Kafka folder:

*Start Zookeeper*
```bash
/bin/zookeeper-server-start.sh config/zookeeper.properties
```
In another terminal:

*Start Kafka*
```bash
/bin/kafka-server-start.sh config/server.properties
```
#### 2.4 **Bitcoin Core (RegTest)**
```bash
bitcoin-qt
```

#### 2.5 **Geth (Ethereum)**
```bash
geth --datadir "/home/{username}/{ethereum-location}/private/kollybistes"      --networkid 2025  --port 30306    --http --http.addr "127.0.0.1" --http.port 8545      --http.corsdomain "*" --http.vhosts "*"      --http.api "eth,web3,net,txpool,miner"      --ws --ws.addr "127.0.0.1" --ws.port 8546 --ws.origins "*"      --authrpc.jwtsecret "/home/{username}/{ethereum-location}/private/jwt.secret"      --mine --miner.etherbase "0xYour-System_wallet"      --unlock "0xYour-System_wallet"      --password "/home/{username}/{ethereum-location}/private/password.txt"      --allow-insecure-unlock      --nodiscover      --syncmode full
```

- *{username} - Your PC username*
- *{ethereum-location} - Your Ethereum setup location*
- *{Your-System-Wallet} - The wallet address you will use for the application* 

## ▶️ Run Applications

Assuming you have installed Maven globally, all modules will use ``` mvn ``` for uniformity, otherwise use ``` mvnw``` (Maven Wrapper)

### Backend Root Module
This pulls all the required dependencies and builds the sub-modules

```bash
cd backend
mvn clean install
```

### Core Sub-Module

```bash
cd ../core
mvn spring-boot:run
```
Runs at: http://localhost:8080

Swagger UI: ```/swagger-ui/index.html```

### Notification Sub-Module

```bash
cd ../notification
mvn spring-boot:run
```
Runs on: http://localhost:8081


## 🧪 API Testing

Use Postman or Swagger UI (```/swagger-ui/index.html```) to test:
- Balance endpoints
- Exchange endpoints
- Admin/system wallets
- Rates and fee fetchers


## 📝 License

GNU GPLv3 — see ```LICENSE.md.```
