# Simulateur de Marché Financier Backend

Backend Spring Boot pour un simulateur de trading en temps réel avec WebSocket/STOMP, gestion des ordres, matching automatique et notifications temps réel.

## 🚀 Fonctionnalités

- **Gestion des ordres** : BUY/SELL avec types LIMIT et MARKET
- **Matching automatique** : Engine de matching price-time avec PriorityQueues
- **Transactions ACID** : Isolation READ_COMMITTED pour garantir la cohérence
- **Règlement portefeuille** : Gestion atomique des positions et trésorerie
- **WebSocket temps réel** : Notifications exactly-once post-commit
- **Carnet d'ordres** : Affichage en temps réel des meilleurs prix
- **Tests d'intégration** : Validation des scénarios critiques

## 🏗️ Architecture

### Cycle des statuts d'ordre

```
NEW → VALIDATING → QUEUED → PARTIALLY_FILLED/FILLED
  ↓                    ↓
REJECTED           CANCELLED
```

### Composants principaux

- **OrderService** : Gestion des ordres avec transactions
- **PortfolioSettlementService** : Règlement des portefeuilles
- **OrderBook** : Carnet d'ordres avec PriorityQueues price-time
- **EventPublisher** : Publication d'événements post-commit
- **WebSocket** : Notifications temps réel

## 🔧 Configuration

### Prérequis

- Java 21+
- Maven 3.8+
- MySQL 8.0+ (ou H2 pour les tests)

### Variables d'environnement

```properties
# Base de données
spring.datasource.url=jdbc:mysql://localhost:3306/tradingsimulateur
spring.datasource.username=root
spring.datasource.password=

# Port et contexte
server.port=9090
server.servlet.context-path=/examen

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

## 🚀 Démarrage

### 1. Compilation et tests

```bash
# Tests d'intégration
mvn -q -DskipTests=false test

# Compilation complète
mvn clean compile
```

### 2. Lancement de l'application

```bash
# Avec Maven
mvn spring-boot:run

# Ou avec le wrapper
./mvnw spring-boot:run
```

### 3. Accès aux interfaces

- **API REST** : http://localhost:9090/examen/api/
- **Swagger UI** : http://localhost:9090/examen/swagger-ui/index.html
- **WebSocket** : ws://localhost:9090/examen/ws

## 📡 WebSocket Topics

### Connexion WebSocket

```javascript
const socket = new SockJS('/examen/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);
});
```

### Topics disponibles

#### 1. Statut des ordres
```javascript
stompClient.subscribe('/topic/orders/status/{userId}', function(message) {
    const order = JSON.parse(message.body);
    console.log('Ordre mis à jour:', order);
});
```

**Exemple de message :**
```json
{
  "id": 123,
  "userId": 456,
  "assetId": 789,
  "side": "BUY",
  "type": "LIMIT",
  "status": "FILLED",
  "price": 100.50,
  "quantity": 10,
  "remainingQuantity": 0,
  "createdAt": "2025-01-23T10:30:00Z"
}
```

#### 2. Transactions
```javascript
stompClient.subscribe('/topic/transactions/{userId}', function(message) {
    const trade = JSON.parse(message.body);
    console.log('Transaction exécutée:', trade);
});
```

**Exemple de message :**
```json
{
  "id": 789,
  "assetId": 123,
  "buyOrderId": 456,
  "sellOrderId": 789,
  "price": 100.25,
  "quantity": 5,
  "executedAt": "2025-01-23T10:30:15Z"
}
```

#### 3. Carnet d'ordres
```javascript
stompClient.subscribe('/topic/orderbook/{assetId}', function(message) {
    const orderBook = JSON.parse(message.body);
    console.log('Carnet mis à jour:', orderBook);
});
```

**Exemple de message :**
```json
{
  "bids": [
    {"price": 100.00, "quantity": 10},
    {"price": 99.50, "quantity": 5}
  ],
  "asks": [
    {"price": 100.50, "quantity": 8},
    {"price": 101.00, "quantity": 12}
  ],
  "bestBid": 100.00,
  "bestAsk": 100.50
}
```

## 🔄 Règles de matching

### Priorité price-time

1. **BUY** : Prix décroissant puis antériorité (FIFO à prix égal)
2. **SELL** : Prix croissant puis antériorité (FIFO à prix égal)

### Prix d'exécution

- **LIMIT vs LIMIT** : Prix de l'ordre le plus ancien
- **MARKET vs LIMIT** : Prix de l'ordre LIMIT
- **MARKET vs MARKET** : Prix de référence

### Conditions de matching

- **BUY** peut matcher si `prix >= meilleur ASK`
- **SELL** peut matcher si `prix <= meilleur BID`

## 💰 Règlement portefeuille

### Ordre d'ACHAT (BUY)

```java
// Réservation
reservedCash += prix * quantité

// Exécution
cash -= prix_exécution * quantité
reservedCash -= prix_limite * quantité
positions[asset] += quantité

// Remboursement si exécution à meilleur prix
if (prix_exécution < prix_limite) {
    cash += (prix_limite - prix_exécution) * quantité
}
```

### Ordre de VENTE (SELL)

```java
// Réservation
reserved[asset] += quantité

// Exécution
reserved[asset] -= quantité
positions[asset] -= quantité
cash += prix_exécution * quantité
```

### Annulation

```java
// BUY
reservedCash -= prix * quantité_restante

// SELL
reserved[asset] -= quantité_restante
```

## 🧪 Tests d'intégration

### Scénarios testés

1. **Fonds insuffisants** : Ordre BUY rejeté, aucune réservation
2. **Quantité insuffisante** : Ordre SELL rejeté, aucune réservation
3. **Exécution partielle + annulation** : Portefeuille cohérent, réservations libérées
4. **FIFO à prix égal** : L'ordre le plus ancien est servi en premier

### Exécution des tests

```bash
# Tous les tests
mvn test

# Tests d'intégration uniquement
mvn test -Dtest=OrderServiceIntegrationTest

# Avec logs détaillés
mvn test -Dlogging.level.tn.esprit.examen.nomPrenomClasseExamen=DEBUG
```

## 📊 API Endpoints

### Ordres

- `POST /api/orders` - Créer un ordre
- `DELETE /api/orders/{id}` - Annuler un ordre
- `GET /api/orders/user/{userId}` - Ordres d'un utilisateur
- `GET /api/orders/user/{userId}/trades` - Transactions d'un utilisateur
- `GET /api/orders/orderbook/{assetId}` - Carnet d'ordres

### Ordres de session

- `POST /api/session-orders` - Créer un ordre de session
- `POST /api/session-orders/{id}/execute` - Exécuter un ordre
- `POST /api/session-orders/{id}/cancel` - Annuler un ordre
- `GET /api/session-orders/session/{sessionId}` - Ordres d'une session

## 🔒 Sécurité

- **Isolation READ_COMMITTED** : Évite les dirty reads
- **Transactions ACID** : Atomicité des opérations
- **Validation des fonds** : Vérification avant réservation
- **Publication exactly-once** : Événements post-commit uniquement

## 🚀 Déploiement

### Variables d'environnement de production

```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_URL=jdbc:mysql://prod-db:3306/trading
export DB_USERNAME=trading_user
export DB_PASSWORD=secure_password
```

### Docker (optionnel)

```dockerfile
FROM openjdk:21-jdk-slim
COPY target/*.jar app.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## 📝 Logs et monitoring

### Logs importants

- `📝 Placement ordre` : Création d'ordre
- `🔄 Matching` : Processus de matching
- `⚡ Exécution` : Exécution de trade
- `❌ Annulation` : Annulation d'ordre
- `💰 Règlement` : Mise à jour portefeuille

### Métriques recommandées

- Nombre d'ordres par seconde
- Latence de matching
- Taux de rejet
- Volume traité

## 🤝 Contribution

1. Fork le projet
2. Créer une branche feature (`git checkout -b feature/AmazingFeature`)
3. Commit les changements (`git commit -m 'Add some AmazingFeature'`)
4. Push vers la branche (`git push origin feature/AmazingFeature`)
5. Ouvrir une Pull Request

## 📄 Licence

Ce projet est sous licence MIT. Voir le fichier `LICENSE` pour plus de détails.
