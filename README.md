# PyMApp Tool - SingleInstanceApp

A lightweight Java utility that guarantees a single running instance of a desktop application.

SingleInstanceApp allows secondary application launches to communicate with the primary instance, optionally restoring its main window and forwarding command-line arguments.

This project is part of the **PyMApp RAD Framework**, but it has no dependency on PyMApp and can be used completely standalone.

---

## Features

* Single application instance enforcement.
* Local IPC communication using TCP sockets.
* Forward command-line arguments to the primary instance.
* Restore or bring the primary window to the front.
* No external dependencies.
* Cross-platform.
* Java 8 compatible.

---

## Requirements

* Java 8 or later.

---

## Basic Usage

```java
public static void main(String[] args) {
    
    SingleInstanceApp.lock(
        MyApplication.class.getName()
    );
    
    new MyApplication().start();
}
```

---

## Full Usage Example

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

## How It Works

SingleInstanceApp calculates a deterministic TCP port from the fully qualified name of the application's main class.

The first process that successfully binds the port becomes the primary instance.

Subsequent executions:

1. Connect to the primary instance.
2. Send startup arguments.
3. Optionally request window restoration.
4. Exit immediately.

All communication occurs through the local loopback interface (127.0.0.1).

 First Execution
 ───────────────

        ┌─────────────────────┐
        │  Application Start  │
        └──────────┬──────────┘
                   │
                   ▼
        ┌─────────────────────┐
        │  Acquire Lock Port  │
        └──────────┬──────────┘
                   │ Success
                   ▼
        ┌─────────────────────┐
        │  Primary Instance   │
        │   Listening Mode    │
        └─────────────────────┘

 Subsequent Executions
 ─────────────────────

        ┌─────────────────────┐
        │  Application Start  │
        └──────────┬──────────┘
                   │
                   ▼
        ┌─────────────────────┐
        │ Connect to Primary  │
        └──────────┬──────────┘
                   │
                   ▼
        ┌─────────────────────┐
        │   Send Arguments    │
        │   Request Focus     │
        └──────────┬──────────┘
                   │
                   ▼
        ┌─────────────────────┐
        │    Exit Process     │
        └─────────────────────┘

---

## Design Goals

* Simplicity.
* Zero dependencies.
* Small footprint.
* Cross-platform compatibility.
* Easy integration into existing desktop applications.

---

## Limitations

* Intended for desktop applications.
* Communication is local to the machine.
* Not intended for distributed environments.
* Uses a TCP port as the locking mechanism.

---

## Why SingleInstanceApp?

Many existing solutions for single-instance Java applications rely on:

* Native libraries.
* Platform-specific APIs.
* File locking mechanisms.
* External dependencies.

SingleInstanceApp was designed with a different goal:

* Pure Java.
* Zero dependencies.
* Cross-platform behavior.
* Easy integration.
* Java 8 compatibility.

The entire implementation is based on local TCP communication through the loopback interface (127.0.0.1), making it lightweight, portable, and easy to maintain.

---

## Project Information

Project:
PyMApp Tool - SingleInstanceApp

Author: Fernando R. Olmedo {ferol.dev}

Repository: https://github.com/feroldev/PyMApp_tool_SingleInstanceApp

Version: 1.6.2

License: Apache License 2.0

---

## Maven Central

PyMApp Tool - SingleInstanceApp is available from Maven Central.

### Maven

```xml
<dependency>
    <groupId>dev.ferol</groupId>
    <artifactId>pymapp-tool-singleinstanceapp</artifactId>
    <version>1.6.2</version>
</dependency>
```

### Gradle

```gradle
implementation 'dev.ferol:pymapp-tool-singleinstanceapp:1.6.2'
```

The artifact is available at:

[PyMApp Tool - SingleInstanceApp on Maven Central](https://central.sonatype.com/artifact/dev.ferol/pymapp-tool-singleinstanceapp?utm_source=chatgpt.com)

---

## License

Copyright (c) 1999-2025 Fernando R. Olmedo {ferol.dev}

Licensed under the Apache License, Version 2.0.

