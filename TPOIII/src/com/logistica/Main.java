package com.logistica;

import java.io.*;
import java.util.*;

public class Main {

    // Nodo: representa un cliente o centro de distribucion
    static class Nodo {
        int id;
        List<Arista> adyacentes = new ArrayList<>(); // Lista de rutas conectadas
        public Nodo(int id) { this.id = id; }
    }

    // Arista: representa una ruta con un costo entre nodos
    static class Arista {
        Nodo destino;
        double costo;
        public Arista(Nodo destino, double costo) {
            this.destino = destino;
            this.costo = costo;
        }
    }

    // Centro de distribucion
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

    // Cliente y su produccion anual
    static class Cliente {
        int id;
        double volumenProduccionAnual;
        public Cliente(int id, double volumenProduccionAnual) {
            this.id = id;
            this.volumenProduccionAnual = volumenProduccionAnual;
        }
    }

    // Variables globales
    static Map<Integer, Nodo> grafo = new HashMap<>(); // Grafo con nodos
    static List<CentroDistribucion> centros = new ArrayList<>(); // Lista de centros
    static List<Cliente> clientes = new ArrayList<>(); // Lista de clientes
    static double costoMinimoGlobal = Double.MAX_VALUE; // Mejor costo encontrado
    static List<Integer> mejorCombinacionCentros = new ArrayList<>(); // Mejor combinacion de centros

    public static void main(String[] args) throws IOException {
        cargarDatos(); // Cargamos datos desde los archivos
        backtracking(new ArrayList<>(), 0, 0); // Empezamos seleccion de centros con costo inicial en 0
        mostrarMejorCombinacion(); // Mostramos la mejor combinacion encontrada

        // Imprimir las matrices de distancias y costos
        imprimirMatrizDijkstra();
        imprimirMatrizCostos();
    }

    // Cargamos datos de archivos
    static void cargarDatos() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader("/Users/tomasbonomo/Downloads/clientesYCentros.txt"));
        int totalClientes = Integer.parseInt(br.readLine().split("\t")[0]);
        int totalCentros = Integer.parseInt(br.readLine().split("\t")[0]);

        // Cargamos los centros
        for (int i = 0; i < totalCentros; i++) {
            String[] partes = br.readLine().split(",");
            int id = Integer.parseInt(partes[0]);
            double costoUnitarioAlPuerto = Double.parseDouble(partes[1]);
            double costoFijoAnual = Double.parseDouble(partes[2]);
            centros.add(new CentroDistribucion(id + 50, costoUnitarioAlPuerto, costoFijoAnual)); // ID + 50 para diferenciar
            grafo.put(id + 50, new Nodo(id + 50));
        }

        // Cargamos los clientes
        for (int i = 0; i < totalClientes; i++) {
            String[] partes = br.readLine().split(",");
            int id = Integer.parseInt(partes[0]);
            double volumen = Double.parseDouble(partes[1]);
            clientes.add(new Cliente(id, volumen));
            grafo.put(id, new Nodo(id));
        }
        br.close();

        // Cargamos las rutas
        br = new BufferedReader(new FileReader("/Users/tomasbonomo/Downloads/rutas.txt"));
        int totalRutas = Integer.parseInt(br.readLine().split("\t")[0]);

        // Procesamos cada ruta entre nodos
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

    // Backtracking para elegir centros optimos, con poda para ahorrar tiempo
    static void backtracking(List<Integer> seleccionados, int inicio, double costoParcial) {
        if (costoParcial >= costoMinimoGlobal) return;

        if (inicio == centros.size()) {
            double costoActual = evaluarCombinacion(seleccionados);
            System.out.println("Combinacion: " + seleccionados + " -> Costo: " + costoActual);
            if (costoActual < costoMinimoGlobal) {
                costoMinimoGlobal = costoActual;
                mejorCombinacionCentros = new ArrayList<>(seleccionados);
            }
            return;
        }

        for (int i = inicio; i < centros.size(); i++) {
            int centroId = centros.get(i).id;
            double costoFijoCentro = centros.get(i).costoFijoAnual;

            seleccionados.add(centroId);
            backtracking(seleccionados, i + 1, costoParcial + costoFijoCentro);
            seleccionados.remove(seleccionados.size() - 1);
        }
    }

    static double evaluarCombinacion(List<Integer> centrosSeleccionados) {
        double costoTotal = 0;

        for (Cliente cliente : clientes) {
            double costoMinimoCliente = Double.MAX_VALUE;
            for (int idCentro : centrosSeleccionados) {
                double distanciaClienteCentro = dijkstra(cliente.id, idCentro);
                if (distanciaClienteCentro != Double.MAX_VALUE) {
                    double costoClienteCentro = distanciaClienteCentro * cliente.volumenProduccionAnual;
                    CentroDistribucion centro = obtenerCentroPorId(idCentro);
                    double costoCentroPuerto = centro.costoUnitarioAlPuerto * cliente.volumenProduccionAnual;
                    double costoTotalCliente = costoClienteCentro + costoCentroPuerto;

                    if (costoTotalCliente < costoMinimoCliente) {
                        costoMinimoCliente = costoTotalCliente;
                    }
                }
            }

            if (costoMinimoCliente == Double.MAX_VALUE) {
                return Double.MAX_VALUE;
            }

            costoTotal += costoMinimoCliente;

            if (costoTotal >= costoMinimoGlobal) {
                return Double.MAX_VALUE;
            }
        }

        for (int idCentro : centrosSeleccionados) {
            CentroDistribucion centro = obtenerCentroPorId(idCentro);
            costoTotal += centro.costoFijoAnual;
        }

        return costoTotal;
    }

    static CentroDistribucion obtenerCentroPorId(int id) {
        for (CentroDistribucion centro : centros) {
            if (centro.id == id) return centro;
        }
        return null;
    }

    static void mostrarMejorCombinacion() {
        System.out.println("\n=== Mejor Combinación Encontrada ===");
        System.out.println("Costo mínimo total: " + costoMinimoGlobal);
        System.out.println("Centros seleccionados: " + mejorCombinacionCentros);

        System.out.println("\nAsignación de Clientes a Centros:");
        for (Cliente cliente : clientes) {
            int centroAsignado = -1;
            double costoMinimoCliente = Double.MAX_VALUE;

            for (int idCentro : mejorCombinacionCentros) {
                double distanciaClienteCentro = dijkstra(cliente.id, idCentro);
                if (distanciaClienteCentro != Double.MAX_VALUE) {
                    double costoClienteCentro = distanciaClienteCentro * cliente.volumenProduccionAnual;
                    CentroDistribucion centro = obtenerCentroPorId(idCentro);
                    double costoCentroPuerto = centro.costoUnitarioAlPuerto * cliente.volumenProduccionAnual;
                    double costoTotalCliente = costoClienteCentro + costoCentroPuerto;

                    if (costoTotalCliente < costoMinimoCliente) {
                        costoMinimoCliente = costoTotalCliente;
                        centroAsignado = idCentro;
                    }
                }
            }

            if (centroAsignado != -1) {
                System.out.println("Cliente " + cliente.id + " se asigna al Centro " + (centroAsignado - 50) + " con costo: " + costoMinimoCliente);
            } else {
                System.out.println("Cliente " + cliente.id + " no tiene un centro asignado.");
            }
        }
    }

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
            if (actual.id == destinoId) return actual.distancia;
            if (actual.distancia > distancias.get(actual.id)) continue;
            Nodo nodoActual = grafo.get(actual.id);
            for (Arista arista : nodoActual.adyacentes) {
                double nuevaDistancia = actual.distancia + arista.costo;
                if (nuevaDistancia < distancias.get(arista.destino.id)) {
                    distancias.put(arista.destino.id, nuevaDistancia);
                    cola.add(new NodoDistancia(arista.destino.id, nuevaDistancia));
                }
            }
        }
        return Double.MAX_VALUE;
    }

    static class NodoDistancia {
        int id;
        double distancia;
        public NodoDistancia(int id, double distancia) {
            this.id = id;
            this.distancia = distancia;
        }
    }

    static void imprimirMatrizDijkstra() {
        System.out.println("\n=== Matriz de Distancias (Clientes a Centros) ===");

        System.out.print("Cliente          \t");
        for (CentroDistribucion centro : centros) {
            System.out.print("C" + (centro.id - 50) + "\t");
        }
        System.out.println();

        for (Cliente cliente : clientes) {
            System.out.print("Cliente " + cliente.id + "\t\t");
            for (CentroDistribucion centro : centros) {
                double distancia = dijkstra(cliente.id, centro.id);
                if (distancia == Double.MAX_VALUE) {
                    System.out.print("INF\t");
                } else {
                    System.out.print(distancia + "\t");
                }
            }
            System.out.println();
        }
    }

    static void imprimirMatrizCostos() {
        System.out.println("\n=== Matriz de Costos (Clientes a Centros a Puerto) ===");

        System.out.print("Cliente            \t");
        for (CentroDistribucion centro : centros) {
            System.out.print("C" + (centro.id - 50) + "\t");
        }
        System.out.println();

        for (Cliente cliente : clientes) {
            System.out.print("Cliente " + cliente.id + "\t\t");
            for (CentroDistribucion centro : centros) {
                double distancia = dijkstra(cliente.id, centro.id);
                if (distancia == Double.MAX_VALUE) {
                    System.out.print("INF\t");
                } else {
                    double costoTotal = distancia * cliente.volumenProduccionAnual +
                            centro.costoUnitarioAlPuerto * cliente.volumenProduccionAnual;
                    System.out.print(costoTotal + "\t");
                }
            }
            System.out.println();
        }
    }
}
