# Demo de Inyección SQL — SSDLC 2.3 Construcción segura

Proyecto Java mínimo que reproduce la vulnerabilidad de la diapositiva y la corrige con `PreparedStatement`. Usa SQLite embebido (archivo `cursos.db` que se crea solo), así que **no necesitas instalar ningún motor de base de datos**.

## Requisitos

- JDK 8 o superior instalado (`java -version` y `javac -version` deben funcionar).
- El archivo `sqlite-jdbc-3.46.1.3.jar` incluido en este proyecto.

## Cómo compilar

Abre una terminal dentro de la carpeta `sql-injection-demo/` y ejecuta:

```bash
javac -cp sqlite-jdbc-3.46.1.3.jar CursosDemo.java
```

Esto genera el archivo `CursosDemo.class`.

## Cómo ejecutar

**En Linux / macOS:**

```bash
java -cp .:sqlite-jdbc-3.46.1.3.jar CursosDemo
```

**En Windows (CMD o PowerShell):** usa `;` en lugar de `:`

```cmd
java -cp .;sqlite-jdbc-3.46.1.3.jar CursosDemo
```

El programa crea una pequeña base de datos `cursos.db` con 5 cursos de ejemplo y muestra un menú.

## Las tres pruebas que debes documentar

### Prueba 1 — Búsqueda normal (versión vulnerable)

```
Opcion: 1
Nombre del curso a buscar: Redes de Computadoras

[SQL ejecutado] SELECT id, nombre FROM cursos WHERE nombre = 'Redes de Computadoras'
---- Resultados ----
  #3  Redes de Computadoras
Total devuelto: 1 curso(s).
```

Comportamiento esperado: devuelve **1** curso. Todo bien.

### Prueba 2 — Inyección SQL (versión vulnerable)

```
Opcion: 1
Nombre del curso a buscar: ' OR '1'='1

[SQL ejecutado] SELECT id, nombre FROM cursos WHERE nombre = '' OR '1'='1'
---- Resultados ----
  #1  Seguridad de Software
  #2  Bases de Datos 
  #3  Redes de Computadoras
  #4  Sistemas Operativos
  #5  Programacion Web
Total devuelto: 5 curso(s).
```

**Evidencia del ataque:** devuelve **todos** los cursos aunque no se buscaron. El filtro `WHERE` quedó anulado porque `'1'='1'` es siempre verdadero.

### Prueba 3 — Mismo ataque, versión segura

```
Opcion: 2
Nombre del curso a buscar: ' OR '1'='1

[SQL ejecutado] SELECT id, nombre FROM cursos WHERE nombre = ?
[Parametro 1 ] ' OR '1'='1
---- Resultados ----
Total devuelto: 0 curso(s).
```

**Resultado:** cero cursos. El `PreparedStatement` buscó literalmente un curso llamado `' OR '1'='1`, que no existe. El ataque quedó neutralizado.

## ¿Por qué funciona la versión segura?

Con `PreparedStatement`, el driver JDBC envía al motor SQLite dos cosas por canales separados:

1. La **plantilla** del SQL (`SELECT ... WHERE nombre = ?`). El motor la compila y la estructura queda fija.
2. El **valor** del parámetro, por otro canal, ya con la estructura cerrada.

Por eso cualquier comilla, `OR`, `--` o `;` que venga en la entrada no puede cambiar la consulta: ya está compilada. Se garantiza la separación entre **código** y **dato**.

## Más payloads para probar (versión vulnerable)

Prueba cada uno con la **opción 1** para ver otras formas de inyección:

| Entrada | Qué hace |
|---|---|
| `' OR '1'='1` | Devuelve todos los registros |
| `' OR 1=1 --` | Comenta el resto, mismo efecto |
| `' UNION SELECT 999, 'HACKEADO' --` | Combina con resultados inventados |
| `'; DROP TABLE cursos; --` | Intentaría borrar la tabla (depende de permisos) |

Repítelos con la **opción 2** y verás que ninguno funciona.

## Relación con el SSDLC

Esto corresponde a la **Fase 3 — Construcción segura** del ciclo de vida de desarrollo seguro. Herramientas que detectan esta vulnerabilidad automáticamente:

- **SAST:** SonarQube, Semgrep, SpotBugs con FindSecBugs — marcan la concatenación de strings en SQL.
- **DAST:** OWASP ZAP, sqlmap — prueban payloads contra la app en ejecución.
- **Revisión de código:** OWASP Code Review Guide.

Referencias:
- OWASP Top 10 — A03:2021 Injection.
- CWE-89: Improper Neutralization of Special Elements used in an SQL Command.
- OWASP SQL Injection Prevention Cheat Sheet.
