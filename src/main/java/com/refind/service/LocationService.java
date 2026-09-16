package com.refind.service;

import com.refind.model.Location;

import java.util.List;
import java.util.Optional;

public interface LocationService {
    Location createLocation(Location location);
    Optional<Location> getLocationById(Long id);
    List<Location> getAllLocations();
    Location updateLocation(Location location);
    boolean deleteLocation(Long id);
}
