# framework

## Dependencies

Ce framework utilise certaines fonctionnalités optionnelles qui nécessitent des dépendances externes fournies par le développeur.

### JSON Support (JsonUtil, ApiResponse, ErrorInfo)
Si vous utilisez `JsonUtil`, `ApiResponse` ou `ErrorInfo`, vous devez ajouter les bibliothèques Jackson suivantes à votre classpath :

- **Jars à télécharger** :
  - `jackson-annotations-2.20.jar`
  - `jackson-core-2.20.1.jar`
  - `jackson-databind-2.20.1.jar`

- **Via Maven** (ajoutez dans votre `pom.xml`) :
  ```xml
  <dependencies>
    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-databind</artifactId>
      <version>2.20.1</version>
    </dependency>
    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-core</artifactId>
      <version>2.20.1</version>
    </dependency>
    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-annotations</artifactId>
      <version>2.20</version>
    </dependency>
  </dependencies>
  ```

- **Via Gradle** (ajoutez dans votre `build.gradle`) :
  ```gradle
  dependencies {
    implementation 'com.fasterxml.jackson.core:jackson-databind:2.20.1'
    implementation 'com.fasterxml.jackson.core:jackson-core:2.20.1'
    implementation 'com.fasterxml.jackson.core:jackson-annotations:2.20'
  }
  ```

Le framework détectera automatiquement l'absence de ces dépendances au démarrage et loguera un avertissement.