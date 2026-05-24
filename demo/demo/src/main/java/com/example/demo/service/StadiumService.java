package com.example.demo.service;

import com.example.demo.controller.StadiumController;
import com.example.demo.dao.StadiumRepository;
import com.example.demo.entity.Stadium;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StadiumService {

    private final StadiumRepository stadiumRepository;

    @Autowired
    public StadiumService(StadiumRepository stadiumRepository) {
        this.stadiumRepository = stadiumRepository;
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
        return toStadiumResponse(stadiumRepository.save(stadium));
    }

    public StadiumController.StadiumResponse update(Long id, StadiumController.StadiumRequest request) {
        Stadium stadium = findStadiumEntity(id);
        applyRequest(stadium, request);
        return toStadiumResponse(stadiumRepository.save(stadium));
    }

    public void delete(Long id) {
        if (!stadiumRepository.existsById(id)) {
            throw new ResourceNotFoundException("Stadium not found with id = " + id);
        }
        stadiumRepository.deleteById(id);
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
