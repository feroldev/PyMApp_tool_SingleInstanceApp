# PyMApp Tool - SingleInstanceApp

Utilidad ligera para Java que garantiza la ejecución de una única instancia de una aplicación de escritorio.

SingleInstanceApp permite que las ejecuciones secundarias se comuniquen con la instancia principal, restauren la ventana principal y transmitan argumentos de línea de comandos.

Este proyecto forma parte de **PyMApp RAD Framework**, pero no posee dependencias con PyMApp y puede utilizarse de forma completamente independiente.

---

## Características

* Control de instancia única.
* Comunicación IPC mediante sockets TCP locales.
* Transferencia de argumentos de consola.
* Restauración opcional de la ventana principal.
* Sin dependencias externas.
* Multiplataforma.
* Compatible con Java 8.

---

## Requisitos

* Java 8 o superior.

---

## Ejemplo Básico

```java
public static void main(String[] args) {
    
    SingleInstanceApp.lock(
        MyApplication.class.getName()
    );
    
    new MyApplication().start();
}
```

---

## Ejemplo Completo

```java
public static void main(String[] args) {
    
    MyApplication app = new MyApplication();
    
    SingleInstanceApp.lock(
        app.getClass().getName(),
        app::setMainWinToFront,
        app::setRemoteArgs,
        args
    );
    
    app.load(args);
    
    EventQueue.invokeLater(app::run);
}
```

---

## Funcionamiento

SingleInstanceApp calcula un puerto TCP determinístico a partir del nombre completo de la clase principal.

La primera ejecución que logra enlazar dicho puerto se convierte en la instancia principal.

Las ejecuciones posteriores:

1. Se conectan a la instancia principal.
2. Transfieren los argumentos recibidos.
3. Solicitan restaurar la ventana principal.
4. Finalizan automáticamente.

Toda la comunicación se realiza exclusivamente sobre la interfaz local 127.0.0.1.

 Primera Ejecución
 ─────────────────

        ┌─────────────────────┐
        │  Aplicación Start   │
        └──────────┬──────────┘
                   │
                   ▼
        ┌─────────────────────┐
        │   Adquirir Puerto   │
        │     de Bloqueo      │
        └──────────┬──────────┘
                   │ Éxito
                   ▼
        ┌─────────────────────┐
        │ Instancia Primaria  │
        │    Modo Escucha     │
        └─────────────────────┘

 Segunda Ejecución
 ─────────────────

        ┌─────────────────────┐
        │  Aplicación Start   │
        └──────────┬──────────┘
                   │
                   ▼
        ┌─────────────────────┐
        │ Conectar con Primera│
        └──────────┬──────────┘
                   │
                   ▼
        ┌─────────────────────┐
        │  Enviar Argumentos  │
        │ Solicitud de Enfoque│
        └──────────┬──────────┘
                   │
                   ▼
        ┌─────────────────────┐
        │   Fin del Proceso   │
        └─────────────────────┘

---

## Objetivos de Diseño

* Simplicidad.
* Cero dependencias.
* Bajo consumo de recursos.
* Compatibilidad multiplataforma.
* Integración sencilla.

---

## Limitaciones

* Diseñado para aplicaciones de escritorio.
* Comunicación exclusivamente local.
* No pensado para entornos distribuidos.
* Utiliza un puerto TCP como mecanismo de bloqueo.

---

## ¿Por qué SingleInstanceApp?

Muchas soluciones existentes para aplicaciones Java de instancia única se basan en:

* Bibliotecas nativas.
* API específicas de la plataforma.
* Mecanismos de bloqueo de archivos.
* Dependencias externas.

SingleInstanceApp se diseñó con un objetivo diferente:

* Java puro.
* Cero dependencias.
* Comportamiento multiplataforma.
* Fácil integración.
* Compatibilidad con Java 8.

Toda la implementación se basa en la comunicación TCP local a través de la interfaz de bucle local (127.0.0.1), lo que la hace ligera, portátil y fácil de mantener.

---

## Información del Proyecto

Proyecto:
PyMApp Tool - SingleInstanceApp

Autor:
Fernando R. Olmedo {ferol.dev}

Repositorio:
https://github.com/feroldev/PyMApp_tool_SingleInstanceApp

Versión:
1.6.2

Licencia:
Apache License 2.0

---

## Licencia

Copyright (c) 1999-2025 Fernando R. Olmedo {ferol.dev}

Licenciado bajo Apache License 2.0.

