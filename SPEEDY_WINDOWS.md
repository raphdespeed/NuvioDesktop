# Nuvio Speedy pour Windows

Portage de l'interface et des fonctions communes de Nuvio Speedy mobile sur le lecteur natif de Nuvio Desktop. Version de travail : 0.4.15-speedy (119).

## Adaptations Windows

- Réglages, profils et préférences conservés dans un dossier Nuvio Speedy distinct.
- Import/export des sauvegardes JSON et choix du dossier des téléchargements par les fenêtres Windows.
- TV en direct et guides EPG, fournisseurs portables CloudStream, réglages IA et interfaces Speedy provenant du mobile.
- Commandes de lecture reliées à la couche native Windows, avec fenêtres séparées pour les options Speedy.
- DNS chiffré pour les requêtes de l'application ; les requêtes du lecteur natif utilisent le DNS du système.
- Recherche des mises à jour dirigée vers le fork `raphdespeed/NuvioDesktop`.

## Limites à vérifier avant publication

Cette branche reste un portage en cours tant que la compilation et les essais Windows ne sont pas validés. La présence d'un écran de réglages ne prouve pas que toutes ses fonctions sont prises en charge par le lecteur natif.

La première version de test a passé la compilation JVM Windows, l'affichage de l'écran de sélection des profils, l'enregistrement des préférences et une lecture audio/vidéo locale avec augmentation du volume. Les services externes et tous les réglages avancés n'ont pas été validés avec des comptes réels.

Les extensions CloudStream utilisant du bytecode Android DEX ne fonctionnent pas sous Windows. Les polices personnalisées, les superpositions du lecteur et les fonctions liées au matériel mobile nécessitent une validation ou une adaptation du moteur natif. Les services IA et les intégrations tierces nécessitent leurs configurations habituelles.

## Compilation

Utiliser un JDK avec `jpackage`, le SDK Android requis par le projet multiplateforme, WiX 3 et le SDK NuGet WebView2. La compilation du pont natif nécessite les outils C++ Visual Studio. Un pont précompilé ne doit être réutilisé que si son code source correspond exactement à la version compilée.

Depuis PowerShell, lancer `scripts/build-speedy-windows.ps1` avec `-JavaHome`, `-AndroidSdk`, `-WebView2Sdk` et `-WixDirectory` si ces chemins ne sont pas déjà configurés. Le script échoue explicitement si la compilation échoue. Lorsqu'elle réussit, il place l'installateur et sa somme SHA-256 dans `output_windows`.

Pour cette première compilation locale, le pont natif non modifié est repris de l'installateur officiel `0.1.26-alpha`. Son source C++ a été comparé à la version de référence (objet Git `62d31e21645c14a768d2f3d232b455cfed77cd31`). La somme SHA-256 de l'installateur officiel utilisé est `d88eb7d9b5d59b9d840f68d3f9357b49266aec10feca8acc8aa70e40e6de7dc0`. Les fichiers Kotlin Speedy et l'installateur Windows sont compilés et assemblés localement.
