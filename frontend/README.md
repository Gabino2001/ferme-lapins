# Ferme des Lapins

Application de gestion de ferme de lapins : lapins (fiche détaillée), cages (plan), reproduction (saillies, mises bas, sevrage),
engraissement, santé (maladies + traitements conseillés), argent (ventes, dépenses, gain ou perte) et stock.
Un moteur d’alertes transforme les données en problèmes + solutions, poussés en temps réel (SSE).

- `backend/`  : Spring Boot 3.2.5, Java 21, JPA, MySQL, JWT
- `frontend/` : Flutter, Riverpod, Dio

## 1. Backend

Structure (`backend/src/main/java/com/ferme/lapins/`) :

```
config/      SecurityConfig, OpenApiConfig
controller/  AuthController, FermeController, CrudController + un contrôleur par entité
service/     FermeService, AlerteService, CageService, BilanService, DashboardService, AlerteStream (temps réel)
repository/  OwnedRepo + un repository par entité (Spring Data JPA)
model/       entités JPA (Lapin, Cage, Saillie, Portee, LotEngraissement, CasMaladie, StockItem, Mouvement...) + Guide
dto/         Dtos (requêtes), Alerte, Dashboard, Bilan, CageEtat
security/    JwtService, JwtFilter
event/       FermeChanged
exception/   GlobalExceptionHandler
util/        Util
```

### MySQL en local
```
cd backend
docker compose up -d                 # MySQL 8.4 (root / root, base ferme_lapins)
export DB_PASSWORD=root              # Windows PowerShell : $env:DB_PASSWORD="root"
mvn spring-boot:run
```
Sans Docker : installe MySQL 8, puis `CREATE DATABASE ferme_lapins CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`
et définis `DB_USER` / `DB_PASSWORD`. Les tables se créent seules (`ddl-auto=update`).

### Variables d’environnement

| Variable | Valeur par défaut (dev) | En production |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/ferme_lapins?...` | URL de ton MySQL hébergé, ex. `jdbc:mysql://hote:3306/ferme_lapins?useSSL=true&serverTimezone=UTC` |
| `DB_USER` | `root` | utilisateur MySQL |
| `DB_PASSWORD` | vide | mot de passe MySQL |
| `JWT_SECRET` | clé de développement | **obligatoire** : phrase aléatoire de 32 caractères minimum |
| `CORS_ORIGINS` | `*` | URL de ton front web, séparées par des virgules |

Documentation interactive de l’API : `http://localhost:8080/swagger-ui.html` (bouton Authorize : coller le token reçu au login).
Santé : `/actuator/health`.

Déploiement avec le `Dockerfile` (Render, Railway...) : Render ne propose pas de MySQL géré, utilise un MySQL hébergé ailleurs et renseigne `DB_URL`, `DB_USER`, `DB_PASSWORD`.

## 2. Frontend

```
cd frontend
flutter create .          # génère les dossiers android/ios/web sans toucher à lib/
flutter pub get
flutter run --dart-define=API_URL=https://ton-backend.onrender.com
```
Émulateur Android avec backend local : l’URL par défaut `http://10.0.2.2:8080` fonctionne.
Sur Android en HTTP (local), autoriser le trafic non chiffré dans `AndroidManifest.xml` (`android:usesCleartextTraffic="true"`).

## 3. Principaux endpoints (tous sous JWT sauf /api/auth)

- `POST /api/auth/register`, `POST /api/auth/login`
- CRUD : `/api/lapins`, `/api/cages`, `/api/saillies`, `/api/portees`, `/api/lots`, `/api/maladies`, `/api/stock`, `/api/mouvements`
- Actions : `POST /api/saillies/{id}/palpation?resultat=ok|non`, `/api/saillies/{id}/mise-bas`, `/api/portees/{id}/sevrage`,
  `/api/lots/{id}/vente`, `/api/lots/{id}/perte`, `/api/lapins/{id}/pesees`, `/api/maladies/{id}/statut?statut=GUERI|DECEDE`, `/api/stock/{id}/reappro?quantite=`
- Lecture : `/api/dashboard`, `/api/alertes`, `/api/alertes/stream` (SSE), `/api/cages/plan`, `/api/bilan?jours=30`, `/api/maladies/guide`

Chaque utilisateur ne voit que ses propres données (colonne `utilisateur_id` sur toutes les tables).
Devise par défaut : FCFA (fonction `fcfa` dans `frontend/lib/core.dart`).

## Phase 1 des améliorations

- **Vaccins et traitements** : `/api/soins` (+ `POST /api/soins/{id}/realise`). Rappel automatique : si « répéter tous les N jours » est renseigné, le soin suivant est planifié quand on marque le précédent comme fait. Alertes « en retard » et « à prévoir sous 7 jours ».
- **Indicateurs de performance** : `GET /api/statistiques` (taille des portées, mortalité naissance → sevrage, coût d’aliment par lapin vendu, gain/perte par mois, classement des lapines). Écran accessible depuis l’Accueil et le menu du compte.
- **Mot de passe** : `POST /api/compte/mot-de-passe` (menu du compte dans l’application).

## Phase 2 : sur le terrain

- **Mode hors-ligne** : les listes déjà chargées restent consultables sans réseau (cache sur le téléphone). Les saisies faites sans réseau sont mises en file d’attente et envoyées automatiquement au retour du réseau (bandeau orange en haut avec le nombre de modifications en attente). Limite : une saisie faite hors-ligne n’apparaît dans les listes qu’après synchronisation.
- **Tâches du jour** (Accueil) : tâches récurrentes à cocher (`/api/taches`, bouton « Ajouter » pour les tâches habituelles) + alertes à traiter aujourd’hui.
- **Photo d’un lapin** : sur la fiche, toucher la photo (caméra ou galerie). Stockée réduite (640 px) en base : `/api/lapins/{id}/photo`.
- **Codes QR** : sur la fiche d’un lapin (icône QR) et en touchant une cage dans le plan. Le format est `ferme:lapin:ID` ou `ferme:cage:NUMERO`. L’icône scanner en haut de l’écran ouvre la fiche correspondante (caméra sur Android/iOS, saisie manuelle du code sur le web).
- **Notifications push** : résumé quotidien à 7 h (heure de Douala) des points à traiter. Voir ci-dessous.

### Activer les notifications push (Firebase)

1. Sur https://console.firebase.google.com créer un projet (gratuit), puis **Ajouter une application Android** avec le nom de package de `frontend/android/app/build.gradle(.kts)` (`applicationId`, par défaut `com.example.ferme_lapins`).
2. Télécharger `google-services.json` et le placer dans `frontend/android/app/`.
3. Plugin Google Services : dans `frontend/android/settings.gradle(.kts)`, bloc `plugins`, ajouter `id("com.google.gms.google-services") version "4.4.2" apply false`. Dans `frontend/android/app/build.gradle(.kts)`, bloc `plugins`, ajouter `id("com.google.gms.google-services")`.
4. Console Firebase > Paramètres du projet > **Comptes de service** > Générer une nouvelle clé privée (fichier JSON).
5. Backend : définir la variable d’environnement `FIREBASE_CREDENTIALS` avec le **contenu** du JSON (ou le chemin du fichier). Au démarrage, le journal affiche « Notifications push activées ».
6. Lancer l’application sur le téléphone Android, se connecter (le jeton de l’appareil est enregistré automatiquement), puis tester avec `POST /api/appareils/test` (Swagger).

Variables facultatives : `PUSH_CRON` (défaut `0 0 7 * * *`) et `PUSH_ZONE` (défaut `Africa/Douala`).

### iOS (si besoin)
Dans `frontend/ios/Runner/Info.plist`, ajouter `NSCameraUsageDescription` et `NSPhotoLibraryUsageDescription` (photos et scanner). Les notifications push iOS demandent en plus une clé APNs dans Firebase.

## Phase 3 : gestion de l'élevage et de l'équipe

- **Généalogie** : chaque lapin peut avoir un père et une mère de l'élevage (`pereId`, `mereId`) ou un parent extérieur (nom libre). La fiche affiche l'ascendance sur 3 générations et les descendants. `GET /api/genealogie/{id}`.
- **Alerte de consanguinité** : avant d'enregistrer une saillie, le lien de parenté est vérifié (`GET /api/genealogie/parente`). Niveaux : élevé (parent/enfant, frère/sœur, grand-parent, oncle/nièce), moyen (cousins germains, arrière-grand-parent), faible (cousins éloignés). L'éleveur peut passer outre après confirmation. Analyse basée sur les parents enregistrés, sur 3 générations.
- **Planning de reproduction** (Élevage > Planning) : état de chaque lapine (prête, gestante, allaitante, au repos, trop jeune), date clé, mâle conseillé (le moins apparenté), et conseil de réforme si les portées sont petites. Le rythme de ressaillie (11, 21 ou 42 jours) est réglable : ce sont des repères généraux. `GET /api/planning?delai=21`.
- **Clients et commandes** (Argent > Clients et commandes) : clients, commandes avec acompte, livraison (option : retirer les lapins d'un lot d'engraissement, la vente est alors enregistrée dans l'argent), paiements, impayés. Alertes de livraison et de paiement en retard.
- **Équipe et droits** (menu du compte > Équipe et droits) :

| Rôle | Droits |
|---|---|
| PROPRIETAIRE | tout |
| OUVRIER | tout le travail de la ferme, sans l'argent, les clients, les statistiques ni l'équipe |
| LECTEUR | consultation seulement |

Le propriétaire crée les comptes de ses employés (email + mot de passe provisoire) et peut changer un rôle ou retirer quelqu'un : l'effet est immédiat. Tous les membres d'une même ferme partagent les mêmes données. Les alertes d'argent et de clients sont masquées aux non-propriétaires.
Les comptes existants restent propriétaires. Le jeton de connexion contient seulement l'identité de la personne : son rôle est relu en base à chaque requête.

## Phase 4 : intelligence, exports et confort

### Assistant (Claude)
Bouton étincelle en haut de l'écran. Il connaît les lapins, les alertes, le stock, la reproduction et (pour le propriétaire) les comptes. Questions en texte, à la voix, ou avec une photo d'un lapin. Il donne des pistes, jamais de diagnostic ni de dose.
Backend : variable d'environnement `ANTHROPIC_API_KEY` (clé créée sur https://console.anthropic.com). Facultatives : `ANTHROPIC_MODEL` (défaut `claude-sonnet-5-5`) et `ASSISTANT_LIMITE` (questions par heure et par personne, défaut 30). Sans clé, l'assistant répond qu'il n'est pas configuré. Les questions et un résumé de la ferme sont envoyés à l'API d'Anthropic : le dire aux utilisateurs.
`POST /api/assistant`, `GET /api/assistant/etat`.

### Prévisions
Accueil > Prévisions à 30 jours : mises bas et lapereaux attendus, sevrages, lots prêts à vendre, soins, livraisons, risques de rupture de stock, ventes estimées (propriétaire). `GET /api/previsions`.

### Exports Excel et PDF (propriétaire)
Argent > Exporter. `POST /api/export/lien?type=excel|pdf` renvoie un lien valable 5 minutes, ouvert dans le navigateur du téléphone (le fichier se télécharge). Excel : une feuille par domaine (lapins, portées, engraissement, maladies, soins, argent, commandes, stock). PDF : situation, alertes, bilan de 30 jours, dépenses, reproduction.

### Thème sombre
Menu du compte > Thème : clair, sombre ou automatique. Mémorisé sur l'appareil.

### Saisie vocale
Micro dans l'assistant et dans tous les champs texte des formulaires. Android : ajouter dans `android/app/src/main/AndroidManifest.xml`, avant `<application>` :
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO"/>
<queries>
  <intent><action android:name="android.speech.RecognitionService"/></intent>
  <intent><action android:name="android.intent.action.VIEW"/><data android:scheme="https"/></intent>
</queries>
```
(la seconde règle `queries` sert au téléchargement des exports). iOS : `NSMicrophoneUsageDescription` et `NSSpeechRecognitionUsageDescription` dans `Info.plist`.

### Tests automatiques
`cd backend && mvn test` : consanguinité, planning de reproduction, bilan (gain, perte, coût de revient).

### Migrations de base de données (Flyway) : à faire sur ta base réelle
Aujourd'hui les tables sont créées par Hibernate (`ddl-auto=update`), ce qui convient au développement. Pour la production :
1. `mysqldump --no-data --skip-comments -u root -p ferme_lapins > V1__init.sql` (retirer les `AUTO_INCREMENT=n` de fin de table).
2. Placer le fichier dans `backend/src/main/resources/db/migration/`.
3. Ajouter `flyway-core` et `flyway-mysql` dans le `pom.xml`.
4. Dans `application.properties` : `spring.flyway.baseline-on-migrate=true`, `spring.flyway.baseline-version=1`, `spring.jpa.hibernate.ddl-auto=validate`.
Chaque évolution du schéma devient ensuite un fichier `V2__...sql`.

## Phase 5 : finitions et mise en production

- **Courbe de croissance d'un lot** (Élevage > Engraissement > Peser / Courbe) : poids moyen d'un lapin à chaque pesée, gain en g/jour, alerte de ralentissement (aide à choisir le moment de vente). `GET|POST /api/lots/{id}/pesees`.
- **Mot de passe oublié** : sur l'écran de connexion, un code à 6 chiffres est envoyé par email (valable 15 minutes, 5 essais, une demande par minute). Le propriétaire peut aussi définir un nouveau mot de passe pour un membre de l'équipe (Équipe et droits > Nouveau mot de passe). Configuration de l'envoi d'emails (variables d'environnement du backend) :
  `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT` (587), `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true`, `SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true`, et facultativement `MAIL_FROM`. Avec Gmail : `smtp.gmail.com`, et un « mot de passe d'application ». Sans SMTP configuré, le code est écrit dans le journal du serveur (utile en développement).
- **Connexion protégée** : 10 mots de passe faux en 15 minutes bloquent la connexion de cet email pour un moment.
- **Tablette et ordinateur** : à partir de 900 px de large, menu latéral à gauche et contenu centré.
- **Devise réglable** : menu du compte > Devise (propriétaire). Elle s'applique aux écrans, aux alertes, au PDF et à l'assistant. `GET|PUT /api/config`.
- **Sauvegardes** : `scripts/sauvegarde.sh` (Linux/cron) et `scripts/sauvegarde.ps1` (Windows, Planificateur de tâches). Garde 14 jours. Sur un hébergeur MySQL, activer aussi ses sauvegardes automatiques.

### Liste de contrôle avant la mise en production
1. `JWT_SECRET` : phrase aléatoire d'au moins 32 caractères (jamais la valeur par défaut).
2. `DB_URL`, `DB_USER`, `DB_PASSWORD` : base MySQL hébergée, connexion chiffrée (`useSSL=true`).
3. `CORS_ORIGINS` : l'adresse de ton application web, au lieu de `*`.
4. Désactiver la documentation publique de l'API : `SPRINGDOC_SWAGGER_UI_ENABLED=false` et `SPRINGDOC_API_DOCS_ENABLED=false`.
5. HTTPS obligatoire (Render le fournit). Dans l'application : `--dart-define=API_URL=https://...`.
6. Notifications : `FIREBASE_CREDENTIALS`. Assistant : `ANTHROPIC_API_KEY`. Emails : variables `SPRING_MAIL_*`.
7. Sauvegardes planifiées et restauration testée une fois.
8. `mvn test` au vert avant chaque déploiement.
