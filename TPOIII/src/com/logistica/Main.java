package com.logistica;

import java.io.*;
import java.util.*;

public class Main {

    // Clase Nodo para representar los nodos del grafo (clientes o centros)
    static class Nodo {
        int id;
        List<Arista> adyacentes = new ArrayList<>();

        public Nodo(int id) { 
            this.id = id;
        }
    }

    // Clase Arista para representar las rutas y sus costos entre nodos
    static class Arista {
        Nodo destino;
        double costo;

        public Arista(Nodo destino, double costo) {
            this.destino = destino;
            this.costo = costo;
        }
    }

    // Clase para representar los centros de distribución
    static class CentroDistribucion {
        int id;
        double costoUnitarioAlPuerto;
        double costoFijoAnual;

        public CentroDistribucion(int id, double costoUnitarioAlPuerto, double costoFijoAnual) {
            this.id = id;
            this.costoUnitarioAlPuerto = costoUnitarioAlPuerto;
            this.costoFijoAnual = costoFijoAnual;
        }
    }

    // Clase para representar los clientes y su producción anual
    static class Cliente {
        int id;
        double volumenProduccionAnual;

        public Cliente(int id, double volumenProduccionAnual) {
            this.id = id;
            this.volumenProduccionAnual = volumenProduccionAnual;
        }
    }

    // Variables globales
    static Map<Integer, Nodo> grafo = new HashMap<>(); // Grafo con nodos (clientes y centros)
    static List<CentroDistribucion> centros = new ArrayList<>(); // Lista de centros de distribución
    static List<Cliente> clientes = new ArrayList<>(); // Lista de clientes
    static int totalCentrosAConstruir = 3; // Total de centros a construir
    static double costoMinimoGlobal = Double.MAX_VALUE; // Costo mínimo encontrado
    static List<Integer> mejorCombinacionCentros = new ArrayList<>(); // Mejor combinación de centros
    static Map<Integer, Integer> asignacionClientes = new HashMap<>(); // Asignación de clientes a centros

    public static void main(String[] args) throws IOException {
        cargarDatos(); // Cargar datos de archivos
        List<Integer> seleccionados = new ArrayList<>();
        backtracking(seleccionados, 0); // Iniciar el proceso de selección de centros mediante backtracking
        mostrarResultado(); // Mostrar el resultado óptimo encontrado
    }

    // Método para cargar los datos desde archivos
    static void cargarDatos() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader("C:\\Users\\tomas\\eclipse-workspace\\TPOIII/clientesYCentros.txt"));
        int totalClientes = Integer.parseInt(br.readLine().split("\t")[0]);
        int totalCentros = Integer.parseInt(br.readLine().split("\t")[0]);

        // Cargar información de los centros de distribución
        for (int i = 0; i < totalCentros; i++) {
            String[] partes = br.readLine().split(",");
            int id = Integer.parseInt(partes[0]);
            double costoUnitarioAlPuerto = Double.parseDouble(partes[1]);
            double costoFijoAnual = Double.parseDouble(partes[2]);
            centros.add(new CentroDistribucion(id + 50, costoUnitarioAlPuerto, costoFijoAnual)); // Sumamos 50 para distinguir los IDs
            grafo.put(id + 50, new Nodo(id + 50));
        }

        // Cargar información de los clientes
        for (int i = 0; i < totalClientes; i++) {
            String[] partes = br.readLine().split(",");
            int id = Integer.parseInt(partes[0]);
            double volumen = Double.parseDouble(partes[1]);
            clientes.add(new Cliente(id, volumen));
            grafo.put(id, new Nodo(id));
        }
        br.close();

        // Cargar rutas
        br = new BufferedReader(new FileReader("C:\\Users\\tomas\\eclipse-workspace\\TPOIII/rutas.txt"));
        int totalRutas = Integer.parseInt(br.readLine().split("\t")[0]);

        // Cargar información de las rutas entre los nodos
        for (int i = 0; i < totalRutas; i++) {
            String[] partes = br.readLine().split(",");
            int origen = Integer.parseInt(partes[0]);
            int destino = Integer.parseInt(partes[1]);
            double costo = Double.parseDouble(partes[2]);

            Nodo nodoOrigen = grafo.get(origen);
            Nodo nodoDestino = grafo.get(destino);
            if (nodoOrigen != null && nodoDestino != null) {
                nodoOrigen.adyacentes.add(new Arista(nodoDestino, costo));
            }
        }
        br.close();
    }

    // Backtracking para seleccionar los centros óptimos
    static void backtracking(List<Integer> seleccionados, int inicio) {
        if (seleccionados.size() == totalCentrosAConstruir) {
            evaluarCombinacion(seleccionados); // Evaluar la combinación actual de centros
            return;
        }

        for (int i = inicio; i < centros.size(); i++) {
            seleccionados.add(centros.get(i).id);
            backtracking(seleccionados, i + 1); // Recursión para probar la siguiente combinación
            seleccionados.remove(seleccionados.size() - 1); // Retroceder para probar otra combinación
        }
    }

    // Evaluar la combinación de centros seleccionados
    static void evaluarCombinacion(List<Integer> centrosSeleccionados) {
        Map<Integer, Map<Integer, Double>> distanciasClientesCentros = new HashMap<>();

        // Asignar cada cliente al centro más cercano
        for (Cliente cliente : clientes) {
            double costoMinimo = Double.MAX_VALUE;
            int centroAsignado = -1;
            for (int idCentro : centrosSeleccionados) {
                double costo = dijkstra(cliente.id, idCentro);
                if (costo < costoMinimo) {
                    costoMinimo = costo;
                    centroAsignado = idCentro;
                }
            }
            if (centroAsignado == -1) {
                // Si no hay ruta desde el cliente a los centros seleccionados, la combinación no es válida
                return;
            }
            distanciasClientesCentros.put(cliente.id, new HashMap<>());
            distanciasClientesCentros.get(cliente.id).put(centroAsignado, costoMinimo);
        }

        // Calcular el costo total de la combinación
        double costoTotal = 0;
        Map<Integer, Double> volumenPorCentro = new HashMap<>();
        for (int idCentro : centrosSeleccionados) {
            volumenPorCentro.put(idCentro, 0.0);
        }

        // Sumar costos de transporte cliente-centro
        for (Cliente cliente : clientes) {
            Map<Integer, Double> distancias = distanciasClientesCentros.get(cliente.id);
            int centroAsignado = distancias.keySet().iterator().next();
            double costoTransporteClienteCentro = distancias.get(centroAsignado) * cliente.volumenProduccionAnual;
            costoTotal += costoTransporteClienteCentro;
            volumenPorCentro.put(centroAsignado, volumenPorCentro.get(centroAsignado) + cliente.volumenProduccionAnual);
        }

        // Sumar costos de los centros hacia el puerto y costos fijos
        for (int idCentro : centrosSeleccionados) {
            CentroDistribucion centro = obtenerCentroPorId(idCentro);
            double costoCentroPuerto = centro.costoUnitarioAlPuerto * volumenPorCentro.get(idCentro);
            costoTotal += costoCentroPuerto;
            costoTotal += centro.costoFijoAnual;
        }

        // Actualizar si se encontró un costo menor
        if (costoTotal < costoMinimoGlobal) {
            costoMinimoGlobal = costoTotal;
            mejorCombinacionCentros = new ArrayList<>(centrosSeleccionados);
            asignacionClientes.clear();
            for (Cliente cliente : clientes) {
                int centroAsignado = distanciasClientesCentros.get(cliente.id).keySet().iterator().next();
                asignacionClientes.put(cliente.id, centroAsignado);
            }
        }
    }

    // Algoritmo de Dijkstra para encontrar el costo mínimo entre dos nodos
    static double dijkstra(int origenId, int destinoId) {
        Map<Integer, Double> distancias = new HashMap<>();
        for (int id : grafo.keySet()) {
            distancias.put(id, Double.MAX_VALUE);
        }
        distancias.put(origenId, 0.0);
        PriorityQueue<NodoDistancia> cola = new PriorityQueue<>(Comparator.comparingDouble(n -> n.distancia));
        cola.add(new NodoDistancia(origenId, 0.0));

        while (!cola.isEmpty()) {
            NodoDistancia actual = cola.poll();
            if (actual.id == destinoId) {
                return actual.distancia;
            }
            if (actual.distancia > distancias.get(actual.id)) {
                continue;
            }
            Nodo nodoActual = grafo.get(actual.id);
            for (Arista arista : nodoActual.adyacentes) {
                double nuevaDistancia = actual.distancia + arista.costo;
                if (nuevaDistancia < distancias.get(arista.destino.id)) {
                    distancias.put(arista.destino.id, nuevaDistancia);
                    cola.add(new NodoDistancia(arista.destino.id, nuevaDistancia));
                }
            }
        }
        return Double.MAX_VALUE; // No se encontró una ruta válida
    }

    // Obtener un centro de distribución por su ID
    static CentroDistribucion obtenerCentroPorId(int id) {
        for (CentroDistribucion centro : centros) {
            if (centro.id == id) {
                return centro;
            }
        }
        return null;
    }

    // Mostrar el resultado óptimo encontrado
    static void mostrarResultado() {
        System.out.println("Costo total mínimo: " + costoMinimoGlobal);
        System.out.println("Centros de distribución a construir:");
        for (int idCentro : mejorCombinacionCentros) {
            System.out.println("- Centro " + (idCentro - 50));
        }
        System.out.println("Asignación de clientes a centros:");
        for (Map.Entry<Integer, Integer> entry : asignacionClientes.entrySet()) {
            System.out.println("Cliente " + entry.getKey() + " asignado al centro " + (entry.getValue() - 50));
        }
    }

    // Clase auxiliar para manejar nodos y sus distancias
    static class NodoDistancia {
        int id;
        double distancia;

        public NodoDistancia(int id, double distancia) {
            this.id = id;
            this.distancia = distancia;
        }
    }
}
