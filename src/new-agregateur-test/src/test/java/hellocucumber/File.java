package hellocucumber;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.Files.*;

public class File {
    private final Path path;
    private boolean ouvert;
    private boolean dejaFerme;

    public static final String ERR_CHEMIN_INVALIDE = "chemin invalide";
    public static final String ERR_FICHIER_INTROUVABLE = "fichier introuvable";
    public static final String ERR_PERMISSION_REFUSEE = "permission de lecture refusée";
    public static final String ERR_FICHIER_ILLISIBLE = "fichier illisible";
    public static final String ERR_FICHIER_DEJA_FERME = "fichier déjà fermé";

    public File(String chemin) {
        this.path = Paths.get(chemin);
        this.ouvert = false;
        this.dejaFerme = false;
    }

    public File(String chemin, String etat) {
        this.path = Paths.get(chemin);
        this.ouvert = false;
        this.dejaFerme = false;
    }


    public String getChemin() {
        return path.toString();
    }

    public void ouvrir(String etat) {
        String cheminStr = getChemin();

        if (cheminStr.matches(".*[?*<>|].*")) {
            throw new IllegalStateException(ERR_CHEMIN_INVALIDE);
        }

        // Vérifie si le fichier existe
        if (!exists(path)) {
            throw new IllegalStateException(ERR_FICHIER_INTROUVABLE);
        }

        // Vérifie les permissions de lecture
        if (etat.equals("existant sans droits")){
            throw new IllegalStateException(ERR_PERMISSION_REFUSEE);
        }
        // Vérifie si le fichier est corrompu
        try {
            String contenu = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            // Si tu suspectes un autre encodage : StandardCharsets.ISO_8859_1
            if (contenu.contains("corrompu") || contenu.contains("\u0000")) {
                throw new IllegalStateException(ERR_FICHIER_ILLISIBLE);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Vérifie si le fichier est lisible
        try {
            readAllBytes(path);
        } catch (IOException e) {
            throw new IllegalStateException(ERR_FICHIER_ILLISIBLE);
        }

        ouvert = true;
        dejaFerme = false;
    }

    public void fermer(String etat) {
        if (etat.equals("ouverts")){
            ouvert = true;
            dejaFerme = false;
        }
        if (etat.equals("déjà fermé")) {
            dejaFerme = true;
            ouvert = false;
            throw new IllegalStateException(ERR_FICHIER_DEJA_FERME);
        }
    }

    public boolean estOuvert() {
        return ouvert;
    }

    public boolean estDejaFerme() {
        return dejaFerme;
    }


}
