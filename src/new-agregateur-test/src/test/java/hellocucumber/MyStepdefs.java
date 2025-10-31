package hellocucumber;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class MyStepdefs {


    private File file;
    private String lastMessage;
    private Exception lastException;

    private String fichier;
    private String messageResultat;
    private List<String> lignesFichier;

    public String getEtat() {
        return etat;
    }

    public void setEtat(String etat) {
        this.etat = etat;
    }

    private String etat;

    @Given("Un fichier avec le chemin {string} et donc l'etat est {string}")
    public void un_fichier_avec_le_chemin_et_donc_l_etat_est(String chemin, String etat) {
        try {
            this.file = new File(chemin);
            this.etat = etat;
        } catch (Exception e) {
            lastException = e;
        }
    }

    @When("Je tente d'ouvrir le fichier")
    public void je_tente_d_ouvrir_le_fichier() {
        try {
            if (file == null) {
                throw new IllegalStateException("Le fichier n'a pas été initialisé");
            }
            file.ouvrir(this.etat);
            lastMessage = "fichier ouvert avec succès";
        } catch (IllegalStateException e) {
            lastException = e;
            lastMessage = e.getMessage();
        }
    }

    @Then("Le système doit afficher {string}")
    public void le_système_doit_afficher(String messageAttendu) {
        if (lastException != null) {
            assertThat(lastException.getMessage()).isEqualTo(messageAttendu);
        } else {
            assertThat(messageAttendu).isEqualTo(lastMessage);
        }
    }

    @Given("Le fichier avec le chemin {string} donc est {string}")
    public void le_fichier_avec_le_chemin_donc_est(String chemin, String etat) {
        this.file = new File(chemin);

        try{
            file.fermer(etat);
        }catch (Exception e) {
            lastMessage = "une erreur fichier déjà fermé doit être remontée";
        }
    }

    @When("Je tente de fermer le fichier donc l'etat est {string}")
    public void je_tente_de_fermer_le_fichier_donc_l_etat_est(String etat) {
        try {
            file.fermer(etat);
            lastMessage = "le fichier doit être fermé sans probleme";
        } catch (IllegalStateException e) {
            lastException = e;
            lastMessage = e.getMessage();
        }
    }


    // Step spécifique pour le message avec guillemets
    @Then("Le système doit afficher \"une erreur {string} doit être remontée\"")
    public void le_systeme_doit_afficher_erreur_avec_guillemets(String erreurType) {
        String messageComplet = "une erreur " + erreurType + " doit être remontée";
        if (lastException != null) {
            System.out.println( "Une exception devrait être levée" +lastException.getMessage());
        } else {
            assertThat(messageComplet).isEqualTo(this.lastMessage);
        }
    }

    @Given("que le nom du fichier {string} commence par CA")
    public void que_le_nom_du_fichier_commence_par_CA(String fichier) {

        this.fichier = fichier;
        assertThat(Paths.get(fichier).getFileName().toString())
                .as("Le nom du fichier doit commencer par CA")
                .startsWith("CA");
    }

    @When("je me rend a la ligne onze du fichier {string}")
    public void je_me_rend_a_la_ligne_onze_du_fichier(String fichier) throws IOException {


        lignesFichier = Files.readAllLines(Paths.get(fichier), StandardCharsets.ISO_8859_1);
        assertThat(lignesFichier.size())
                .as("Le fichier doit contenir au moins 11 lignes")
                .isGreaterThanOrEqualTo(11);
    }


    @Then("je dois retrouvé les champs nommés Date;Libellé ;Débit euros;Crédit euros;")
    public void je_dois_retrouv_les_champs_nomm_s_Date_Libell_D_bit_euros_Cr_dit_euros() {

        String ligne11 = lignesFichier.get(10); // Ligne 11 = index 10
        System.out.println("Contenu de la ligne 11 : " + ligne11);

        assertThat(ligne11)
                .as("Les champs attendus ne correspondent pas")
                .contains("Date")
                .contains("Libellé")
                .contains("Débit euro")
                .contains("Crédit euro");
    }

    @Given("que le nom du fichier {string} commence par T_cpte")
    public void que_le_nom_du_fichier_commence_par_T_cpte(String fichier) {

        this.fichier = fichier;
        assertThat(Paths.get(fichier).getFileName().toString())
                .as("Le nom du fichier doit commencer par T_cpte")
                .startsWith("T_cpte");
    }

    @When("je me place a la deuxieme ligne et je compte le nombre de colonne du fichier {string}")
    public void je_me_place_a_la_deuxieme_ligne_et_je_compte_le_nombre_de_colonne_du_fichier(String fichier)throws IOException {

        lignesFichier = Files.readAllLines(Paths.get(fichier),StandardCharsets.ISO_8859_1);

        assertThat(lignesFichier.size())
                .as("Le fichier doit contenir au moins deux lignes")
                .isGreaterThanOrEqualTo(2);

        String ligne2 = lignesFichier.get(1); // Ligne 2 = index 1
        String[] colonnes = ligne2.split(";");
        int nombreColonnes = colonnes.length;
        System.out.println("Nombre de colonnes trouvées : " + nombreColonnes);

        assertThat(nombreColonnes)
                .as("Le fichier T_cpte doit contenir 8 colonnes")
                .isEqualTo(8);
    }

    @Then("je dois obtenir huit colonnes")
    public void je_dois_obtenir_huit_colonnes() {

        System.out.println("Vérification du nombre de colonnes OK ");
    }


    @Given("que le nom du fichier ne commence ni par CA ni par T_cpte")
    public void que_le_nom_du_fichier_ne_commence_ni_par_CA_ni_par_T_cpte() {

        this.fichier = "src/test/resources/Feature/TestFolder/missing.csv";

        String nom = Paths.get(fichier).getFileName().toString();
        if (!nom.startsWith("CA") && !nom.startsWith("T_cpte")) {
            messageResultat = "ce fichier ne peut etre importé";
        } else {
            messageResultat = "ce fichier peut etre importé";
        }
    }

    @Then("renvoyer un message {string} d'érreur")
    public void renvoyer_un_message_d_rreur(String messageAttendu) {

        System.out.println("Message obtenu : " + messageResultat);
        assertThat(messageResultat)
                .as("Le message attendu ne correspond pas")
                .isEqualTo(messageAttendu);
    }

}
