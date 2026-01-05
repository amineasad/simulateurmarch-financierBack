package tn.esprit.examen.nomPrenomClasseExamen.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.examen.nomPrenomClasseExamen.dto.BotTemplateDTO;
import tn.esprit.examen.nomPrenomClasseExamen.entities.BotTemplate;
import tn.esprit.examen.nomPrenomClasseExamen.entities.StrategyType;
import tn.esprit.examen.nomPrenomClasseExamen.repositories.BotTemplateRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BotMarketplaceService {

    private final BotTemplateRepository botTemplateRepository;

    public List<BotTemplateDTO> getAllTemplates() {
        return botTemplateRepository.findAll().stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    public List<BotTemplateDTO> getTemplatesByStrategy(StrategyType strategyType) {
        return botTemplateRepository.findByStrategyType(strategyType).stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }
    
    public BotTemplateDTO getTemplate(Long id) {
        BotTemplate template = botTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));
        return mapEntityToDto(template);
    }
    
    public BotTemplateDTO createTemplate(BotTemplateDTO dto) {
        BotTemplate template = new BotTemplate();
        template.setName(dto.getName());
        template.setDescription(dto.getDescription());
        template.setStrategyType(dto.getStrategyType());
        template.setDefaultConfiguration(dto.getDefaultConfiguration());
        template.setHistoricalReturns(dto.getHistoricalReturns());
        template.setWinRate(dto.getWinRate());
        template.setDrawdown(dto.getDrawdown());
        template.setSharpeRatio(dto.getSharpeRatio());
        template.setPopularity(0);
        
        BotTemplate saved = botTemplateRepository.save(template);
        return mapEntityToDto(saved);
    }

    private BotTemplateDTO mapEntityToDto(BotTemplate entity) {
        BotTemplateDTO dto = new BotTemplateDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setStrategyType(entity.getStrategyType());
        dto.setDefaultConfiguration(entity.getDefaultConfiguration());
        dto.setHistoricalReturns(entity.getHistoricalReturns());
        dto.setWinRate(entity.getWinRate());
        dto.setDrawdown(entity.getDrawdown());
        dto.setSharpeRatio(entity.getSharpeRatio());
        dto.setPopularity(entity.getPopularity());
        return dto;
    }
}
