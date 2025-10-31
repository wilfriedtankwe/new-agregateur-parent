
Feature: agregation des données dans la table budget
  l'utilisateur veut pouvoir centraliser l'ensemble de ses transactions dans une table budget en BD
  pour ce faire il doit acceder à 4 fichiers dont un de type CA et 3 autres de type LCL et copier ligne
  par ligne les données qui s'y trouves en respectant les correspondances de champs.




  Scenario Outline: Ouverture d’un fichier selon son état
    Given Un fichier avec le chemin "<chemin>" et donc l'etat est "<etat>"
    When Je tente d'ouvrir le fichier
    Then Le système doit afficher "<message>"

    Examples:
      | chemin                                                                                               | etat                 | message                                                       |
      | src/test/resources/hellocucumber/fichier/CA20250820_115728.csv                                      | existant             | fichier ouvert avec succès                                    |
      | src/test/resources/fichier/!#@!@LCL.csv                                                              | manquant             | une erreur "fichier introuvable" doit être remontée           |
      | !@!*&?*?invalid.csv                                                                                  | chemin invalide      | une erreur "chemin invalide" doit être remontée               |
      | src/test/resources/hellocucumber/fichier/T_cpte_00737_691594V_du_22-05-2025_au_19-08-2025.csv         | existant sans droits | une erreur "permission de lecture refusée" doit être remontée |
      | src/test/resources/hellocucumber/fichier/LCL2.csv                                                     | corrompu             | une erreur "fichier illisible" doit être remontée             |



  Scenario Outline: Fermeture d’un fichier  selon son état
  Given Le fichier avec le chemin "<chemin>" donc est "<etat>"
  When Je tente de fermer le fichier donc l'etat est "<etat>"
  Then Le système doit afficher "<message>"

    Examples:
    | chemin                                                                                              | etat       | message                                            |
    | src/test/resources/hellocucumber/fichier/CA20250820_115728.csv                               | ouverts    | le fichier doit être fermé sans probleme           |
    | src/test/resources/hellocucumber/fichier/T_cpte_00737_691594V_du_22-05-2025_au_19-08-2025.csv| déjà fermé | une erreur "fichier déjà fermé" doit être remontée |