# Sprint: Integración View – Model – Controller (SimuladorCPU)

**Rama:** `ViewGrupo1`
**Fecha:** 2026-07-14
**Módulo:** `SimuladorCPU`

## 1. Objetivo del sprint

Conectar la capa de vista (`org.example.view`) con la capa de modelo
(`org.example.model`), que hasta este punto existían de forma aislada:
la vista tenía su propio motor de simulación duplicado (`SimulationEngine`
+ `Process`) y el modelo (`Queue`, `Proceso`, `Cpu`, `Metrics`) no era
usado por ninguna pantalla. Se decidió que el **modelo maneje toda la
lógica de negocio** y que la **vista solo pinte**, usando `Controlador`
como intermediario (patrón MVC).

## 2. Contexto / decisión de arquitectura

Se identificaron dos motores de simulación incompatibles:

| | Vista (antes) | Modelo (antes) |
|---|---|---|
| Motor | `SimulationEngine`, basado en *ticks* síncronos disparados por un `javax.swing.Timer` | `Cpu extends Thread`, con hilos reales, `Thread.sleep(500)` y `Semaphore` |
| Entidad de proceso | `view.Process` (con color, memoria) | `model.Proceso` (sin color) |
| Uso real en la UI | Sí, todos los paneles | No, clase `Controlador` vacía |

Se preguntó al usuario cómo resolver la incompatibilidad y se eligió la
opción: **"Model maneja la lógica, view solo pinta"** — eliminar el motor
duplicado de la vista y hacer que `Controlador` orqueste el modelo real
(colas, hilos `Cpu`, métricas), exponiendo únicamente datos de solo
lectura a los paneles Swing.

## 3. Trabajo realizado

### 3.1 Modelo (`org.example.model`)

- **`Cpu.java`**
  - Bug fix: el hilo nunca comprobaba `isInterrupted()` en su bucle
    principal, por lo que `interrupt()` no lo detenía realmente (solo
    rompía el bucle interno y seguía despachando procesos). Se cambió
    la condición a `while (!isInterrupted())`.
  - Al interrumpir un `Cpu` en medio de la ejecución de un proceso
    (pausa/stop), el proceso ya no se pierde: se reencola en Listos
    (`queue.agregarAListos(proceso)`) antes de salir del hilo.
  - Se eliminó el reloj local (`reloj`) por hilo; ahora se apoya en el
    reloj centralizado de `Metrics`, evitando que cada núcleo lleve su
    propio tiempo desincronizado.
  - Se agregó `procesoActual` (con `getProcesoActual()`) y `core`
    (con `getCore()`), necesarios para que la vista sepa qué proceso
    corre en cada núcleo y pueda dibujar el Gantt por core.
  - Se agregó un constructor sobrecargado que recibe el índice de core.

- **`Metrics.java`**
  - `recordGanttEntry` ahora también registra el núcleo de ejecución
    (sobrecarga `recordGanttEntry(Proceso p, int core)`), para poder
    reconstruir el diagrama de Gantt por núcleo en la UI.

- **`Queue.java` / `Proceso.java`**: sin cambios de comportamiento
  (ya eran funcionales); se usaron tal cual desde `Controlador`.

### 3.2 Controlador (`org.example.controller.Controlador`)

Antes: clase vacía. Ahora implementa:

- Creación de procesos (`crearProceso`, `crearProcesoAleatorio`) con
  asignación automática de color (paleta) por PID.
- Ciclo de vida de la simulación: `iniciar()`, `pausar()`, `detener()`,
  `reiniciar()` — crea/destruye los hilos `Cpu` según el número de
  núcleos configurado.
- Reloj compartido (`javax.swing.Timer` de 500ms) que:
  - avanza el tiempo de `Metrics`,
  - admite procesos "Nuevos" cuya llegada ya se cumplió,
  - incrementa el tiempo de espera de los procesos en Listos,
  - registra entradas de Gantt y ciclos ocupados por cada `Cpu` activo.
- Configuración en caliente (`aplicarConfiguracion`): núcleos, quantum,
  algoritmo.
- Bitácora de eventos (`log`/`getEventLog`), con escritura a `log.txt`
  (mismo comportamiento que tenía `SimulationEngine`).
- Getters de solo lectura para la vista: colas (listos/bloqueados/nuevos),
  procesos terminados, todos los procesos, CPUs activas, historial de
  Gantt, utilización de CPU, espera promedio, retorno promedio,
  throughput.

### 3.3 Vista (`org.example.view`)

- **Eliminados** `SimulationEngine.java` y `Process.java` (motor y
  entidad duplicados, ya no se usan).
- **`estructura.java`**: instancia `Controlador` en lugar de
  `SimulationEngine` y lo inyecta a todos los paneles.
- **`TopBarPanel`**: los botones Start/Pause/Stop/Reset llaman a
  `controlador.iniciar()/pausar()/detener()/reiniciar()`. Ya no gestiona
  su propio `Timer` de simulación (el reloj vive en `Controlador`).
- **`ProcessCreatorPanel`**: crea procesos vía
  `controlador.crearProceso(...)`; el pool de procesos se pinta leyendo
  `Proceso` del modelo. Se retiró el campo "memoria" (no existe en el
  modelo de dominio).
- **`SimulationAreaPanel`**: Ready/Running/Blocked(I/O)/Finished ahora
  se alimentan de `Controlador` (`getColaListos`, `getCpus`,
  `getColaBloqueados`, `getProcesosTerminados`). Se agregó
  visualización real de la cola de bloqueados por E/S, que antes
  siempre mostraba "0" (no existía en el motor viejo).
- **`CpuConfigPanel`**: el combo de algoritmos ahora ofrece los que el
  modelo realmente soporta (`FIFO`, `SJF`, `Prioridades`,
  `Round Robin`); se quitó el checkbox "Enable Preemption" porque no
  tiene equivalente en el modelo de hilos actual (dejarlo habría sido
  un control sin efecto).
- **`BottomBarPanel`**: estadísticas, tabla de procesos y diagrama de
  Gantt migrados a leer `Proceso`/`Cpu`/bitácora de `Controlador`. El
  Gantt ahora se dibuja por núcleo real usando la bitácora de
  `Metrics`.
- **`LogPanel`**: lee `controlador.getEventLog()` en vez del log del
  motor eliminado.

## 4. Riesgos / limitaciones conocidas

- El "reloj" de la simulación es un `Timer` de Swing en `Controlador`
  desacoplado del `sleep(500)` interno de cada hilo `Cpu`; con varios
  núcleos puede haber un pequeño desfase visual entre el tiempo
  mostrado y el ciclo real de cada hilo. Es aceptable para fines de
  visualización, no para medición de precisión de tiempo real.
- Pausar la simulación interrumpe los hilos `Cpu`; hay una ventana de
  hasta ~500ms en la que el hilo puede tardar en reaccionar a la
  interrupción antes de reencolar su proceso.
- No se pudo compilar el proyecto en este entorno (no hay JDK instalado,
  solo JRE 1.8; falta `javac`/`mvn`). La validación se hizo por lectura
  exhaustiva de código y diagnósticos del editor. **Pendiente: compilar
  y probar en un entorno con JDK antes de dar el sprint por cerrado.**

## 5. Archivos modificados

```
M  SimuladorCPU/.../controller/Controlador.java     (antes vacío)
?? SimuladorCPU/.../model/Cpu.java                   (nuevo, movido a git add pendiente)
?? SimuladorCPU/.../model/Metrics.java                (nuevo, movido a git add pendiente)
M  SimuladorCPU/.../model/Proceso.java
M  SimuladorCPU/.../model/Queue.java
M  SimuladorCPU/.../view/BottomBarPanel.java
M  SimuladorCPU/.../view/CpuConfigPanel.java
M  SimuladorCPU/.../view/LogPanel.java
D  SimuladorCPU/.../view/Process.java
M  SimuladorCPU/.../view/ProcessCreatorPanel.java
M  SimuladorCPU/.../view/SimulationAreaPanel.java
D  SimuladorCPU/.../view/SimulationEngine.java
M  SimuladorCPU/.../view/TopBarPanel.java
M  SimuladorCPU/.../view/estructura.java
```

---

## 6. Checklist del sprint

### Diseño / decisión
- [x] Detectar la duplicación de motores de simulación entre `view` y `model`
- [x] Presentar opciones de integración al equipo/usuario
- [x] Decidir estrategia: modelo maneja lógica, vista solo pinta

### Modelo
- [x] Corregir bug de `Cpu` que impedía detener el hilo con `interrupt()`
- [x] Reencolar el proceso en curso al pausar/interrumpir un `Cpu`
- [x] Centralizar el reloj de simulación en `Metrics` (quitar reloj local por hilo)
- [x] Exponer proceso en ejecución y núcleo (`getProcesoActual`, `getCore`) en `Cpu`
- [x] Registrar el núcleo en la bitácora de Gantt (`Metrics.recordGanttEntry`)

### Controlador
- [x] Implementar `Controlador` como orquestador de `Queue` + `Metrics` + `Cpu`
- [x] Ciclo de vida: iniciar / pausar / detener / reiniciar
- [x] Reloj compartido (admisión de llegadas, espera acumulada, registro de Gantt)
- [x] Aplicar configuración en caliente (núcleos, quantum, algoritmo)
- [x] Bitácora de eventos + escritura a `log.txt`
- [x] Exponer API de solo lectura para la vista (colas, métricas, procesos)

### Vista
- [x] Eliminar `SimulationEngine` y `Process` duplicados
- [x] `estructura.java` inyecta `Controlador` a todos los paneles
- [x] `TopBarPanel` controla la simulación a través del `Controlador`
- [x] `ProcessCreatorPanel` crea procesos vía `Controlador`
- [x] `SimulationAreaPanel` muestra Ready/Running/Blocked/Finished reales
- [x] `CpuConfigPanel` alineado a los algoritmos soportados por el modelo
- [x] `BottomBarPanel` (stats, tabla, Gantt) leyendo datos del `Controlador`
- [x] `LogPanel` leyendo la bitácora del `Controlador`

### Pendiente antes de cerrar el sprint
- [ ] Compilar el proyecto con un JDK real (`mvn compile` / build del IDE)
- [ ] Probar manualmente en la UI: crear procesos, iniciar/pausar/detener/reiniciar
- [ ] Probar los 4 algoritmos de scheduling (FIFO, SJF, Prioridades, Round Robin)
- [ ] Probar con más de un núcleo configurado
- [ ] Verificar que el diagrama de Gantt se dibuja correctamente por núcleo
- [ ] Verificar que la cola de bloqueados (E/S) se actualiza en la UI
- [ ] Revisión de código por otro integrante del equipo
- [ ] `git add` de los archivos nuevos (`Cpu.java`, `Metrics.java`) y commit
