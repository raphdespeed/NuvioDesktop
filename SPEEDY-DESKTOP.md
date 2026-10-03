# Nuvio Speedy pour Windows

Cette branche repart de Nuvio Desktop (`e166b226`). Les écrans d’accueil, de recherche, de bibliothèque, les fiches et les réglages Desktop sont conservés. Les fonctions Speedy sont intégrées dans cette interface PC.

## TV en direct

L’entrée « TV en direct » de la navigation ouvre trois zones : catégories et favoris, liste de chaînes avec recherche, puis guide des programmes et bouton de lecture. Un clic sélectionne une chaîne ; un double-clic lance la lecture dans le lecteur Desktop.

Sources prises en charge : URL M3U/M3U8, fichier M3U local, Xtream et portail Stalker. Les informations de connexion sont enregistrées localement, par profil. Le guide XMLTV peut être compressé en GZIP, XZ ou ZIP. Les favoris et la dernière chaîne sont conservés par profil.

Les contrôles de lecture, le plein écran et les raccourcis sont ceux du lecteur Windows de Nuvio Desktop. Les sources et guides restent soumis à leur disponibilité et aux droits d’accès de votre fournisseur.

## État du portage

La reconstruction actuelle couvre la base Desktop et la TV en direct. L’assistant IA, les extensions CloudStream propres au mobile et les autres ajouts Speedy demandent encore leur adaptation dans les écrans PC. Cette branche ne doit pas être présentée comme un portage complet de toutes les fonctions mobiles.

L’ancien portage qui reprenait l’interface mobile reste disponible sur `codex/speedy-windows` pour référence. Le développement Desktop natif est sur `codex/speedy-desktop-native`.

## Vérification

Les tests `DesktopLiveTvTest` vérifient les identifiants du guide, la recherche et la sélection de la chaîne à lire, les métadonnées M3U, le rapprochement XMLTV et le décodage borné des guides compressés. Ils produisent une capture d’écran dans `composeApp/build/test-artifacts/desktop-live-tv.png`.

```powershell
./gradlew.bat :composeApp:desktopTest --tests 'com.nuvio.app.features.livetv.DesktopLiveTvTest'
```

## Connexion au compte

À partir de 0.4.17-speedy-pc.1, l’installateur intègre la configuration publique du serveur de compte utilisé par Nuvio Speedy. La version précédente avait été distribuée sans cette configuration et ne pouvait pas se connecter.

La création de l’installateur exige `NUVIO_SUPABASE_URL` et `NUVIO_SUPABASE_ANON_KEY` dans `local.properties` (fichier ignoré par Git) ou dans l’environnement. Le test réseau peut être activé avec `SPEEDY_VERIFY_AUTH_BACKEND=1` et vérifie uniquement les paramètres publics du serveur, sans mot de passe ni connexion à un compte personnel.

Le logo visible affiche Nuvio Speedy sur la connexion, le chargement et les réglages. Le suffixe de statut de membre ne fait plus partie du nom affiché.
