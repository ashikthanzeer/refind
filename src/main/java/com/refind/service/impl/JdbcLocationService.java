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

    public Location createLocation(Location location) {
        if (location == null || location.getCampus() == null || location.getCampus().isBlank()) {
            throw new ValidationException("Location campus is required.");
        }
        return dao.save(location);
    }

    public Optional<Location> getLocationById(Long id) {
        return dao.findById(id);
    }

    public List<Location> getAllLocations() {
        return dao.findAll();
    }

    public Location updateLocation(Location location) {
        if (location == null || location.getId() == null) {
            throw new ValidationException("Location id is required for update.");
        }
        if (location.getCampus() == null || location.getCampus().isBlank()) {
            throw new ValidationException("Location campus is required.");
        }
        return dao.update(location);
    }

    public boolean deleteLocation(Long id) {
        return dao.deleteById(id);
    }
}
