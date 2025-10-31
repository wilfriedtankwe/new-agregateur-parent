package hellocucumber;

import com.dimsoft.agregateur.new_agregateur_prod.beans.Bank;
import com.dimsoft.agregateur.new_agregateur_prod.beans.Budget;
import com.dimsoft.agregateur.new_agregateur_prod.beans.Compte;
import com.dimsoft.agregateur.new_agregateur_prod.factory.BudgetFactory;
import com.dimsoft.agregateur.new_agregateur_prod.models.AggregationResultDto;
import com.dimsoft.agregateur.new_agregateur_prod.models.TransactionCADto;
import com.dimsoft.agregateur.new_agregateur_prod.repositories.BankRepository;
import com.dimsoft.agregateur.new_agregateur_prod.repositories.BudgetRepository;
import com.dimsoft.agregateur.new_agregateur_prod.repositories.CompteRepository;
import com.dimsoft.agregateur.new_agregateur_prod.services.impl.BudgetServiceImportImplement;
import com.dimsoft.agregateur.new_agregateur_prod.utils.ManageDuplicateTransaction;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AggregationStep {

    private final BudgetServiceImportImplement budgetService;
    private final BudgetRepository budgetRepository;
    private final CompteRepository compteRepository;
    private final BankRepository bankRepository;

    public AggregationStep (BudgetServiceImportImplement budgetService, BudgetRepository budgetRepository, CompteRepository compteRepository, BankRepository bankRepository) {
        this.budgetService = budgetService;
        this.budgetRepository = budgetRepository;
        this.compteRepository = compteRepository;
        this.bankRepository = bankRepository;
    }

    private SimpleDateFormat dateFormat;

    // Variables de contexte
    private String repertoire;
    private List<String> fichiersDisponibles;
    private List<TransactionCADto> transactionsCA;
    private AggregationResultDto resultatAgregation;
    private Long compteId;
    private Long bankId;
    private String messageUtilisateur;


    @Before
    public void setUp() {
        //  INITIALISER les listes (c'était ça le problème !)
        fichiersDisponibles = new ArrayList<>();
        transactionsCA = new ArrayList<>();

        dateFormat = new SimpleDateFormat("dd/MM/yyyy");

        // Nettoyer la BD
        budgetRepository.deleteAll();

        // Créer des données de test
        Compte compte = new Compte();
        compte.setTitle("CCHQ (X1797)");
        compte.setComment("cpt courent");
        compte.setType("depot");
        compteRepository.save(compte);

        Bank bank = new Bank();
        bank.setNumero(12345678L);
        bank.setTitle("CA");
        bankRepository.save(bank);

        System.out.println(" Setup terminé");
    }

    @Given("les fichiers suivants sont disponibles dans le répertoire d'entrée")
    public void lesFichiersSontDisponibles(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);

        for (Map<String, String> row : rows) {
            String fichier = row.get("fichier");
            fichiersDisponibles.add(fichier);
        }

        System.out.println(" " + fichiersDisponibles.size() + " fichiers disponibles");
    }

    @Given("Données disponibles dans la base de Données")
    public void donneesDisponiblesDansLaBD(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);

        System.out.println(" Données de compte en BD :");
        for (Map<String, String> row : rows) {
            System.out.println("  - Compte " + row.get("id") + " : " + row.get("title"));
        }

        assertNotNull(rows);
        assertTrue(rows.size() >= 4, "Au moins 4 comptes doivent être disponibles");
    }


    @Given("le repertoire {string} auquel j'ai accès")
    public void leRepertoireAuquelJaiAcces(String repertoire) {
        this.repertoire = repertoire;
        System.out.println(" Répertoire : " + repertoire);
        assertNotNull(repertoire);
    }

    @Given("dont la structure a été correctement identifiée et les numéros de {string}  et l'id  {string} de la banque ont été correctement recupérés")
    public void structureIdentifieeEtNumeros(String compte, String bank) {
        this.compteId = 1L;
        this.bankId = 2L;

        transactionsCA.add(BudgetFactory.creerTransactionCA(
                "12/08/2025", "CHEQUE EMIS 8186609", 150.0, null
        ));
        transactionsCA.add(BudgetFactory.creerTransactionCA(
                "04/08/2025", "REGLEMENT ASSU. CNP PRET HABITAT 08/25", null, 59.62
        ));
        transactionsCA.add(BudgetFactory.creerTransactionCA(
                "04/08/2025", "COTISATION Offre Compte à composer", 4.68, null
        ));

        System.out.println(" " + transactionsCA.size() + " transactions préparées");
        System.out.println(" Compte ID : " + compteId + ", Bank ID : " + bankId);
    }

    @When("je lance l'agregation des données des transactions du fichier {string} dans la table budget")
    public void jeLanceLagregation(String fichier) {
        System.out.println("\n Lancement de l'agrégation...\n");

        resultatAgregation = budgetService.importCATransactions(
                transactionsCA,
                compteId,
                bankId
        );

        assertNotNull(resultatAgregation, "Le résultat ne doit pas être null");
    }

    @Then("la table budget contiendra les données de la nouvelle transaction")
    public void laTableBudgetContiendraLesDonnees(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        List<Budget> budgets = budgetRepository.findAll();

        System.out.println("\n Vérification des données en BD :");
        System.out.println("  - Attendu : " + rows.size() + " transactions");
        System.out.println("  - Trouvé : " + budgets.size() + " transactions");

        assertEquals(rows.size(), budgets.size(),
                "Le nombre de transactions doit correspondre");

        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> expectedRow = rows.get(i);
            Budget actualBudget = budgets.get(i);

            System.out.println("\n  Transaction " + (i+1) + " :");
            System.out.println("    - Libellé : " + actualBudget.getLibelle());
            System.out.println("    - Montant : " + actualBudget.getMontant());

            assertEquals(expectedRow.get("libelle"), actualBudget.getLibelle(),
                    "Libellé incorrect pour la transaction " + (i+1));
        }

        System.out.println("\n Toutes les transactions sont correctes");
    }

    @Then("le programme lui retournera un message de confirmation de copie de la forme {string}")
    public void leProgrammeRetourneraUnMessage(String messageAttendu) {
        assertNotNull(resultatAgregation, "Le résultat doit exister");
        assertTrue(resultatAgregation.isSuccess(), "L'opération doit être un succès");
        assertEquals(messageAttendu, resultatAgregation.getMessage(),
                "Le message ne correspond pas");

        System.out.println("\n Message : " + resultatAgregation.getMessage());
    }

    @Given("une transaction redondante d'un fichier {string} du repertoire au cours d'un processus d'agregation de données")
    public void uneTransactionRedondante(String fichier) {
        this.compteId = 1L;
        this.bankId = 2L;

        Budget existing = new Budget();
        existing.setCompte(new Compte());
        existing.setBank(new Bank());
        existing.setDateOperation(parseDate("12/08/2025"));
        existing.setLibelle("CHEQUE EMIS 8186609");
        existing.setMontant(-150.0);
        existing.getCompte().setId(compteId);
        existing.getBank().setId(bankId);
        budgetRepository.save(existing);

        System.out.println("Transaction existante créée en BD");

        transactionsCA.clear();
        transactionsCA.add(BudgetFactory.creerTransactionCA(
                "12/08/2025", "CHEQUE EMIS 8186609", 150.0, null
        ));

        System.out.println(" Transaction redondante préparée");

        budgetService.setStrategieTest(ManageDuplicateTransaction.ANNULER);

    }

    @When("le programme me retourne un message de la forme {string}")
    public void leProgrammeMeRetourneUnMessageDeLaForme(String message) {
        this.messageUtilisateur = message;
        System.out.println(" Message attendu : " + message);
    }

    @When("je valide l'annulation de la transaction")
    public void jeValideLannulation() {
        System.out.println("\n⚠ SIMULATION : L'utilisateur choisit ANNULER");

        resultatAgregation = budgetService.importCATransactions(
                transactionsCA,
                compteId,
                bankId
        );
    }

    @Then("la copie s'arrête")
    public void laCopieSarrete() {
        assertNotNull(resultatAgregation, "Le résultat doit exister");
        assertFalse(resultatAgregation.isSuccess(), "L'opération doit être annulée");
        System.out.println(" La copie s'est bien arrêtée");
    }

    @Then("la table budget ne contient pas de nouvelle valeur:")
    public void laTableBudgetNeContientPasDeNouvelleValeur(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        List<Budget> budgets = budgetRepository.findAll();

        assertEquals(rows.size(), budgets.size(),
                "Le nombre de transactions doit rester inchangé");

        System.out.println(" Aucune nouvelle transaction ajoutée");
    }

    @Given("un message de la forme {string}")
    public void unMessageDeLaForme(String message) {
        this.messageUtilisateur = message;
        System.out.println(" Message : " + message);
    }

    @When("je decide de continuer")
    public void jeDecideDeContinuer() {
        System.out.println(" L'utilisateur décide de continuer");

        resultatAgregation = budgetService.importCATransactions(
                transactionsCA,
                compteId,
                bankId
        );
    }

    @Then("le programme continu et la table budget contiendra une fois de plus les données de la nouvelle transaction")
    public void leProgrammeContinuEtContiendraDonnees(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        List<Budget> budgets = budgetRepository.findAll();

        System.out.println(" Transactions en BD : " + budgets.size());
        System.out.println(" Attendu : " + rows.size());

        assertEquals(rows.size(), budgets.size(),
                "Le nombre de transactions doit inclure les doublons");

        System.out.println(" Doublons bien enregistrés");
    }

    @Then("le programme lui retournera un message de la forme {string}")
    public void leProgrammeRetourneraMessageDeLaForme(String messageAttendu) {
        if (resultatAgregation != null) {
            System.out.println(" Message reçu : " + resultatAgregation.getMessage());
            assertTrue(
                    resultatAgregation.getMessage().contains("redondante") ||
                            resultatAgregation.getMessage().contains("succès"),
                    "Le message doit indiquer un succès"
            );
        }
    }

    @Then("le programme continu et la table budget ne contiendra que les données qui n'existaient pas encore en BD:")
    public void leProgrammeContinuSansDoublons(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        List<Budget> budgets = budgetRepository.findAll();

        System.out.println(" Transactions uniques en BD : " + budgets.size());

        assertEquals(rows.size(), budgets.size(),
                "Seules les nouvelles transactions doivent être en BD");

        System.out.println(" Doublons bien ignorés");
    }

    private Date parseDate(String dateStr) {
        try {
            return dateFormat.parse(dateStr);
        } catch (Exception e) {
            throw new RuntimeException("Erreur de parsing de date: " + dateStr, e);
        }
    }
}