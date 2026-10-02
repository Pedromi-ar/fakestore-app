# FakeStore App (Java 26 · VS Code)

Aplicación de escritorio en **Java Swing** (sin dependencias externas) que consume
[Fake Store API](https://fakestoreapi.com) e implementa las historias US03, US04 y US05.

## Requisitos
- JDK **26.0.1**
- VS Code + extensión **Extension Pack for Java**
- Conexión a internet

## Cómo ejecutar
**Opción A (VS Code):** abre la carpeta, abre `Main.java` y pulsa *Run* (o F5 → "Ejecutar FakeStore App").
Si tu JDK está en otra ruta, ajusta `.vscode/settings.json`.

**Opción B (terminal):** `./run.sh` (Linux/Mac) o `run.bat` (Windows).

**Opción C (Maven):** `mvn compile exec:java`

## Estructura
```
src/main/java/com/tienda/
 ├─ Main.java
 ├─ model/    Product, Rating, Role        (modelo que empata con el JSON)
 ├─ util/     Json                         (parser JSON propio)
 ├─ service/  ProductApi, ApiException     (GET/PUT/DELETE a la API)
 ├─ session/  Session, SessionManager      (sesión local con el rol)
 └─ ui/       LoginFrame, CatalogFrame, ProductCellRenderer,
              ProductDetailDialog, ImageLoader
```

## Cobertura de criterios
| US | Requisito | Implementación |
|----|-----------|----------------|
| 03 | Catálogo con imagen, título y precio | `CatalogFrame` + `ProductCellRenderer` |
| 03 | Estado de carga | Barra indeterminada centrada (`LOADING`) |
| 03 | Error con "Reintentar" | Panel `ERROR` con botón |
| 03 | Lista reciclable | `JList` + renderer reutilizado (no un scroll con todo) |
| 03 | Imágenes asíncronas | `ImageLoader` (pool de hilos + caché) |
| 03 | Mapeo del JSON | `record Product` / `record Rating` |
| 04 | GET `/products/categories` → chips | `loadCategories()` |
| 04 | GET `/products/category/{c}` con carga | `loadProducts(category)` |
| 04 | "Ver todos" → `/products` | Primer chip |
| 04 | Limpiar datos previos | `model.clear()` antes de cada petición |
| 05 | GET `/products/{id}` | `ProductApi.getById` |
| 05 | Cliente/Auditor sin controles de gestión | Botones no se crean |
| 05 | Administrador: Editar y Eliminar | Solo se instancian si `role == ADMINISTRADOR` |
| 05 | Error → "Producto no disponible" | Alerta y se permanece en el catálogo |
| 05 | Rol desde sesión local | `SessionManager` (`~/.fakestore-app/session.properties`) |

## Notas
- El login es simulado (usuario + rol) porque las historias piden leer el rol de la sesión local.
- Fake Store API **no persiste** PUT/DELETE: responde OK pero al recargar verás los datos originales.
- Las categorías con espacios (ej. `men's clothing`) se codifican en la URL automáticamente.
