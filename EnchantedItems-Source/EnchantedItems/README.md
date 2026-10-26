# EnchantedItems

Plugin Paper indépendant inspiré du concept d'enchantements sans restrictions.

## Prérequis
- Minecraft Java 1.21.11 avec Paper
- JDK 21
- Maven 3.9+ (ou Maven intégré à IntelliJ IDEA)
- Connexion Internet lors de la première compilation pour télécharger les dépendances

## Compilation
Dans le dossier qui contient `pom.xml` :

```bash
mvn clean package
```

Le fichier sera créé ici :

```text
target/enchanteditems-1.0.0.jar
```

## Installation
1. Arrête le serveur Paper.
2. Copie `target/enchanteditems-1.0.0.jar` dans le dossier `plugins/` du serveur.
3. Redémarre le serveur.
4. Vérifie `/plugins` et la console.
5. Modifie `plugins/EnchantedItems/config.yml` si nécessaire, puis utilise `/ei reload`.

## Commandes
- `/ei help`
- `/ei enchant <identifiant> <niveau>` : applique un enchantement à l'objet tenu en main.
- `/ei reload`

Exemples :
- `/ei enchant sharpness 10`
- `/ei enchant smite 5`
- `/ei enchant mending 1`

Toutes les commandes de modification exigent `enchanteditems.admin` (OP par défaut).

## Remarques
- Le plugin utilise l'API publique Paper/Bukkit et ne modifie pas le client.
- Les fusions à l'enclume sont ajustées via `PrepareAnvilEvent`; les règles vanilla peuvent varier selon l'objet et les versions. Teste d'abord sur un serveur local avec une copie du monde.
- Ce projet est une implémentation originale inspirée d'un concept, pas une copie du code propriétaire d'un autre plugin.
