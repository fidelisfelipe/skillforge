package com.skillforge.hub.dojo;

import com.skillforge.dojo.message.KataThemeResponseMessage.KataDelivery;
import com.skillforge.dojo.message.KataThemeResponseMessage.KataTheme;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service para gerenciar Temas e Katas
 *
 * TODO Phase 5.1: Implementar persistência real (DB ou arquivo THEMES.json)
 */
@Service
@Slf4j
public class KataService {

    // TODO Phase 5.1: Carregar de quests/dojo/java-21-certified/THEMES.json
    private static final Map<String, KataTheme> THEMES = Map.ofEntries(
        Map.entry("virtual-threads", new KataTheme(
            "virtual-threads",
            "Virtual Threads & Concurrency",
            "Master Java 21 Virtual Threads for efficient concurrency",
            "intermediate",
            "Oracle Java 21 - Module 7",
            List.of("KATA-001A", "KATA-001B", "KATA-001C")
        )),
        Map.entry("records", new KataTheme(
            "records",
            "Records & Data Classes",
            "Create immutable data carriers with Records",
            "beginner",
            "Oracle Java 21 - Module 2",
            List.of("KATA-002A", "KATA-002B", "KATA-002C")
        )),
        Map.entry("pattern-matching", new KataTheme(
            "pattern-matching",
            "Pattern Matching",
            "Destructure and match complex data structures",
            "advanced",
            "Oracle Java 21 - Module 5",
            List.of("KATA-003A", "KATA-003B", "KATA-003C")
        ))
    );

    // TODO Phase 5.1: Carregar de directories KATA-001/, KATA-002/, etc.
    private static final Map<String, KataDelivery> KATAS = Map.ofEntries(
        // --- Virtual Threads ---
        Map.entry("KATA-001A", new KataDelivery(
            "KATA-001A", "virtual-threads",
            "Virtual Threads: Hello Concurrency",
            "# Crie um executor com Virtual Threads\n\nSubstitua um `FixedThreadPool` de 10 threads"
                + " por Virtual Threads e demonstre que 10.000 tarefas concorrentes completam"
                + " sem `OutOfMemoryError`.",
            "https://github.com/fidelisfelipe/skillforge-katas/tree/kata-001a-template",
            80, "intermediate", System.currentTimeMillis()
        )),
        Map.entry("KATA-001B", new KataDelivery(
            "KATA-001B", "virtual-threads",
            "Virtual Threads: Blocking I/O Without Pain",
            "# Simule I/O bloqueante com Virtual Threads\n\nCrie um serviço que faz 1.000 chamadas"
                + " HTTP simuladas (Thread.sleep) em Virtual Threads e meça o throughput"
                + " comparado a platform threads.",
            "https://github.com/fidelisfelipe/skillforge-katas/tree/kata-001b-template",
            100, "intermediate", System.currentTimeMillis()
        )),
        Map.entry("KATA-001C", new KataDelivery(
            "KATA-001C", "virtual-threads",
            "Structured Concurrency with StructuredTaskScope",
            "# Use StructuredTaskScope para fan-out/fan-in\n\nImpl. um agregador que chama"
                + " 3 serviços em paralelo com `ShutdownOnFailure`, cancela os demais"
                + " se um falhar, e retorna o resultado combinado.",
            "https://github.com/fidelisfelipe/skillforge-katas/tree/kata-001c-template",
            120, "advanced", System.currentTimeMillis()
        )),
        // --- Records ---
        Map.entry("KATA-002A", new KataDelivery(
            "KATA-002A", "records",
            "Records: Modelo de Domínio Imutável",
            "# Refatore POJOs mutáveis para Records\n\nConverta `Money`, `Address` e `Order`"
                + " em Java Records. Garanta que `equals`/`hashCode` funcionem corretamente"
                + " em coleções.",
            "https://github.com/fidelisfelipe/skillforge-katas/tree/kata-002a-template",
            80, "beginner", System.currentTimeMillis()
        )),
        Map.entry("KATA-002B", new KataDelivery(
            "KATA-002B", "records",
            "Records: Compact Constructors e Validação",
            "# Adicione validação via compact constructor\n\nCrie um Record `Temperature` que"
                + " aceita apenas valores entre -273.15°C e 1.416e32°C e lança"
                + " `IllegalArgumentException` fora do range.",
            "https://github.com/fidelisfelipe/skillforge-katas/tree/kata-002b-template",
            90, "beginner", System.currentTimeMillis()
        )),
        Map.entry("KATA-002C", new KataDelivery(
            "KATA-002C", "records",
            "Sealed Interfaces + Records: ADT em Java",
            "# Modele uma expressão aritmética como ADT\n\nDefina `sealed interface Expr`"
                + " com implementações `Num`, `Add`, `Mul` como Records. Implemente um"
                + " evaluator recursivo sem instanceof tradicional.",
            "https://github.com/fidelisfelipe/skillforge-katas/tree/kata-002c-template",
            110, "intermediate", System.currentTimeMillis()
        )),
        // --- Pattern Matching ---
        Map.entry("KATA-003A", new KataDelivery(
            "KATA-003A", "pattern-matching",
            "Pattern Matching: instanceof sem Casting",
            "# Elimine todos os casts explícitos\n\nRefatore um método com 10+ blocos"
                + " `if (x instanceof Foo) { Foo f = (Foo) x; }` usando pattern matching"
                + " para instanceof.",
            "https://github.com/fidelisfelipe/skillforge-katas/tree/kata-003a-template",
            90, "intermediate", System.currentTimeMillis()
        )),
        Map.entry("KATA-003B", new KataDelivery(
            "KATA-003B", "pattern-matching",
            "Switch Expressions com Guarded Patterns",
            "# Use guarded patterns para lógica de negócio\n\nImplemente um classificador"
                + " de `Shape` (Circle, Rectangle, Triangle) que retorna área, perímetro"
                + " e categoria usando `switch` com `when` guards.",
            "https://github.com/fidelisfelipe/skillforge-katas/tree/kata-003b-template",
            100, "intermediate", System.currentTimeMillis()
        )),
        Map.entry("KATA-003C", new KataDelivery(
            "KATA-003C", "pattern-matching",
            "Record Deconstruction Patterns",
            "# Destruture Records em switch\n\nDado `sealed interface Event` com Records"
                + " `UserCreated(String id, String email)`, `OrderPlaced(String orderId, double amount)`,"
                + " implemente um handler usando deconstruction patterns.",
            "https://github.com/fidelisfelipe/skillforge-katas/tree/kata-003c-template",
            130, "advanced", System.currentTimeMillis()
        ))
    );

    // Rastreia índice atual por hero+tema (em memória, reinicia com o servidor)
    private final Map<String, Map<String, Integer>> heroProgress = new ConcurrentHashMap<>();

    /**
     * Get all available themes for hero (exclude already completed)
     */
    public List<KataTheme> getAvailableThemes(String heroId) {
        log.debug("Fetching available themes for hero: {}", heroId);
        // TODO: Filter by hero completion status
        return new java.util.ArrayList<>(THEMES.values());
    }

    /**
     * Check if theme exists
     */
    public boolean themeExists(String themeId) {
        return THEMES.containsKey(themeId);
    }

    /**
     * Assign first kata from theme (or next if assignNext=true)
     */
    public KataDelivery assignKata(String heroId, String themeId) {
        return assignKata(heroId, themeId, false);
    }

    /**
     * Assign kata (first or next based on assignNext flag)
     */
    public KataDelivery assignKata(String heroId, String themeId, boolean assignNext) {
        if (themeId == null) {
            log.warn("assignKata called with null themeId for hero: {}", heroId);
            return null;
        }
        KataTheme theme = THEMES.get(themeId);
        if (theme == null) {
            return null;
        }

        List<String> kataIds = theme.kataIds();
        Map<String, Integer> themeProgress = heroProgress.computeIfAbsent(heroId, k -> new ConcurrentHashMap<>());

        int currentIndex = themeProgress.getOrDefault(themeId, 0);
        if (assignNext) {
            currentIndex = (currentIndex + 1) % kataIds.size();
            themeProgress.put(themeId, currentIndex);
        }

        String kataId = kataIds.get(currentIndex);
        KataDelivery kata = KATAS.get(kataId);

        if (kata == null) {
            log.warn("Kata not found: {}", kataId);
            return null;
        }

        log.info("Assigned kata {} (index={}) to hero {} from theme {}", kataId, currentIndex, heroId, themeId);
        return kata;
    }
}
