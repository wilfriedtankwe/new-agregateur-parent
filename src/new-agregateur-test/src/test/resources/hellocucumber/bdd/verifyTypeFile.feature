Feature: agregation des données dans la table budget
  l'utilisateur veut pouvoir centraliser l'ensemble de ses transactions dans une table budget en BD
  pour ce faire il doit acceder à 4 fichiers dont un de type CA et 3 autres de type LCL et copier ligne
  par ligne les données qui s'y trouves en respectant les correspondances de champs.

  Scenario Outline: vérification de la structure d'un fichier
    Given que le nom du fichier "<fichier>" commence par CA
    When je me rend a la ligne onze du fichier "<fichier>"
    Then je dois retrouvé les champs nommés Date;Libellé ;Débit euros;Crédit euros;

    Examples:
      | fichier                                                         | Date       | Libellé | Débit euros | Crédit euros |
      | src/test/resources/hellocucumber/fichier/CA20250820_115728.csv  | 12/05/2025 | vente   |          24 |          -24  |


  Scenario Outline: vérification de la structure d'un fichier
    Given que le nom du fichier "<fichier>" commence par T_cpte
    When je me place a la deuxieme ligne et je compte le nombre de colonne du fichier "<fichier>"
    Then je dois obtenir huit colonnes

    Examples:
      | fichier                                                                                              | date       | montant | mode de paiement |  | libellé        |  | zero | divers |
      | src/test/resources/hellocucumber/fichier/T_cpte_00737_691594V_du_22-05-2025_au_19-08-2025.csv | 12/08/2025 |    1250 | cheque           |  | Google playApp |  |    0 | divers |

  Scenario Outline: vérification de la structure d'un fichier quelconque
    Given que le nom du fichier ne commence ni par CA ni par T_cpte
    Then renvoyer un message "<message>" d'érreur

    Examples:
      | fichier                                            | message                         |
      | src/test/resources/Feature/TestFolder/misssing.csv | ce fichier ne peut etre importé |

