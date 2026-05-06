-- =====================================================================
-- V6: Moderator feedback for rejected reviews — gives the user a reason
-- to read so they can fix the review and resubmit (mirrors the workflow
-- already in place for collections).
-- =====================================================================

ALTER TABLE reviews
    ADD COLUMN moderator_comment TEXT;
