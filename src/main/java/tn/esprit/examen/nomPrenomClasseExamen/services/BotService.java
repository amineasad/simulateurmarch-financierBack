package tn.esprit.examen.nomPrenomClasseExamen.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.examen.nomPrenomClasseExamen.dto.BotDTO;
import tn.esprit.examen.nomPrenomClasseExamen.entities.Bot;
import tn.esprit.examen.nomPrenomClasseExamen.entities.BotStatus;
import tn.esprit.examen.nomPrenomClasseExamen.entities.User;
import tn.esprit.examen.nomPrenomClasseExamen.repositories.BotRepository;
import tn.esprit.examen.nomPrenomClasseExamen.repositories.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BotService {

    private final BotRepository botRepository;
    private final UserRepository userRepository;

    public BotDTO createBot(BotDTO botDTO) {
        Bot bot = new Bot();
        mapDtoToEntity(botDTO, bot);
        bot.setStatus(BotStatus.STOPPED);
        
        if (botDTO.getUserId() != null) {
            User user = userRepository.findById(botDTO.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            bot.setUser(user);
        }

        Bot savedBot = botRepository.save(bot);
        return mapEntityToDto(savedBot);
    }

    public BotDTO updateBot(Long id, BotDTO botDTO) {
        Bot bot = botRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bot not found"));
        
        // Update fields
        bot.setName(botDTO.getName());
        bot.setDescription(botDTO.getDescription());
        bot.setConfiguration(botDTO.getConfiguration());
        bot.setInvestmentAmount(botDTO.getInvestmentAmount());
        bot.setTradingPair(botDTO.getTradingPair());
        bot.setMaxPositionSize(botDTO.getMaxPositionSize());
        bot.setStopLossPercentage(botDTO.getStopLossPercentage());
        bot.setTakeProfitPercentage(botDTO.getTakeProfitPercentage());
        
        if (botDTO.getStrategyType() != null) {
            bot.setStrategyType(botDTO.getStrategyType());
        }

        Bot savedBot = botRepository.save(bot);
        return mapEntityToDto(savedBot);
    }

    public BotDTO getBot(Long id) {
        Bot bot = botRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bot not found"));
        return mapEntityToDto(bot);
    }

    public List<BotDTO> getUserBots(Long userId) {
        return botRepository.findByUserId(userId).stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }
    
    public void deleteBot(Long id) {
        botRepository.deleteById(id);
    }

    public BotDTO updateBotStatus(Long id, BotStatus status) {
        Bot bot = botRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bot not found"));
        bot.setStatus(status);
        Bot savedBot = botRepository.save(bot);
        return mapEntityToDto(savedBot);
    }

    private void mapDtoToEntity(BotDTO dto, Bot entity) {
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setStrategyType(dto.getStrategyType());
        entity.setConfiguration(dto.getConfiguration());
        entity.setInvestmentAmount(dto.getInvestmentAmount());
        entity.setTradingPair(dto.getTradingPair());
        entity.setMaxPositionSize(dto.getMaxPositionSize());
        entity.setStopLossPercentage(dto.getStopLossPercentage());
        entity.setTakeProfitPercentage(dto.getTakeProfitPercentage());
    }

    private BotDTO mapEntityToDto(Bot entity) {
        BotDTO dto = new BotDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setStrategyType(entity.getStrategyType());
        dto.setStatus(entity.getStatus());
        dto.setConfiguration(entity.getConfiguration());
        dto.setInvestmentAmount(entity.getInvestmentAmount());
        dto.setTradingPair(entity.getTradingPair());
        dto.setMaxPositionSize(entity.getMaxPositionSize());
        dto.setStopLossPercentage(entity.getStopLossPercentage());
        dto.setTakeProfitPercentage(entity.getTakeProfitPercentage());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        if (entity.getUser() != null) {
            dto.setUserId(entity.getUser().getId());
        }
        return dto;
    }
}
