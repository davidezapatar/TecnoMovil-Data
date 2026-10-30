# TecnoMovil Data – Procesamiento funcional de datos de transporte

Módulo en **Java puro (sin frameworks)** que procesa los registros diarios del sistema de transporte urbano de la ciudad de TecnoValle usando **programación funcional**: funciones puras, inmutabilidad, lambdas, funciones de orden superior y Streams (también en paralelo con `parallelStream()`).

**Actividad:** EA2. Creación de repositorio – Programación Orientada a Objetos II
**Institución:** Institución Universitaria Digital de Antioquia
**Autor:** David Esteban Zapata Rodríguez – Grupo 10
**Docente:** Jorge Armando Julio

---

## ¿Qué resuelve?

| Tarea del caso de estudio | Método en `ProcesadorTransporte` |
|---|---|
| a) Afluencia por estación | `afluenciaPorEstacion`, `usuariosUnicosPorEstacion` |
| b) Horas pico | `flujoPorHora`, `horasPico` |
| c) Rutas más utilizadas | `usosPorRuta` + `topN` |
| d) Patrones de viaje por usuario | `patronesPorUsuario` |
| e) Tiempo promedio entre estaciones consecutivas | `tiemposEntreEstaciones` |
| f) Detección de sobrecarga en rutas | `estadoDeRutas` |

Los datos se simulan con `GeneradorDatos`: por defecto 150.000 usuarios y unos 510.000 registros de un día.

## Cómo ejecutarlo

Requisito: **JDK 17 o superior** (se probó con Java 21).

**Windows:** doble clic en `ejecutar.bat`, o en la consola:

```
ejecutar.bat
ejecutar.bat 300000
```

**Linux / Mac:**

```
./ejecutar.sh
```

**A mano (cualquier sistema):**

```
javac -encoding UTF-8 -d out src/tecnomovil/*.java src/tecnomovil/*/*.java
java -cp out tecnomovil.Verificacion
java -cp out tecnomovil.App
```

**IntelliJ IDEA:** abrir la carpeta del proyecto, marcar `src` como *Sources Root* y ejecutar `App` o `Verificacion`.

## Estructura

```
src/tecnomovil/
├── App.java                     Programa principal (único lugar que imprime)
├── Verificacion.java            16 pruebas sin frameworks
├── modelo/                      Records inmutables
│   ├── Accion.java              ENTRADA / SALIDA
│   ├── Registro.java            idUsuario, ruta, estacion, accion, timestamp
│   ├── Tramo.java               origen -> destino
│   ├── EstadoRuta.java          resultado de la tarea f
│   └── ResultadoTiempos.java    resultado de la tarea e
├── datos/
│   └── GeneradorDatos.java      Datos simulados, generados con funciones puras
├── procesamiento/
│   ├── Filtros.java             Predicados y funciones que se combinan
│   └── ProcesadorTransporte.java Tareas a) a f)
└── reportes/
    └── Reporte.java             Convierte resultados en texto (funciones puras)
docs/                            Informe en PDF
```

## Conceptos funcionales usados

- **Funciones puras:** todos los métodos de `ProcesadorTransporte`, `Filtros`, `Reporte` y `GeneradorDatos` devuelven un resultado que depende solo de sus parámetros, sin cambiar nada afuera.
- **Inmutabilidad:** `record`, `List.of`, `Stream.toList()`, `Collectors.toUnmodifiableList()`, `Collections.unmodifiableMap`, `Map.copyOf`.
- **Lambdas y referencias a métodos:** `r -> r.accion() == Accion.ENTRADA`, `Registro::estacion`.
- **Funciones de orden superior:** `entreHoras(6, 9)`, `deRuta(...)` y `superaUmbral(0.85)` devuelven funciones; `topN` y `tablaConBarras` reciben funciones.
- **Composición:** `ES_ENTRADA.and(entreHoras(6, 9))`, `ES_ENTRADA.negate()`, `Function.andThen`, `Comparator.thenComparing`.
- **Streams:** `filter`, `map`, `flatMap`, `sorted`, `limit`, `groupingBy`, `counting`, `mapping`, `averagingDouble`, `collectingAndThen`.
- **Paralelismo:** cada tarea recibe `paralelo = true/false`; el programa comprueba que los dos modos den el mismo resultado.
