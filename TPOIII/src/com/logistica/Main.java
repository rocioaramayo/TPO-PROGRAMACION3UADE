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

    public static void main(String[] args) throws IOException {
        cargarDatos(); // Cargar datos de archivos
        imprimirMatrizDijkstra(); // Imprimir la matriz de caminos más cortos
        imprimirMatrizCostos(); // Imprimir la matriz de costos de transporte
    }

    // Método para cargar los datos desde archivos
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
        br = new BufferedReader(new FileReader("/Users/tomasbonomo/Downloads/rutas.txt"));
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

    // Método para imprimir la matriz de distancias mínimas entre clientes y centros
    public static void imprimirMatrizDijkstra() {
        System.out.println("\n=== Matriz de Caminos Más Cortos (Clientes a Centros de Distribución) ===");

        // Imprimir encabezado de centros
        System.out.print("Cliente\\Centro\t");
        for (CentroDistribucion centro : centros) {
            System.out.print("C" + (centro.id - 50) + "\t");
        }
        System.out.println();

        // Calcular y mostrar distancias desde cada cliente a cada centro
        for (Cliente cliente : clientes) {
            System.out.print("Cliente " + cliente.id + "\t\t");
            for (CentroDistribucion centro : centros) {
                double distanciaMinima = dijkstra(cliente.id, centro.id);
                if (distanciaMinima == Double.MAX_VALUE) {
                    System.out.print("INF\t");  // Imprimir INF si no hay ruta
                } else {
                    System.out.print(distanciaMinima + "\t");  // Imprimir la distancia calculada
                }
            }
            System.out.println();
        }
    }

    // Método para imprimir la matriz de costos de transporte entre clientes y centros
    public static void imprimirMatrizCostos() {
        System.out.println("\n=== Matriz de Costos Totales de Transporte (Clientes a Centros a Puerto) ===");

        // Imprimir encabezado de centros
        System.out.print("Cliente          \t");
        for (CentroDistribucion centro : centros) {
            System.out.print("C" + (centro.id - 50) + "\t");
        }
        System.out.println();

        // Calcular y mostrar costos de transporte desde cada cliente a cada centro, incluyendo el costo al puerto
        for (Cliente cliente : clientes) {
            System.out.print("Cliente " + cliente.id + "\t\t");
            for (CentroDistribucion centro : centros) {
                double distanciaMinima = dijkstra(cliente.id, centro.id);
                if (distanciaMinima == Double.MAX_VALUE) {
                    System.out.print("INF\t");  // Imprimir INF si no hay ruta
                } else {
                    // Calcular el costo de transporte al centro
                    double costoTransporteCentro = distanciaMinima * cliente.volumenProduccionAnual;
                    
                    // Calcular el costo adicional desde el centro al puerto
                    double costoTransportePuerto = centro.costoUnitarioAlPuerto * cliente.volumenProduccionAnual;

                    // Costo total del cliente al centro y luego al puerto
                    double costoTotal = costoTransporteCentro + costoTransportePuerto;

                    System.out.print(costoTotal + "\t");  // Imprimir el costo total de transporte
                }
            }
            System.out.println();
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
