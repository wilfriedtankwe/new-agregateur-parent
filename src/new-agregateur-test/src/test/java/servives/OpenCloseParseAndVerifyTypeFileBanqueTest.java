package servives;


import com.dimsoft.agregateur.new_agregateur_prod.components.DetecteurTypeFichierBancaire;
import com.dimsoft.agregateur.new_agregateur_prod.components.ParseurCAFile;
import com.dimsoft.agregateur.new_agregateur_prod.components.ParseurLLCFile;
import com.dimsoft.agregateur.new_agregateur_prod.enums.TypeFichierBancaire;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests TDD pour la détection et gestion des fichiers bancaires CA et LCL
 */
public class OpenCloseParseAndVerifyTypeFileBanqueTest {

    private static final String DOSSIER_TEST = "src/test/resources/hellocucumber/fichier";
    private static final String FICHIER_CA = DOSSIER_TEST + "/CA20250820_115728.csv";
    private static final String FICHIER_LCL = DOSSIER_TEST + "/T_cpte_00737_691594V_du_22-05-2025_au_19-08-2025.csv";
    private static final String FICHIER_INVALIDE = DOSSIER_TEST + "/LCL2.csv";
    private static final String FICHIER_MANQUANT = DOSSIER_TEST + "/missing.csv";

    @Before
    public void setup() {
        // Créer les fichiers de test s'ils n'existent pas
        creerFichiersTest();
    }

    private void creerFichiersTest() {
        try {
            Files.createDirectories(Paths.get(DOSSIER_TEST));

            // Créer fichier CA si absent
            if (!Files.exists(Paths.get(FICHIER_CA))) {
                String contenuCA = "Téléchargement du 20/08/2025;\n" +
                        "\n" +
                        "M. DIMO NOULA SEVERIN\n" +
                        "Compte de Dépôt carte n° 04109661797;\n" +
                        "Solde au 20/08/2025 -1 017,87 €\n" +
                        "\n" +
                        "Liste des opérations du compte;\n" +
                        "\n" +
                        "\n" +
                        "\n" +
                        "Date;Libellé;Débit euros;Crédit euros;\n" +
                        "12/05/2025;vente;24;-24;\n" +
                        "13/05/2025;achat;;100;\n";
                Files.write(Paths.get(FICHIER_CA), contenuCA.getBytes(StandardCharsets.UTF_8));
            }

            // Créer fichier LCL si absent
            if (!Files.exists(Paths.get(FICHIER_LCL))) {
                String contenuLCL = "22/05/2025;1250;cheque;Google playApp;0;divers;ref1;supp1\n" +
                        "23/05/2025;-6,99;carte;CB Google Play;0;divers;ref2;supp2\n" +
                        "24/05/2025;-10,89;carte;CB Intergleize;0;divers;ref3;supp3\n";
                Files.write(Paths.get(FICHIER_LCL), contenuLCL.getBytes(StandardCharsets.UTF_8));
            }

        } catch (IOException e) {
            throw new RuntimeException("Erreur création fichiers test", e);
        }
    }

    // ========== TESTS DÉTECTION DE TYPE ==========

    @Test
    public void test_detecter_fichier_CA() {
        TypeFichierBancaire type =
                DetecteurTypeFichierBancaire.detecterType("CA20250820_115728.csv");

        assertThat(type).isEqualTo(TypeFichierBancaire.CA);
    }

    @Test
    public void test_detecter_fichier_CA_minuscules() {
        TypeFichierBancaire type = DetecteurTypeFichierBancaire.detecterType("ca20250820_115728.csv");

        System.out.println("Test CA minuscules : " + type);
        assertThat(type).isEqualTo(TypeFichierBancaire.CA);
    }

    @Test
    public void test_detecter_fichier_LCL() {
        TypeFichierBancaire type =
                DetecteurTypeFichierBancaire.detecterType("T_cpte_00737_691594V_du_22-05-2025_au_19-08-2025.csv");

        assertThat(type).isEqualTo(TypeFichierBancaire.LCL);
    }


    @Test
    public void test_detecter_fichier_LCL_majuscules() {
        TypeFichierBancaire type = DetecteurTypeFichierBancaire.detecterType("T_CPTE_00737_691594V.CSV");

        System.out.println("Test LCL majuscules : " + type);
        assertThat(type).isEqualTo(TypeFichierBancaire.LCL);
    }

    @Test
    public void test_detecter_fichier_inconnu() {
        TypeFichierBancaire type =
                DetecteurTypeFichierBancaire.detecterType("Unknown_file.csv");

        assertThat(type).isEqualTo(TypeFichierBancaire.INCONNU);
    }


    @Test
    public void test_estFichierCA() {
        boolean resultat = DetecteurTypeFichierBancaire.estFichierCA("CA20250820_115728.csv");
        assertThat(resultat).isTrue();
    }

    @Test
    public void test_estFichierLCL() {
        boolean resultat = DetecteurTypeFichierBancaire.estFichierLCL("T_cpte_00737_691594V_du_22-05-2025_au_19-08-2025.csv");
        assertThat(resultat).isTrue();
    }

    @Test
    public void test_estFichierValide_CA() {
        boolean resultat = DetecteurTypeFichierBancaire.estFichierValide("CA20250820_115728.csv");
        assertThat(resultat).isTrue();
    }

    @Test
    public void test_estFichierValide_LCL() {
        boolean resultat = DetecteurTypeFichierBancaire.estFichierValide("T_cpte_00737_691594V.csv");
        assertThat(resultat).isTrue();
    }

    @Test
    public void test_estFichierValide_Invalide() {
        boolean resultat = DetecteurTypeFichierBancaire.estFichierValide("invalid_file.csv");
        assertThat(resultat).isFalse();
    }

    // ========== TESTS PARSEUR CA ==========

    @Test
    public void test_parseur_CA_ouvre_fichier() {
        ParseurCAFile parseur = new ParseurCAFile();
        List<Map<String, String>> donnees = parseur.parserFichier(FICHIER_CA);

        assertThat(donnees).isNotEmpty();
    }

    @Test
    public void test_parseur_CA_verifie_structure() {
        ParseurCAFile parseur = new ParseurCAFile();
        boolean valide = parseur.verifierStructure(FICHIER_CA);

        assertThat(valide).isTrue();
    }

    @Test
    public void test_parseur_CA_premiere_ligne_contient_date() {
        ParseurCAFile parseur = new ParseurCAFile();
        List<Map<String, String>> donnees = parseur.parserFichier(FICHIER_CA);

        assertThat(donnees.get(0)).containsKey("Date");
    }

    @Test
    public void test_parseur_CA_premiere_ligne_contient_montants() {
        ParseurCAFile parseur = new ParseurCAFile();
        List<Map<String, String>> donnees = parseur.parserFichier(FICHIER_CA);
        Map<String, String> premiereLigne = donnees.get(0);

        boolean hasMontant = premiereLigne.containsKey("Débit euros") ||
                premiereLigne.containsKey("Crédit euros");
        assertThat(hasMontant).isTrue();
    }

    @Test
    public void test_parseur_CA_extrait_numero_compte() {
        ParseurCAFile parseur = new ParseurCAFile();
        parseur.parserFichier(FICHIER_CA);

        String numeroCompte = parseur.getNumeroCompte();
        assertThat(numeroCompte).isNotNull();
        assertThat(numeroCompte).matches("\\d+");
    }

    // ========== TESTS PARSEUR LCL ==========

    @Test
    public void test_parseur_LCL_ouvre_fichier() {
        ParseurLLCFile parseur = new ParseurLLCFile();
        List<Map<String, String>> donnees = parseur.parserFichier(FICHIER_LCL);

        assertThat(donnees).isNotEmpty();
    }

    @Test
    public void test_parseur_LCL_verifie_structure() {
        ParseurLLCFile parseur = new ParseurLLCFile();
        boolean valide = parseur.verifierStructure(FICHIER_LCL);

        assertThat(valide).isTrue();
    }

    @Test
    public void test_parseur_LCL_premiere_transaction_ligne_2() {
        ParseurLLCFile parseur = new ParseurLLCFile();
        List<Map<String, String>> donnees = parseur.parserFichier(FICHIER_LCL);

        // La première ligne de donnée doit avoir une date
        assertThat(donnees.get(0)).containsKey("Date");
        assertThat(donnees.get(0).get("Date")).matches("\\d{2}/\\d{2}/\\d{4}");
    }

    @Test
    public void test_parseur_LCL_huit_colonnes() {
        ParseurLLCFile parseur = new ParseurLLCFile();
        List<Map<String, String>> donnees = parseur.parserFichier(FICHIER_LCL);

        assertThat(donnees.get(0)).hasSize(8);
    }


    // ========== TESTS OUVERTURE FICHIERS ==========

    @Test
    public void test_ouverture_fichier_CA_existant() {
        ParseurCAFile parseur = new ParseurCAFile();
        List<Map<String, String>> donnees = parseur.parserFichier(FICHIER_CA);

        assertThat(donnees).isNotEmpty();
    }

    @Test
    public void test_ouverture_fichier_LCL_existant() {
        ParseurLLCFile parseur = new ParseurLLCFile();
        List<Map<String, String>> donnees = parseur.parserFichier(FICHIER_LCL);

        assertThat(donnees).isNotEmpty();
    }

    @Test
    public void test_ouverture_fichier_manquant_CA() {
        ParseurCAFile parseur = new ParseurCAFile();

        assertThatThrownBy(() -> parseur.parserFichier(FICHIER_MANQUANT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("n'existe pas");
    }

    @Test
    public void test_ouverture_fichier_manquant_LCL() {
        ParseurLLCFile parseur = new ParseurLLCFile();

        assertThatThrownBy(() -> parseur.parserFichier(FICHIER_MANQUANT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("n'existe pas");
    }

    // ========== TESTS CHEMIN INVALIDE ==========

    @Test
    public void test_chemin_invalide() {
        String cheminInvalide = "!@!*&?*?invalid.csv";
        TypeFichierBancaire type =
                DetecteurTypeFichierBancaire.detecterType(cheminInvalide);

        assertThat(type).isEqualTo(TypeFichierBancaire.INCONNU);
    }

    // ========== TESTS FICHIER CORROMPU ==========

    @Test
    public void test_fichier_corrompu_detection() {
        String fichierCorrompu = "corrompu_file.csv";
        TypeFichierBancaire type =
                DetecteurTypeFichierBancaire.detecterType(fichierCorrompu);

        assertThat(type).isEqualTo(TypeFichierBancaire.INCONNU);
    }

    // ========== TESTS FICHIER INCONNU ==========

    @Test
    public void test_fichier_inconnu_message_erreur() {
        String fichierInconnu = "unknown_bank.csv";
        TypeFichierBancaire type =
                DetecteurTypeFichierBancaire.detecterType(fichierInconnu);

        assertThat(type).isEqualTo(TypeFichierBancaire.INCONNU);
    }

    // ========== TESTS FERMETURE FICHIER ==========

    @Test
    public void test_gestion_etats_fichier() {
        ParseurCAFile parseur = new ParseurCAFile();

        // Ouverture
        List<Map<String, String>> donnees = parseur.parserFichier(FICHIER_CA);
        assertThat(donnees).isNotEmpty();

        // Après traitement, on considère le fichier comme traité
        // (pas d'erreur = traitement réussi)
    }

    // ========== TESTS UTILITAIRES ==========

    @Test
    public void test_afficher_exemple_CA() {
        ParseurCAFile parseur = new ParseurCAFile();
        List<Map<String, String>> donnees = parseur.parserFichier(FICHIER_CA);

        System.out.println("\n=== EXEMPLE FICHIER CA ===");
        System.out.println("Numéro compte : " + parseur.getNumeroCompte());
        System.out.println("Nombre de transactions : " + donnees.size());
        if (!donnees.isEmpty()) {
            System.out.println("Première transaction : " + donnees.get(0));
        }
    }

    @Test
    public void test_afficher_exemple_LCL() {
        ParseurLLCFile parseur = new ParseurLLCFile();
        List<Map<String, String>> donnees = parseur.parserFichier(FICHIER_LCL);

        System.out.println("\n=== EXEMPLE FICHIER LCL ===");
        System.out.println("Numéro compte : " + parseur.getNumeroCompte());
        System.out.println("Nombre de transactions : " + donnees.size());
        if (!donnees.isEmpty()) {
            System.out.println("Première transaction : " + donnees.get(0));
        }
    }
}

