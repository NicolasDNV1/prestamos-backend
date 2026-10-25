package com.alimentared.backend.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/** Reloj manipulable para probar SLA, ventana de empate y franjas de retiro. */
public class MutableClock extends Clock {

    private volatile Instant instante;
    private final ZoneId zona;

    public MutableClock(Instant instante, ZoneId zona) {
        this.instante = instante;
        this.zona = zona;
    }

    public void avanzar(Duration duracion) {
        instante = instante.plus(duracion);
    }

    public void reiniciar() {
        instante = Instant.now();
    }

    @Override
    public ZoneId getZone() {
        return zona;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return zone.equals(zona) ? this : new MutableClock(instante, zone);
    }

    @Override
    public Instant instant() {
        return instante;
    }
}
