package com.logistica;


public class Nodo implements Comparable<Nodo> {
    int id;
    double costo;

    public Nodo(int id, double costo) {
        this.id = id;
        this.costo = costo;
    }

    @Override
    public int compareTo(Nodo otro) {
        return Double.compare(this.costo, otro.costo);
    }
}

