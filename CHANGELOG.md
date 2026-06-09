# Changelog

Todos los cambios relevantes de este proyecto serán documentados en este archivo.

## [1.6.2] - 2025-07-30

### Primera publicación pública

#### Características

* Control de instancia única mediante sockets TCP locales.
* Comunicación IPC entre procesos.
* Restauración opcional de la ventana principal.
* Transferencia de argumentos de línea de comandos.
* Sin dependencias externas.
* Compatible con Java 8 o superior.

#### Mejoras

* Limpieza completa de recursos mediante `unlock()`.
* Compatibilidad Java 8 restaurada.
* Separación segura de argumentos mediante delimitador ASCII Unit Separator.
* Protección frente a colisiones accidentales de puertos.

