package com.jobmatcher.server.bootstrap;

import com.jobmatcher.server.domain.Language;
import com.jobmatcher.server.repository.LanguageRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(2)
public class LanguagesDataSeeder implements ApplicationRunner {

    private final LanguageRepository languageRepository;

    public LanguagesDataSeeder(LanguageRepository languageRepository) {
        this.languageRepository = languageRepository;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if(languageRepository.count() > 0) return;

        System.out.println("Seeding languages...");
        languageRepository.saveAll(List.of(
                new Language("English"),
                new Language("French"),
                new Language("Spanish"),
                new Language("German"),
                new Language("Chinese"),
                new Language("Japanese"),
                new Language("Russian"),
                new Language("Portuguese"),
                new Language("Italian"),
                new Language("Arabic"),
                new Language("Hindi"),
                new Language("Korean"),
                new Language("Dutch"),
                new Language("Turkish"),
                new Language("Swedish"),
                new Language("Polish"),
                new Language("Vietnamese"),
                new Language("Thai"),
                new Language("Indonesian"),
                new Language("Greek"),
                new Language("Hebrew"),
                new Language("Norwegian"),
                new Language("Danish"),
                new Language("Finnish"),
                new Language("Czech"),
                new Language("Hungarian"),
                new Language("Romanian"),
                new Language("Ukrainian"),
                new Language("Catalan"),
                new Language("Bengali"),
                new Language("Malay"),
                new Language("Persian"),
                new Language("Swahili"),
                new Language("Bulgarian"),
                new Language("Serbian"),
                new Language("Croatian"),
                new Language("Slovak"),
                new Language("Lithuanian"),
                new Language("Latvian"),
                new Language("Estonian"),
                new Language("Icelandic"),
                new Language("Mongolian"),
                new Language("Albanian"),
                new Language("Filipino"),
                new Language("Basque"),
                new Language("Galician"),
                new Language("Irish"),
                new Language("Welsh"),
                new Language("Afrikaans"),
                new Language("Tamil"),
                new Language("Telugu"),
                new Language("Marathi"),
                new Language("Kannada"),
                new Language("Gujarati")
        ));
        System.out.println("Languages seeded.");
        System.out.println("-------------------------------------");

    }
}
