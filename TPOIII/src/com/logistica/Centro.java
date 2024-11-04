package com.logistica;

public class Centro {
    int id;
    double costoUnitarioAlPuerto;
    double costoFijoAnual;

    public Centro(int id, double costoUnitarioAlPuerto, double costoFijoAnual) {
        this.id = id;
        this.costoUnitarioAlPuerto = costoUnitarioAlPuerto;
        this.costoFijoAnual = costoFijoAnual;
    }
}
