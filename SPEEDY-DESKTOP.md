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

## Bibliothèque : Tous, Vus et Non vus

À partir de 0.4.18-speedy-pc.1, les grilles de bibliothèque et les listes « Voir tout » affichent les filtres **Tous (N)**, **Vus (N)** et **Non vus (N)**, comme sur le mobile. Les affiches, le nombre de colonnes, le défilement et les aperçus restent ceux de Desktop. Les catalogues des extensions ne sont pas filtrés par ces boutons.

Les compteurs utilisent les mêmes statuts que les coches sur les affiches. Un film vu apparaît dans Vus ; une série n’y apparaît que si elle est marquée vue ou entièrement terminée. Les épisodes vus d’une série encore incomplète restent suivis individuellement dans sa fiche. Les compteurs de la vue en grille portent sur la liste et le type sélectionnés. La modification d’un statut actualise les filtres sans redémarrer l’application.

## Profil et fiches de séries

La version 0.4.19-speedy-pc.1 affiche les statistiques du profil dans **Paramètres → Compte** : en cours, terminés, bibliothèque, durée suivie, activité des sept derniers jours et titres à venir. Les données viennent du profil actif ; la durée connue provient de la progression de lecture et des métadonnées en cache, sans double comptage. Les genres favoris apparaissent sous la grille de statistiques.

Les fiches Desktop indiquent le nombre de saisons et d’épisodes principaux selon les métadonnées disponibles, avec le même calcul que Mobile (épisodes spéciaux et doublons exclus). Les onglets Supporters et Contributeurs affichent uniquement raph de speed ; le pied de page porte « fait par raph de speed ».

## Plusieurs listes M3U

La version 0.4.20-speedy-pc.1 propose plusieurs listes M3U nommées, affichées séparément dans « Mes listes M3U ». Ajoutez une source, renseignez son nom et son URL (ou importez un fichier), puis enregistrez. Cliquez sur le nom d’une liste pour ouvrir ses chaînes et son guide. Les boutons Modifier et Supprimer concernent la liste sélectionnée.

La liste configurée avant cette mise à jour est reprise sous « Ma liste M3U ». Les listes et la sélection sont enregistrées localement selon le profil TV, y compris les données des fichiers importés. Les catégories, la recherche et la sélection de chaîne sont réinitialisées lors du changement de liste. Les chaînes et guides ne sont pas fusionnés. Les favoris restent ceux du profil et sont affichés parmi les chaînes de la liste active.
