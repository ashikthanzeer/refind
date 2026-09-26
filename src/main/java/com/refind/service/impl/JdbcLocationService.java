package com.refind.service.impl;

import com.refind.dao.LocationDAO;
import com.refind.exception.ValidationException;
import com.refind.model.Location;
import com.refind.service.LocationService;

import java.util.List;
import java.util.Optional;

public class JdbcLocationService implements LocationService {

    private final LocationDAO dao;

    public JdbcLocationService(LocationDAO dao) {
        this.dao = dao;
    }

    @Override
    public Location createLocation(String campus, String building, String room) {
        if (campus == null || campus.isBlank()) {
            throw new ValidationException("Campus is required for location.");
        }
        Location location = new Location(campus.trim(), building != null ? building.trim() : null, room != null ? room.trim() : null);
        return dao.save(location);
    }

    @Override
    public Optional<Location> getLocationById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return dao.findById(id);
    }

    @Override
    public List<Location> getAllLocations() {
        return dao.findAll();
    }

    @Override
    public Location updateLocation(Location location) {
        if (location == null || location.getId() == null) {
            throw new ValidationException("Location ID is required for update.");
        }
        if (location.getCampus() == null || location.getCampus().isBlank()) {
            throw new ValidationException("Campus is required.");
        }
        return dao.update(location);
    }

    @Override
    public boolean deleteLocation(Long id) {
        if (id == null) {
            throw new ValidationException("Location ID is required for deletion.");
        }
        return dao.deleteById(id);
    }
}
