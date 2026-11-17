package com.tasksync.backend.security;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Rate limiting simples em memória (janela deslizante), por chave
 * arbitrária (e-mail ou IP). Não sobrevive a reinício do processo nem é
 * compartilhado entre instâncias - suficiente para o tamanho atual do
 * projeto (uma única instância no Railway). Se o backend escalar para
 * múltiplas instâncias, isso precisa virar um armazenamento compartilhado
 * (ex.: Redis), senão cada instância teria seu próprio contador.
 *
 * Não há limpeza periódica de chaves antigas: o mapa cresce com o número
 * de e-mails/IPs distintos já vistos. Aceitável para o volume atual; se
 * isso virar um problema de memória, adicionar uma limpeza agendada.
 */
@Component
public class RateLimiterService {

    private final ConcurrentHashMap<String, Deque<Instant>> attempts = new ConcurrentHashMap<>();

    /**
     * @return true se a tentativa é permitida (e já é contabilizada); false
     * se o limite já foi atingido dentro da janela.
     */
    public boolean allow(String key, int maxAttempts, Duration window) {
        Deque<Instant> timestamps = attempts.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            Instant cutoff = Instant.now().minus(window);
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(cutoff)) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= maxAttempts) {
                return false;
            }
            timestamps.addLast(Instant.now());
            return true;
        }
    }
}
