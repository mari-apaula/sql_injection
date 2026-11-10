import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

/**
 * Demo de Inyeccion SQL por concatenacion.
 *
 * Caso de estudio: 2.3 Construccion segura (SSDLC - Fase 3).
 *
 * Este programa muestra dos versiones de la misma busqueda de cursos:
 *   1. VULNERABLE: construye el SQL concatenando la entrada del usuario.
 *   2. SEGURA:     usa PreparedStatement con parametros.
 *
 * La base de datos usada es SQLite embebido (archivo cursos.db),
 * por lo que no se necesita instalar ningun motor de base de datos.
 */
public class CursosDemo {

    public static void main(String[] args) {
        String urlJdbc = "jdbc:sqlite:cursos.db";

        try (Connection cn = DriverManager.getConnection(urlJdbc)) {

            inicializarBD(cn);

            Scanner sc = new Scanner(System.in);
            System.out.println("========================================");
            System.out.println("   Demo de Inyeccion SQL (SSDLC 2.3)");
            System.out.println("========================================");
            System.out.println("1) Buscar curso  [VERSION VULNERABLE]");
            System.out.println("2) Buscar curso  [VERSION SEGURA]");
            System.out.println("3) Listar todos los cursos");
            System.out.println("0) Salir");
            System.out.println("----------------------------------------");

            while (true) {
                System.out.print("\nOpcion: ");
                String linea = sc.nextLine().trim();
                if (linea.equals("0")) {
                    System.out.println("Fin.");
                    break;
                }
                if (linea.equals("3")) {
                    listarTodos(cn);
                    continue;
                }
                if (!linea.equals("1") && !linea.equals("2")) {
                    System.out.println("Opcion invalida.");
                    continue;
                }

                System.out.print("Nombre del curso a buscar: ");
                String nombre = sc.nextLine();

                if (linea.equals("1")) {
                    buscarVulnerable(cn, nombre);
                } else {
                    buscarSegura(cn, nombre);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error de base de datos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /* =============================================================
     *  VERSION VULNERABLE (la de la diapositiva)
     *  ----------------------------------------------------------
     *  Concatena directamente el texto del usuario dentro del SQL.
     *  Cualquier comilla o palabra clave inyectada se ejecuta
     *  como parte de la instruccion.
     * ============================================================= */
    static void buscarVulnerable(Connection cn, String nombre) throws SQLException {
        String sql = "SELECT id, nombre FROM cursos WHERE nombre = '" + nombre + "'";

        System.out.println("\n[SQL ejecutado] " + sql);

        try (Statement st = cn.createStatement();
            ResultSet rs = st.executeQuery(sql)) {
            mostrarCursos(rs);
        }
    }

    /* =============================================================
     *  VERSION SEGURA
     *  ----------------------------------------------------------
     *  Usa PreparedStatement con un parametro (?). El driver envia
     *  la plantilla al motor por un canal y el valor por otro, de
     *  modo que la entrada del usuario se trata siempre como DATO,
     *  jamas como codigo SQL.
     * ============================================================= */
    static void buscarSegura(Connection cn, String nombre) throws SQLException {
        String sql = "SELECT id, nombre FROM cursos WHERE nombre = ?";

        System.out.println("\n[SQL ejecutado] " + sql);
        System.out.println("[Parametro 1 ] " + nombre);

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                mostrarCursos(rs);
            }
        }
    }

    /* =============================================================
     *  Helpers
     * ============================================================= */
    static void mostrarCursos(ResultSet rs) throws SQLException {
        int n = 0;
        System.out.println("---- Resultados ----");
        while (rs.next()) {
            System.out.printf("  #%d  %s%n", rs.getInt("id"), rs.getString("nombre"));
            n++;
        }
        System.out.println("Total devuelto: " + n + " curso(s).");
    }

    static void listarTodos(Connection cn) throws SQLException {
        try (Statement st = cn.createStatement();
            ResultSet rs = st.executeQuery("SELECT id, nombre FROM cursos ORDER BY id")) {
            mostrarCursos(rs);
        }
    }

    static void inicializarBD(Connection cn) throws SQLException {
        try (Statement st = cn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS cursos (" +
                    "  id     INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  nombre TEXT NOT NULL)");
            st.execute("DELETE FROM cursos");
            st.execute("INSERT INTO cursos(nombre) VALUES " +
                    "('Seguridad de Software')," +
                       "('Bases de Datos')," +
                       "('Redes de Computadoras')," +
                       "('Sistemas Operativos')," +
                       "('Programacion Web')");
        }
    }
}
