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
    static double costoMinimoGlobal = Double.MAX_VALUE; // Costo mínimo encontrado
    static List<Integer> mejorCombinacionCentros = new ArrayList<>(); // Mejor combinación de centros
    static int totalCentrosAConstruir = 3; // Total de centros a construir

    public static void main(String[] args) throws IOException {
        cargarDatos(); // Cargar datos de archivos
        backtracking(new ArrayList<>(), 0); // Iniciar el proceso de selección de centros mediante backtracking
        mostrarMejorCombinacion(); // Mostrar el resultado óptimo encontrado
    }

    //  cargar los datos desde archivos
    static void cargarDatos() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader("/Users/tomasbonomo/Downloads/clientesYCentros.txt"));
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

        // Cargar info de los clientes
        for (int i = 0; i < totalClientes; i++) {
            String[] partes = br.readLine().split(",");
            int id = Integer.parseInt(partes[0]);
            double volumen = Double.parseDouble(partes[1]);
            clientes.add(new Cliente(id, volumen));
            grafo.put(id, new Nodo(id));
        }
        br.close();

        // Cargar rutas
        br = new BufferedReader(new FileReader("/Users/tomasbonomo/Downloads/rutas.txt"));
        int totalRutas = Integer.parseInt(br.readLine().split("\t")[0]);

        // Cargar info de las rutas entre los nodos
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

    // Backtracking para seleccionar los centros optimos
    static void backtracking(List<Integer> seleccionados, int inicio) {
        if (seleccionados.size() == totalCentrosAConstruir) {
            double costoActual = evaluarCombinacion(seleccionados); // EvalUar la combinacion actual de centros
            System.out.println("Combinación: " + seleccionados + " -> Costo: " + costoActual);
            if (costoActual < costoMinimoGlobal) {
                costoMinimoGlobal = costoActual;
                mejorCombinacionCentros = new ArrayList<>(seleccionados);
            }
            return;
        }

        for (int i = inicio; i < centros.size(); i++) {
            seleccionados.add(centros.get(i).id);
            backtracking(seleccionados, i + 1); // Recursión para probar la siguiente combinación
            seleccionados.remove(seleccionados.size() - 1); // Retroceder para probar otra combinación
        }
    }

    // Evaluar el costo total de una combinación de centros seleccionados
    static double evaluarCombinacion(List<Integer> centrosSeleccionados) {
        double costoTotal = 0;
        Map<Integer, Double> volumenPorCentro = new HashMap<>();

        // Inicializar volumen por centro
        for (int idCentro : centrosSeleccionados) {
            volumenPorCentro.put(idCentro, 0.0);
        }

        // Calcular costos de transporte cliente-centro y centro-puerto
        for (Cliente cliente : clientes) {
            double costoMinimoCliente = Double.MAX_VALUE;
            for (int idCentro : centrosSeleccionados) {
                double distanciaClienteCentro = dijkstra(cliente.id, idCentro);
                if (distanciaClienteCentro != Double.MAX_VALUE) {
                    // Costo de transporte del cliente al centro
                    double costoClienteCentro = distanciaClienteCentro * cliente.volumenProduccionAnual;
                    // Costo adicional del centro al puerto
                    CentroDistribucion centro = obtenerCentroPorId(idCentro);
                    double costoCentroPuerto = centro.costoUnitarioAlPuerto * cliente.volumenProduccionAnual;
                    double costoTotalCliente = costoClienteCentro + costoCentroPuerto;

                    if (costoTotalCliente < costoMinimoCliente) {
                        costoMinimoCliente = costoTotalCliente;
                        volumenPorCentro.put(idCentro, volumenPorCentro.get(idCentro) + cliente.volumenProduccionAnual);
                    }
                }
            }
            costoTotal += costoMinimoCliente;
        }

        // Añadir costos fijos de cada centro
        for (int idCentro : centrosSeleccionados) {
            CentroDistribucion centro = obtenerCentroPorId(idCentro);
            costoTotal += centro.costoFijoAnual;
        }

        return costoTotal;
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

    // Mostrar la mejor combinación de centros encontrada
    static void mostrarMejorCombinacion() {
        System.out.println("\n=== Mejor Combinación Encontrada ===");
        System.out.println("Costo mínimo total: " + costoMinimoGlobal);
        System.out.println("Centros de distribución seleccionados: " + mejorCombinacionCentros);
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


