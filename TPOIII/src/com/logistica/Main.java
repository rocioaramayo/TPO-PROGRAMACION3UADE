package com.logistica;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Main {

    // Listas globales
    static List<Cliente> listaClientes = new ArrayList<>();
    static List<Centro> listaCentros = new ArrayList<>();

    static Map<Integer, Integer> nodoIdAIndice = new HashMap<>(); // Mapear IDs de nodos a índices en arrays
    static List<Integer> indiceANodoId = new ArrayList<>(); // Para mapear índices a IDs de nodos

    static List<List<Ruta>> grafo = new ArrayList<>(); // Grafo representado como lista de adyacencia

    // Mapas de IDs de clientes y centros a sus índices en las listas
    static Map<Integer, Integer> clienteIdAIndice = new HashMap<>();
    static Map<Integer, Integer> centroIdAIndice = new HashMap<>();

    // Variables para el algoritmo
    static double costoMinimo = Double.MAX_VALUE;
    static List<Centro> mejorCentrosAbiertos;
    static Map<Integer, Integer> mejorAsignacion; // Cliente ID -> Centro ID
    static int totalClientes;
    static int totalCentros;
    static double[][] costosMinimos; // [indiceCliente][indiceCentro]

    public static void main(String[] args) {
        // Limpiar estructuras
        listaClientes.clear();
        listaCentros.clear();
        grafo.clear();
        nodoIdAIndice.clear();
        indiceANodoId.clear();
        clienteIdAIndice.clear();
        centroIdAIndice.clear();

        // Leer los archivos de entrada
        leerClientesYCentros("C:\\Users\\Usuario\\Downloads\\clientesYCentros.txt");
        leerRutas("C:\\Users\\Usuario\\Downloads\\rutas.txt");

        // Inicializar variables
        totalClientes = listaClientes.size();
        totalCentros = listaCentros.size();
        costosMinimos = new double[totalClientes][totalCentros];

        // Mapear IDs a índices
        mapearIdsAIndices();

        // Preprocesamiento: Calcular costos mínimos desde cada cliente a cada centro
        preprocesarCostosMinimos();

        // Iniciar el backtracking sobre combinaciones de centros
        backtrackingCentros(0, new ArrayList<>(), 0);

        // Mostrar resultados
        mostrarResultados();
    }

    // Función para leer el archivo clientesYCentros.txt
    public static void leerClientesYCentros(String nombreArchivo) {
        try (BufferedReader br = new BufferedReader(new FileReader(nombreArchivo))) {
            String linea;
            // Leer total de clientes
            linea = br.readLine();
            int totalClientesArchivo = Integer.parseInt(linea.split("#")[0].trim());

            // Leer total de centros
            linea = br.readLine();
            int totalCentrosArchivo = Integer.parseInt(linea.split("#")[0].trim());

            // Leer datos de los centros
            for (int i = 0; i < totalCentrosArchivo; i++) {
                linea = br.readLine();
                String[] partes = linea.split(",");
                int id = Integer.parseInt(partes[0].trim());
                double costoUnitarioAlPuerto = Double.parseDouble(partes[1].trim());
                double costoFijoAnual = Double.parseDouble(partes[2].trim());
                listaCentros.add(new Centro(id, costoUnitarioAlPuerto, costoFijoAnual));
            }

            // Leer datos de los clientes
            for (int i = 0; i < totalClientesArchivo; i++) {
                linea = br.readLine();
                String[] partes = linea.split(",");
                int id = Integer.parseInt(partes[0].trim());
                double produccionAnual = Double.parseDouble(partes[1].trim());
                listaClientes.add(new Cliente(id, produccionAnual));
            }

        } catch (IOException e) {
            System.err.println("Error al leer el archivo " + nombreArchivo);
            e.printStackTrace();
        }
    }

    // Función para leer el archivo rutas.txt
    public static void leerRutas(String nombreArchivo) {
        try (BufferedReader br = new BufferedReader(new FileReader(nombreArchivo))) {
            String linea;
            // Leer total de rutas
            linea = br.readLine();
            int totalRutas = Integer.parseInt(linea.split("#")[0].trim());

            // Conjunto de nodos para asignar índices
            Set<Integer> nodos = new HashSet<>();

            // Leer datos de las rutas y recopilar nodos
            List<int[]> listaRutas = new ArrayList<>();

            for (int i = 0; i < totalRutas; i++) {
                linea = br.readLine();
                String[] partes = linea.split(",");
                int origen = Integer.parseInt(partes[0].trim());
                int destino = Integer.parseInt(partes[1].trim());
                double costo = Double.parseDouble(partes[2].trim());

                listaRutas.add(new int[]{origen, destino, (int) costo});

                nodos.add(origen);
                nodos.add(destino);
            }

            // Asignar índices a los nodos
            int indice = 0;
            for (Integer nodoId : nodos) {
                nodoIdAIndice.put(nodoId, indice++);
                indiceANodoId.add(nodoId);
                grafo.add(new ArrayList<>());
            }

            // Agregar rutas al grafo
            for (int[] rutaData : listaRutas) {
                int origenId = rutaData[0];
                int destinoId = rutaData[1];
                double costo = rutaData[2];

                int indiceOrigen = nodoIdAIndice.get(origenId);
                int indiceDestino = nodoIdAIndice.get(destinoId);

                grafo.get(indiceOrigen).add(new Ruta(indiceDestino, costo));
            }

        } catch (IOException e) {
            System.err.println("Error al leer el archivo " + nombreArchivo);
            e.printStackTrace();
        }
    }

    // Función para mapear IDs a índices
    public static void mapearIdsAIndices() {
        // Mapear clientes
        int indiceCliente = 0;
        for (Cliente cliente : listaClientes) {
            clienteIdAIndice.put(cliente.id, indiceCliente++);
        }

        // Mapear centros
        int indiceCentro = 0;
        for (Centro centro : listaCentros) {
            centroIdAIndice.put(centro.id, indiceCentro++);
        }
    }

    // Función para calcular los costos mínimos desde cada cliente a cada centro
    public static void preprocesarCostosMinimos() {
        for (int i = 0; i < listaClientes.size(); i++) {
            Cliente cliente = listaClientes.get(i);
            int indiceClienteNodo = nodoIdAIndice.get(cliente.id);
            double[] distancias = calcularCostosMinimos(indiceClienteNodo);

            for (int j = 0; j < listaCentros.size(); j++) {
                Centro centro = listaCentros.get(j);
                int indiceCentroNodo = nodoIdAIndice.get(centro.id);
                costosMinimos[i][j] = distancias[indiceCentroNodo];
            }
        }
    }

    // Algoritmo de Dijkstra
    public static double[] calcularCostosMinimos(int origenIndice) {
        int n = grafo.size();
        double[] distancias = new double[n];
        Arrays.fill(distancias, Double.MAX_VALUE);
        distancias[origenIndice] = 0;

        PriorityQueue<Nodo> cola = new PriorityQueue<>();
        cola.add(new Nodo(origenIndice, 0));

        while (!cola.isEmpty()) {
            Nodo actual = cola.poll();
            if (actual.costo > distancias[actual.id]) continue;

            for (Ruta ruta : grafo.get(actual.id)) {
                double nuevoCosto = actual.costo + ruta.costo;
                if (nuevoCosto < distancias[ruta.destino]) {
                    distancias[ruta.destino] = nuevoCosto;
                    cola.add(new Nodo(ruta.destino, nuevoCosto));
                }
            }
        }

        return distancias;
    }

    // Backtracking sobre las combinaciones de centros
    public static void backtrackingCentros(int indiceCentro, List<Centro> centrosAbiertos, double costoFijoAcumulado) {
        if (indiceCentro == totalCentros) {
            if (centrosAbiertos.isEmpty()) return; // Al menos debe haber un centro abierto
            // Calcular el costo total para esta combinación
            double costoTotal = costoFijoAcumulado;
            Map<Integer, Integer> asignacionClientes = new HashMap<>();
            boolean posible = true;

            for (int i = 0; i < listaClientes.size(); i++) {
                Cliente cliente = listaClientes.get(i);
                double costoMinCliente = Double.MAX_VALUE;
                Centro mejorCentro = null;

                for (Centro centro : centrosAbiertos) {
                    int indiceCentroLista = centroIdAIndice.get(centro.id);
                    double costoClienteCentro = costosMinimos[i][indiceCentroLista];
                    if (costoClienteCentro == Double.MAX_VALUE) continue; // No hay ruta

                    double costoTransporte = (costoClienteCentro + centro.costoUnitarioAlPuerto) * cliente.produccionAnual;

                    if (costoTransporte < costoMinCliente) {
                        costoMinCliente = costoTransporte;
                        mejorCentro = centro;
                    }
                }

                if (mejorCentro == null) {
                    posible = false;
                    break; // No se puede asignar este cliente a ningún centro abierto
                } else {
                    costoTotal += costoMinCliente;
                    asignacionClientes.put(cliente.id, mejorCentro.id);
                }

                // Poda
                if (costoTotal >= costoMinimo) {
                    posible = false;
                    break;
                }
            }

            if (posible && costoTotal < costoMinimo) {
                costoMinimo = costoTotal;
                mejorCentrosAbiertos = new ArrayList<>(centrosAbiertos);
                mejorAsignacion = new HashMap<>(asignacionClientes);
            }

        } else {
            // No abrir el centro actual
            backtrackingCentros(indiceCentro + 1, centrosAbiertos, costoFijoAcumulado);

            // Abrir el centro actual
            Centro centro = listaCentros.get(indiceCentro);
            centrosAbiertos.add(centro);
            double nuevoCostoFijo = costoFijoAcumulado + centro.costoFijoAnual;
            backtrackingCentros(indiceCentro + 1, centrosAbiertos, nuevoCostoFijo);
            centrosAbiertos.remove(centro);
        }
    }

    // Función para mostrar los resultados
    public static void mostrarResultados() {
        if (costoMinimo == Double.MAX_VALUE) {
            System.out.println("No se encontró una solución viable.");
            return;
        }

        System.out.println("Costo Total Mínimo: " + costoMinimo);
        System.out.println("Centros de Distribución a Construir:");
        for (Centro centro : mejorCentrosAbiertos) {
            System.out.println(" - Centro " + centro.id);
        }

        System.out.println("\nAsignación de Clientes a Centros:");
        for (Cliente cliente : listaClientes) {
            Integer centroAsignado = mejorAsignacion.get(cliente.id);
            System.out.println("Cliente " + cliente.id + " asignado al Centro " + centroAsignado);
        }
    }
}
