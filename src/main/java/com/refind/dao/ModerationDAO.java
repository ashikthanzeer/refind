package com.refind.dao;

import com.refind.model.Moderation;
import java.util.List;
import java.util.Optional;

public interface ModerationDAO {
    Moderation save(Moderation moderation);
    Optional<Moderation> findById(Long id);
    Optional<Moderation> findByClaim(Long claimId);
    List<Moderation> findAll();
}
