/* 
 * @(#)SingleInstanceApp.java    1.6.2 25/07/30
 * 
 * Copyright (c) 1999-2025 OLMEDO Fernando R. {ferol.dev}
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package dev.ferol.pymapp.tool;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import java.util.function.Consumer;
import java.util.regex.Pattern;


/**
 * Utilidad que proporciona un mecanismo simple para garantizar la ejecución de
 * una única instancia de una aplicación Java dentro del mismo equipo.
 * <br><br>
 * La primera instancia en iniciar adquiere un puerto de bloqueo determinístico
 * ({@code TCP local 127.0.0.1}) y se convierte en la instancia principal. Las
 * ejecuciónes posteriores detectan dicha instancia, transmiten sus argumentos
 * de línea de comandos y finalizan automáticamente de forma silenciosa.
 * <br><br>
 * Como opción la instancia principal puede restaurar su ventana principal
 * y procesar los argumentos recibidos desde nuevas instancias de ejecución
 * mediante el uso de callbacks funcionales basados en {@link Runnable} y
 * {@link Consumer}.
 * <br><br>
 * La comunicación entre procesos se realiza exclusivamente mediante sockets
 * TCP sobre la interfaz de red local {@code 127.0.0.1}.
 * <br><br>
 * Esta clase es completamente estática y no requiere dependencias externas.
 * <br><br>
 * Características principales:
 * <br><br>
 * #). Control estricto de instancia única por identificador de clase.<br>
 * #). Comunicación IPC local optimizada sin archivos de bloqueo flotantes.<br>
 * #). Transferencia segura de argumentos de consola en tiempo de ejecución.<br>
 * #). Restauración opcional de la interfaz gráfica, de instancia primaria.<br>
 * #). Diseño compatible con Java 8 o superior sin dependencias externas.
 * <br><br>
 * Ejemplo de uso estándar en el ciclo de vida de arranque:
 * <pre>{@code
 * public static void main(String[] args) {
 *     // 1. Instanciación ultra-ligera de la aplicación (sin inicializar UI pesada)
 *     MyApplication app = new MyApplication();
 *     
 *     // 2. Control de exclusión mutua inmediato
 *     SingleInstanceApp.lock(
 *         app.getClass().getName(),
 *         app::setMainWinToFront,
 *         app::setRemoteArgs,
 *         args
 *     );
 *     
 *     // 3. Flujo seguro: si continúa aquí, somos la instancia primaria única
 *     app.start(args);
 * }
 * }</pre>
 * 
 * @see #lock(String)
 * @see #lock(String, Runnable)
 * @see #lock(String, Consumer, String[])
 * @see #lock(String, Runnable, Consumer, String[])
 * @see #unlock()
 * @see Runnable
 * @see Consumer
 * 
 * @author    OLMEDO Fernando R. {ferol.dev}
 * @version    1.6.2 25/07/30
 */
public final class SingleInstanceApp {
    private static InetAddress address; // Dirección del Host
    private static int lockPort = 0; // Puerto de Bloqueo
    private static volatile ServerSocket serverSocket = null; // Socket de Comunicación
    private static volatile String messageHeader = null; // Encabezado del Mensaje de Inicio de Conexión
    private static volatile String answer = null; // Mensaje de Confirmación de Conexión
    private static final String SEPARATOR = "\u001F"; // Separador de Control Unitario ASCII
    private static StringBuilder messagePack = null; // Mensaje Empaquetado: messageHeader + SEPARADOR + arg1 + SEPARADOR + arg2..
    private static volatile Runnable appShow = null; // Método que Trae al Frente en el Escritorio a la Instancia Primaria (opcional)
    private static volatile Consumer<String[]> argsHandler = null; // Callback para Procesar Argumentos en Instancia Primaria (opcional)
    
    
/*----------------------------------------------------------------------------*/
/*                                    API                                     */
/*----------------------------------------------------------------------------*/
    
    
    /**
     * Intenta registrar la aplicación e iniciar el bloqueo de instancia única
     * utilizando la configuración completa de callbacks e intercambio de
     * argumentos IPC.<br>
     * Si el puerto determinístico calculado está libre, el método registra la
     * instancia actual como la "Instancia Primaria (Servidor)" e inicia un hilo
     * daemon de escucha en segundo plano.<br>
     * Si por el contrario el puerto está ocupado por una instancia previa de la
     * misma aplicación, transmite los {@code args} actuales hacia ella y finaliza
     * la JVM secundaria mediante {@code System.exit(0)}.
     * <br><br>
     * 
     * @param className Nombre completamente cualificado de la clase principal
     *        de la aplicación.
     * @param show Callback opcional que se ejecutará en la instancia primaria
     *        cuando una instancia secundaria intente abrirse. Ideado para
     *        restaurar el foco visual.
     * @param handler Callback opcional para procesar los argumentos de consola
     *        remotos recibidos desde ejecuciónes secundarias.
     * @param args Arreglo de argumentos de la línea de comandos de la ejecución
     *        actual.
     * 
     * @throws IllegalArgumentException Si {@code className} es {@code null}, está
     *         vacío o compuesto únicamente por espacios en blanco.
     * 
     * @see #lock(String)
     * @see #lock(String, Runnable)
     * @see #lock(String, Consumer, String[])
     * @see #unlock()
     * @see Runnable
     * @see Consumer
     */
    public static synchronized void lock(
        String className,
        Runnable show,
        Consumer<String[]> handler,
        String[] args
    ) throws IllegalArgumentException {
        
        if (lockPort != 0) { // Evita Re-Ejecución si ya está Inicializado
            return;
        }
        
        if (className == null || className.trim().isEmpty()) {
            throw new IllegalArgumentException("The [className] parameter cannot be null or empty.");
        }
        
        messageHeader = className + ".show";
        answer = className + ".exit";
        
        appShow = show;
        argsHandler = handler;
        
      /*(a) Construcción del Mensaje Empaquetado: messageHeader + SEPARADOR + arg1 + SEPARADOR + arg2.. */
        messagePack = new StringBuilder(messageHeader);
        
        if (args != null && args.length > 0) {
            
            for (String arg : args) {
                messagePack.append(SEPARATOR).append(arg);
            }
        }
      /*(a)*/
        
        try {
            address = InetAddress.getByAddress(new byte[]{127, 0, 0, 1});
        } catch (UnknownHostException e) { /* Imposible para 127.0.0.1 */ }
        
        int basePort = (Math.abs(className.hashCode()) % 16383) + 49152; // Puerto Base Calculado por Hash Range: [49152, 65535]
        
      /*(b) Bucle de Determinación de Puerto y Comunicación (Máximo 7 intentos por seguridad) */
        for (int i = 0; i < 7; i++) {
            lockPort = basePort + i;
            
            if (acquireLockPort()) { // Si es la Instancia Primaria Queda a la Escucha de Secundarias
                listenToNewInstances();
                return; 
            }
            
            if (notifyExistingInstance()) { // Verificar Puerto Ocupado por Instancia Secundaria y Proceder.
                System.exit(0); 
            }
        }
      /*(b)*/
        lockPort = 0;
    }
    
    
    /**
     * Sobrecarga de conveniencia básica para exclusión mutua estricta.<br>
     * Diseñada para aplicaciones de segundo plano, servicios o herramientas de
     * terminal que requieran asegurar una única instancia activa pero no necesitan
     * gestionar interfaces gráficas ni transferir argumentos remotos.
     * <br><br>
     * 
     * @param className Nombre completamente cualificado de la clase principal
     *        de la aplicación.
     * 
     * @throws IllegalArgumentException Si {@code className} es {@code null}, está
     *         vacío o compuesto únicamente por espacios en blanco.
     * 
     * @see #lock(String, Runnable)
     * @see #lock(String, Consumer, String[])
     * @see #lock(String, Runnable, Consumer, String[])
     * @see #unlock()
     */
    public static synchronized void lock(String className) throws IllegalArgumentException {
        lock(className, null, null, null);
    }
    
    
    /**
     * Sobrecarga de conveniencia orientada a aplicaciones con interfaz gráfica
     * estática.<br>
     * Registra la aplicación y define el callback para restaurar o traer al frente
     * la ventana principal de la instancia primaria cuando se intente abrir una
     * ejecución secundaria, descartando los argumentos de consola.
     * <br><br>
     * 
     * @param className Nombre completamente cualificado de la clase principal
     *        de la aplicación.
     * @param show Callback opcional que se ejecutará en la instancia primaria
     *        cuando una instancia secundaria intente abrirse. Ideado para
     *        restaurar el foco visual.
     * 
     * @throws IllegalArgumentException Si {@code className} es {@code null}, está
     *         vacío o compuesto únicamente por espacios en blanco.
     * 
     * @see #lock(String)
     * @see #lock(String, Consumer, String[])
     * @see #lock(String, Runnable, Consumer, String[])
     * @see #unlock()
     * @see Runnable
     */
    public static synchronized void lock(String className, Runnable show) throws IllegalArgumentException {
        lock(className, show, null, null);
    }
    
    
   /**
    * Sobrecarga de conveniencia orientada al procesamiento exclusivo de
    * argumentos IPC.<br>
    * Permite omitir la restauración forzada de foco en la interfaz de usuario
    * pero delega el procesamiento en diferido de los argumentos que envíe
    * cualquier instancia secundario.
    * <br><br>
    * 
    * @param className Nombre completamente cualificado de la clase principal
    *        de la aplicación.
    * @param handler Callback opcional para procesar los argumentos de consola
    *        remotos recibidos desde ejecuciónes secundarias.
    * @param args Arreglo de argumentos de la línea de comandos de la ejecución
    *        actual.
    * 
    * @throws IllegalArgumentException Si {@code className} es {@code null}, está
    *         vacío o compuesto únicamente por espacios en blanco.
    * 
    * @see #lock(String)
    * @see #lock(String, Runnable)
    * @see #lock(String, Runnable, Consumer, String[])
    * @see #unlock()
    * @see Consumer
    */
    public static synchronized void lock(
        String className,
        Consumer<String[]> handler,
        String[] args
    ) throws IllegalArgumentException {
        lock(className, null, handler, args);
    }
    
    
    /**
     * Libera de forma segura los recursos del socket del servidor y limpia las
     * variables estáticas de contexto de la utilidad.
     * <br><br>
     * Este método debe ser invocado de forma explícita al finalizar controladamente
     * la aplicación primaria (por ejemplo, al cerrar la ventana principal o invocar
     * un comando de salida de la app) para asegurar que el puerto TCP nativo quede
     * libre inmediatamente en el Sistema Operativo, evitando bloqueos residuales 
     * en reinicios rápidos de la aplicación.
     * <br><br>
     * 
     * @see #lock(String)
     * @see #lock(String, Runnable)
     * @see #lock(String, Consumer, String[])
     * @see #lock(String, Runnable, Consumer, String[])
     */
    public static void unlock() {
        address = null;
        lockPort = 0;
        messageHeader = null;
        answer = null;
        messagePack = null;
        appShow = null;
        argsHandler = null;
        
        if (serverSocket != null) {
            
            if (!serverSocket.isClosed()) {
                
                try {
                    serverSocket.close();
                } catch (IOException ignored) {}
            }
            serverSocket = null;
        }
    }
    
    
/*----------------------------------------------------------------------------*/
/*                                Private Code                                */
/*----------------------------------------------------------------------------*/
    
    
    /**
     * Constructor privado para evitar la instanciación de la clase (Utility).
     */
    private SingleInstanceApp() {}
    
    
    /**
     * Intenta inicializar el {@link ServerSocket} en la interfaz local utilizando
     * el puerto de bloqueo asignado en la iteración actual.
     * <br><br>
     * 
     * @return {@code true} si logró enlazar (bind) el socket con éxito
     *         convirtiendose en la instancia primaria; {@code false} si el
     *         puerto ya está en uso o no disponible.
     * <br><br>
     * 
     * @see #lock(String, Runnable, Consumer, String[])
     */
    private static boolean acquireLockPort() {
        
        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(address, lockPort));
            return true;
        } catch (IOException e) {
            return false; // Puerto Ocupado
        }
    }
    
    
    /**
     * Notifica a la instancia primaria preexistente mediante una conexión por
     * socket cliente efímero. Transmite la ráfaga de datos empaquetados que
     * contiene la cabecera y los parámetros de inicialización.
     * <br><br>
     * 
     * @return {@code true} si la instancia primaria respondió de forma correcta
     *         reconociendo la trama de datos; {@code false} si se produjo una
     *         falla de conexión o el puerto está ocupado por una aplicación
     *         diferente a la definida.
     * 
     * @see #lock(String, Runnable, Consumer, String[])
     */
    private static boolean notifyExistingInstance() {
        
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(address, lockPort), 200); // Timeout Corto para no Congelar Arranque si Puerto no Responde
            
            try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                
                out.println(messagePack.toString()); // Envía todo en una sola Línea
                
                String response = in.readLine();
                return answer.equals(response); // Confirma si la Respuesta es de una Instancia de la Aplicación
            }
        } catch (IOException e) { 
            return false; 
        }
    }
    
    
    /**
     * Inicia un hilo de escucha en segundo plano (Thread Daemon) dedicado a
     * aceptar las conexiones de futuras instancias secundarias mientras el
     * socket del servidor se mantenga abierto.<br>
     * Procesa la cabecera del mensaje, desempaqueta los tokens mediante el
     * delimitador ASCII y despacha los callbacks de forma asíncrona y segura.
     * <br><br>
     * 
     * @see #lock(String, Runnable, Consumer, String[])
     * @see Runnable
     * @see Consumer 
     */
    private static void listenToNewInstances() {
        Thread threadListener = new Thread(() -> {
            
            while (serverSocket != null && !serverSocket.isClosed()) {
                
                try (Socket client = serverSocket.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                     PrintWriter out = new PrintWriter(client.getOutputStream(), true)) {
                    
                    String incomLine = in.readLine();
                    if (incomLine != null && incomLine.startsWith(messageHeader)) {
                        
                        out.println(answer); // Confirmar para Liberar Cliente SingleInstanceApp
                        
  /*(a) Extraer Argumentos si Existen */
                        String[] args = incomLine.split(Pattern.quote(SEPARATOR));
                        final String[] remoteArgs;
                        
                        if (args.length > 1) {
                            remoteArgs = new String[args.length - 1];
                            System.arraycopy(args, 1, remoteArgs, 0, remoteArgs.length);
                        } else {
                            remoteArgs = new String[0];
                        }
  /*(a)*/
                        if (appShow != null) { // Muestra en el Escritorio esta Instancia de la Aplicación
                            appShow.run();
                        }

                        if (argsHandler != null && remoteArgs.length > 0) { // Proceso Argumentos Remotos por esta Instancia
                            argsHandler.accept(remoteArgs);
                        }
                    }
                } catch (IOException e) { /* Manejo Silencioso de Desconexiónes Abruptas */ }
            }
        });
        
        threadListener.setDaemon(true);
        threadListener.start();
    }
}
