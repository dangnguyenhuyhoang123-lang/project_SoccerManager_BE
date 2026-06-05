package com.example.demo.service;

import com.example.demo.controller.StadiumController;
import com.example.demo.dao.StadiumRepository;
import com.example.demo.dto.RealtimeEventDTO;
import com.example.demo.entity.Stadium;
import com.example.demo.entity.team.Team;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class StadiumService {

    private final StadiumRepository stadiumRepository;
    private final RealtimeEventService realtimeEventService;

    @Autowired
    public StadiumService(
            StadiumRepository stadiumRepository,
            RealtimeEventService realtimeEventService
    ) {
        this.stadiumRepository = stadiumRepository;
        this.realtimeEventService = realtimeEventService;
    }

    public List<StadiumController.StadiumResponse> getStadiums(String search) {
        List<Stadium> stadiums = (search == null || search.isBlank())
                ? stadiumRepository.findAll()
                : stadiumRepository.findByNameContainingIgnoreCase(search);

        return stadiums.stream()
                .map(this::toStadiumResponse)
                .toList();
    }

    public StadiumController.StadiumResponse getStadium(Long id) {
        return toStadiumResponse(findStadiumEntity(id));
    }

    public StadiumController.StadiumResponse create(StadiumController.StadiumRequest request) {
        Stadium stadium = new Stadium();
        applyRequest(stadium, request);
        Stadium saved = stadiumRepository.save(stadium);
        sendStadiumRealtimeEvent(
                saved,
                saved.getId() != null ? saved.getId().longValue() : null,
                "STADIUM_CREATED"
        );
        return toStadiumResponse(saved);
    }

    public StadiumController.StadiumResponse update(Long id, StadiumController.StadiumRequest request) {
        Stadium stadium = findStadiumEntity(id);
        applyRequest(stadium, request);
        Stadium saved = stadiumRepository.save(stadium);
        sendStadiumRealtimeEvent(
                saved,
                saved.getId() != null ? saved.getId().longValue() : id,
                "STADIUM_UPDATED"
        );
        return toStadiumResponse(saved);
    }

    public void delete(Long id) {
        Stadium stadium = findStadiumEntity(id);

        stadiumRepository.deleteById(id);

        sendStadiumRealtimeEvent(
                stadium,
                id,
                "STADIUM_DELETED"
        );
    }

    private Stadium findStadiumEntity(Long id) {
        return stadiumRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stadium not found with id = " + id));
    }

    private void applyRequest(Stadium stadium, StadiumController.StadiumRequest request) {
        stadium.setName(request.name());
        stadium.setAddress(request.address());
        stadium.setCapacity(request.capacity());
        stadium.setGrass(request.grass());
    }

    private void sendStadiumRealtimeEvent(Stadium stadium, Long stadiumId, String type) {
        RealtimeEventDTO event = realtimeEvent(
                type,
                stadiumId,
                "STADIUM",
                "REFETCH_STADIUMS"
        );

        realtimeEventService.sendToAdmins(event);

        if (stadium != null && stadium.getHomeTeams() != null) {
            stadium.getHomeTeams()
                    .stream()
                    .filter(Objects::nonNull)
                    .map(Team::getId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .forEach(teamId -> realtimeEventService.sendToClubManagerByTeamId(teamId, event));
        }
    }

    private RealtimeEventDTO realtimeEvent(
            String type,
            Long referenceId,
            String referenceType,
            String action
    ) {
        return new RealtimeEventDTO(
                type,
                referenceId,
                referenceType,
                action,
                null,
                LocalDateTime.now()
        );
    }

    private StadiumController.StadiumResponse toStadiumResponse(Stadium stadium) {
        return new StadiumController.StadiumResponse(
                stadium.getId() != null ? stadium.getId().longValue() : null,
                stadium.getName(),
                stadium.getAddress(),
                stadium.getCapacity(),
                stadium.getGrass()
        );
    }
}
