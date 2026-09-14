package com.refind.dao;
import com.refind.model.Location; import java.util.*;
public interface LocationDAO { Location save(Location location); Optional<Location> findById(Long id); List<Location> findAll(); Location update(Location location); boolean deleteById(Long id); }
