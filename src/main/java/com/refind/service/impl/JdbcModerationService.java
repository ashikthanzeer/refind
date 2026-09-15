package com.refind.service.impl;
import com.refind.dao.*; import com.refind.exception.ValidationException; import com.refind.model.*; import com.refind.model.enums.*; import com.refind.service.ModerationService; import java.time.LocalDateTime; import java.util.*;
public class JdbcModerationService implements ModerationService {
 private final ClaimDAO claims; private final ModerationDAO moderation; private final ItemDAO items; private final NotificationDAO notifications;
 public JdbcModerationService(ClaimDAO claims, ModerationDAO moderation, ItemDAO items, NotificationDAO notifications){this.claims=claims;this.moderation=moderation;this.items=items;this.notifications=notifications;}
 public List<Claim> getPendingClaims(){return claims.findByStatus(ClaimStatus.PENDING);}
 public Claim approveClaim(Long claimId,Long moderatorId){return decide(claimId,moderatorId,ModerationDecision.APPROVED);}
 public Claim rejectClaim(Long claimId,Long moderatorId){return decide(claimId,moderatorId,ModerationDecision.REJECTED);}
 private Claim decide(Long id,Long moderatorId,ModerationDecision d){Claim c=claims.findById(id).orElseThrow(()->new ValidationException("Claim not found: "+id));if(c.getStatus()!=ClaimStatus.PENDING)throw new ValidationException("Claim is already decided.");User m=new User();m.setId(moderatorId);Moderation x=new Moderation(c,m,d,null,LocalDateTime.now());moderation.save(x);c.setStatus(d==ModerationDecision.APPROVED?ClaimStatus.APPROVED:ClaimStatus.REJECTED);c.setDecidedAt(LocalDateTime.now());Claim updated=claims.update(c);if(d==ModerationDecision.APPROVED){Item i=items.findById(c.getItem().getId()).orElseThrow(()->new ValidationException("Claimed item not found."));i.setStatus(ItemStatus.CLAIMED);items.update(i);}User claimant=c.getClaimant();Notification n=new Notification(claimant,"Your claim #"+id+" was "+d.name().toLowerCase()+".",false,LocalDateTime.now());notifications.save(n);return updated;}
}
