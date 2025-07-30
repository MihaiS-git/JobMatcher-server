package com.jobmatcher.server.controller;

import com.jobmatcher.server.model.SkillDTO;
import com.jobmatcher.server.service.ISkillService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.jobmatcher.server.model.ApiConstants.API_VERSION;

@RestController
@RequestMapping(path = API_VERSION + "/skills")
public class SkillController {

    private final ISkillService skillService;

    public SkillController(ISkillService skillService) {
        this.skillService = skillService;
    }

    @GetMapping
    public ResponseEntity<List<SkillDTO>> getAllSkills() {
        List<SkillDTO> skills = skillService.getAllSkills();
        return ResponseEntity.ok(skills);
    }

}
