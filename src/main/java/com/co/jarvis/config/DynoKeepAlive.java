package com.co.jarvis.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Evita que el dyno de Heroku entre en modo sleep.
 * Se ejecuta cada 25 minutos (Heroku duerme tras 30 min de inactividad).
 */
@Component
public class DynoKeepAlive {

    private static final Logger log = LoggerFactory.getLogger(DynoKeepAlive.class);

    /** 25 minutos en milisegundos */
    @Scheduled(fixedDelay = 1_500_000)
    public void ping() {
        log.debug("keep-alive ping — dyno activo");
    }
}
