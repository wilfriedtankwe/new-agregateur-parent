Feature: agregation des données dans la table budget
  l'utilisateur veut pouvoir centraliser l'ensemble de ses transactions dans une table budget en BD
  pour ce faire il doit acceder à 4 fichiers dont un de type CA et 3 autres de type LCL et copier ligne
  par ligne les données qui s'y trouves en respectant les correspondances de champs.

  Background:
    Given les fichiers suivants sont disponibles dans le répertoire d'entrée
      | fichier                                                                                              |
      | src/test/resources/hellocucumber/fichier/CA20250820_115728.csv                                |
      | src/test/resources/hellocucumber/fichier/T_cpte_00737_691594V_du_22-05-2025_au_19-08-2025.csv |
      | src/test/resources/hellocucumber/fichier/T_cpte_01047_058113K_du_22-05-2025_au_19-08-2025.csv |
      | src/test/resources/hellocucumber/fichier/T_cpte_01049_911662L_du_22-05-2025_au_19-08-2025.csv |
    And  Données disponibles dans la base de Données
      |id |comment           |title                    |type |bank_id|
      | 1 |cpt courent       |CCHQ (X1797)             |depot| 2     |
      | 2 |cpt courent       |Compte de dépôts (X1594V)|depot| 3     |
      | 3 |cpt courent femme |compte de depot (X8113K) |depot| 4     |
      | 4 |cpt pro me lcl    |Compte Courant (x1662L)  |depot| 5     |



  Scenario: agregation des données d'une transaction issue d'un fichier conforme et qui n'existe pas en base de données
    Given  le repertoire "src/test/resources/hellocucumber/fichier" auquel j'ai accès
    And dont la structure a été correctement identifiée et les numéros de "<compte>"  et l'id  "<bank>" de la banque ont été correctement recupérés
    When je lance l'agregation des données des transactions du fichier "fichier" dans la table budget
    Then la table budget contiendra les données de la nouvelle transaction
      | id | dateOperation | libelle                                | categorie                                 | montant | notes | chequeNumero | compte | bank |
      | 1  | 12/08/2025    | CHEQUE EMIS 8186609                    | CHEQUE EMIS 8186609                       | -150    |       |              | 1      | 2    |
      | 2  | 04/08/2025    | REGLEMENT ASSU. CNP PRET HABITAT 08/25 | REGLEMENT ASSU. CNP PRET HABITAT 08/25    | 59,62   |       |              | 1      | 2    |
      | 3  | 04/08/2025    | COTISATION Offre Compte à composer     | COTISATION Offre Compte à composer        | -4,68   |       |              | 1      | 2    |
    And le programme lui retournera un message de confirmation de copie de la forme "la copie s'est parfaitement deroulée"

  Scenario: agregation  annulée des données d'une transaction redondante issue d'un fichier conforme
    Given  une transaction redondante d'un fichier "fichier" du repertoire au cours d'un processus d'agregation de données
    When le programme me retourne un message de la forme "veux-tu annulée l'enregistrment en cours remarque aucune données ne sera ajouté en BD"
    And je valide l'annulation de la transaction
    Then la copie s'arrête
    And la table budget ne contient pas de nouvelle valeur:
      | id | dateOperation | libelle                            | categorie                          | montant | notes | chequeNumero | compte | bank |
      | 1  | 12/08/2025    | CHEQUE EMIS 8186609                | CHEQUE EMIS 8186609                | -150    |       |              | 1      | 2    |
      | 2  | 04/08/2025    | COTISATION Offre Compte à composer | COTISATION Offre Compte à composer | -4,68   |       |              | 1      | 2    |

  Scenario: agregation  des données d'une transaction déjà existante en BD issue d'un fichier conforme avec redonce
    Given  une transaction redondante d'un fichier "fichier" du repertoire au cours d'un processus d'agregation de données
    And un message de la forme "transaction déjà existante, veux-tu quand même enregistrer les données?"
    When je decide de continuer
    Then le programme continu et la table budget contiendra une fois de plus les données de la nouvelle transaction
      | id | dateOperation | libelle                            | categorie                         | montant | notes | chequeNumero | compte | bank |
      | 1  | 12/08/2025    | CHEQUE EMIS 8186609                | CHEQUE EMIS 8186609               | -150    |       |              | 1      | 2    |
      | 2  | 04/08/2025    | COTISATION Offre Compte à composer | COTISATION Offre Compte à composer| -4,68   |       |              | 1      | 2    |
      | 3  | 12/08/2025    | CHEQUE EMIS 8186609                | CHEQUE EMIS 8186609               | -150    |       |              | 1      | 2    |
    And le programme lui retournera un message de la forme "copie des données redondantes terminée avec succès"

  Scenario: agregation des données d'une transaction déjà existante en BD issue d'un fichier conforme sans rédonce
  Given
    Given  une transaction redondante d'un fichier "fichier" du repertoire au cours d'un processus d'agregation de données
    And un message de la forme " transaction déjà existante, veux-tu poursuivre l'agregation sans enregistrer ses données?"
    When je decide de continuer
    Then le programme continu et la table budget ne contiendra que les données qui n'existaient pas encore en BD:
      | id | dateOperation | libelle                            | categorie                          | montant | notes | chequeNumero | compte | bank |
      | 1  | 12/08/2025    | CHEQUE EMIS 8186609                | CHEQUE EMIS 8186609                | -150    |       |              | 1      | 2    |
      | 2  | 04/08/2025    | COTISATION Offre Compte à composer | COTISATION Offre Compte à composer | -4,68   |       |              | 1      | 2    |
      | 3  | 01/09/2025    | VIREMENT SALAIRE                   | REVENU SALAIRE                     | +300000 |       |              | 1      | 2    |
    And le programme lui retournera un message de la forme "copie des données sans redondance terminée avec succès"

