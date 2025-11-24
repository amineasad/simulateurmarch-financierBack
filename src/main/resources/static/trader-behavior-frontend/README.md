# Interface d'Analyse de Comportement des Traders

Cette interface web permet de visualiser les résultats d'analyse de comportement des traders générés par les scripts Python situés dans le dossier `Comportement`.

## Comment exécuter l'application

### Prérequis
- Java 8 ou supérieur
- Maven
- Python 3.x avec les bibliothèques pandas et numpy installées

### Étapes d'exécution

1. **Démarrer l'application Spring Boot**

   Depuis la racine du projet, exécutez la commande suivante :
   ```
   mvnw spring-boot:run
   ```
   
   Si vous rencontrez des problèmes avec le wrapper Maven, vous pouvez utiliser Maven directement si installé :
   ```
   mvn spring-boot:run
   ```

2. **Accéder à l'interface**

   Une fois l'application démarrée, ouvrez votre navigateur et accédez à :
   ```
   http://localhost:8080/trader-behavior-frontend/index.html
   ```

3. **Utiliser l'interface**

   - Le tableau de bord affiche un résumé des stratégies analysées
   - Cliquez sur le bouton "Actualiser" pour exécuter une nouvelle analyse
   - Les résultats sont présentés sous forme de tableau et de graphiques

## Structure des fichiers

- `index.html` : Page principale de l'interface
- `styles.css` : Styles CSS pour l'interface (thème bleu foncé orienté finance)
- `app.js` : Logique JavaScript pour l'interaction avec le backend

## API Backend

L'application expose les endpoints REST suivants :

- `GET /api/trader-analysis` : Récupère les résultats d'analyse des traders
- `POST /api/trader-analysis/run` : Exécute l'analyse des comportements des traders

## Dépannage

Si vous rencontrez des problèmes lors de l'exécution :

1. Vérifiez que Python est correctement installé et accessible dans le PATH
2. Assurez-vous que les bibliothèques Python requises sont installées :
   ```
   pip install pandas numpy
   ```
3. Vérifiez les logs de l'application Spring Boot pour identifier d'éventuelles erreurs