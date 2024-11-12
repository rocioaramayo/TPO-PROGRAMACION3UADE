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

    // Backtracking para elegir centros optimos, sin limite de cantidad de centros
    static void backtracking(List<Integer> seleccionados, int inicio, double costoParcial) {
        // Poda si el costo parcial ya supera el mejor costo encontrado
        if (costoParcial >= costoMinimoGlobal) return;

        // Cuando ya no hay más centros para seleccionar, evaluamos la combinación actual
        if (inicio == centros.size()) {
            double costoActual = evaluarCombinacion(seleccionados); // Calcula el costo de esta combinacion
            System.out.println("Combinacion: " + seleccionados + " -> Costo: " + costoActual);
            if (costoActual < costoMinimoGlobal) {
                costoMinimoGlobal = costoActual;
                mejorCombinacionCentros = new ArrayList<>(seleccionados);
            }
            return;
        }

        // Probar seleccionar el siguiente centro
        for (int i = inicio; i < centros.size(); i++) {
            int centroId = centros.get(i).id;
            double costoFijoCentro = centros.get(i).costoFijoAnual;

            // Agrega centro seleccionado y recurre con costo actualizado
            seleccionados.add(centroId);
            backtracking(seleccionados, i + 1, costoParcial + costoFijoCentro);
            seleccionados.remove(seleccionados.size() - 1); // Retrocede para probar otro centro
        }
    }

    // Evalua el costo de una combinacion seleccionada con poda
    static double evaluarCombinacion(List<Integer> centrosSeleccionados) {
        double costoTotal = 0;

        // Calcula costo cliente-centro y centro-puerto
        for (Cliente cliente : clientes) {
            double costoMinimoCliente = Double.MAX_VALUE;
            for (int idCentro : centrosSeleccionados) {
                double distanciaClienteCentro = dijkstra(cliente.id, idCentro);
                if (distanciaClienteCentro != Double.MAX_VALUE) {
                    // Costo transporte cliente-centro
                    double costoClienteCentro = distanciaClienteCentro * cliente.volumenProduccionAnual;
                    // Costo extra centro-puerto
                    CentroDistribucion centro = obtenerCentroPorId(idCentro);
                    double costoCentroPuerto = centro.costoUnitarioAlPuerto * cliente.volumenProduccionAnual;
                    double costoTotalCliente = costoClienteCentro + costoCentroPuerto;

                    if (costoTotalCliente < costoMinimoCliente) {
                        costoMinimoCliente = costoTotalCliente;
                    }
                }
            }
            costoTotal += costoMinimoCliente;

            // Poda si el costo total ya supera el mejor costo
            if (costoTotal >= costoMinimoGlobal) {
                return Double.MAX_VALUE; // Salida rapida
            }
        }

        // Agrega costo fijo de cada centro
        for (int idCentro : centrosSeleccionados) {
            CentroDistribucion centro = obtenerCentroPorId(idCentro);
            costoTotal += centro.costoFijoAnual;
        }

        return costoTotal;
    }

    // Busca centro por ID
    static CentroDistribucion obtenerCentroPorId(int id) {
        for (CentroDistribucion centro : centros) {
            if (centro.id == id) return centro;
        }
        return null;
    }

    // Muestra la mejor combinacion que encontramos
    static void mostrarMejorCombinacion() {
        System.out.println("\n=== Mejor Combinacion Encontrada ===");
        System.out.println("Costo minimo total: " + costoMinimoGlobal);
        System.out.println("Centros seleccionados: " + mejorCombinacionCentros);
    }

    // Algoritmo Dijkstra para encontrar costo minimo entre nodos
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
        return Double.MAX_VALUE; // No encontramos ruta
    }

    // Clase auxiliar para nodos y distancias en Dijkstra
    static class NodoDistancia {
        int id;
        double distancia;
        public NodoDistancia(int id, double distancia) {
            this.id = id;
            this.distancia = distancia;
        }
    }
}
