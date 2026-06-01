package com.skillforge.template.dojo;

import com.skillforge.dojo.message.KataThemeResponseMessage.KataDelivery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Gerenciar armazenamento local de katas
 *
 * Salva:
 * - Metadata de kata aceito
 * - Instruções
 * - Path para template
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KataLocalService {

    private final HeroProperties heroProperties;

    /**
     * Salvar kata aceito localmente
     */
    public void saveAcceptedKata(KataDelivery kata) throws IOException {
        Path dojoPath = Paths.get(heroProperties.getLocalStorageBasePath())
            .resolve("dojo")
            .resolve(kata.kataId());

        Files.createDirectories(dojoPath);

        // Salvar metadata
        Path metaFile = dojoPath.resolve("KATA.json");
        String kataJson = String.format(
            "{\"kataId\": \"%s\", \"title\": \"%s\", \"difficulty\": \"%s\", \"xpReward\": %d}",
            kata.kataId(), kata.title(), kata.difficulty(), kata.xpReward()
        );
        Files.writeString(metaFile, kataJson);

        // Salvar spec
        Path specFile = dojoPath.resolve("SPEC.md");
        Files.writeString(specFile, kata.spec());

        log.info("💾 Kata saved locally: {}", dojoPath);
    }

    /**
     * Verificar se kata está aceito
     */
    public boolean isKataAccepted(String kataId) {
        Path dojoPath = Paths.get(heroProperties.getLocalStorageBasePath())
            .resolve("dojo")
            .resolve(kataId);
        return Files.exists(dojoPath);
    }

    /**
     * Get path onde hero deve implementar
     */
    public Path getImplementationPath(String kataId) {
        return Paths.get(heroProperties.getLocalStorageBasePath())
            .resolve("dojo")
            .resolve(kataId);
    }
}
