package com.jobmatcher.server.controller;

import com.jobmatcher.server.model.LanguageDTO;
import com.jobmatcher.server.service.ILanguageService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

import static com.jobmatcher.server.model.ApiConstants.API_VERSION;

@RestController
@RequestMapping(path = API_VERSION + "/languages")
public class LanguageController {

    private final ILanguageService languageService;

    public LanguageController(ILanguageService languageService) {
        this.languageService = languageService;
    }

    @GetMapping(path = "{id}")
    public ResponseEntity<LanguageDTO> getLanguageById(@PathVariable Integer id){
        LanguageDTO language = languageService.findLanguageById(id);
        return ResponseEntity.ok(language);
    }

    @GetMapping(path = "/name/{name}")
    public ResponseEntity<LanguageDTO> getLanguageByName(@PathVariable String name){
        LanguageDTO language = languageService.findLanguageByName(name);
        return ResponseEntity.ok(language);
    }

    @GetMapping
    public ResponseEntity<List<LanguageDTO>> getAllLanguages(){
        List<LanguageDTO> languages = languageService.getAllLanguages();
        return ResponseEntity.ok(languages);
    }

    @PostMapping
    public ResponseEntity<LanguageDTO> saveLanguage(@Valid @RequestBody LanguageDTO language){
        LanguageDTO savedLanguage = languageService.saveLanguage(language);
        URI location = URI.create(API_VERSION + "/languages/" + savedLanguage.getId());
        return ResponseEntity.created(location).body(savedLanguage);
    }
}
