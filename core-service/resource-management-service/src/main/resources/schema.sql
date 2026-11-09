-- Đảm bảo mỗi Leader chỉ có tối đa 1 đội đang chờ duyệt (PENDING) hoặc đang hoạt động
-- Cho phép tạo đội mới nếu đội cũ đã giải tán (DISBANDED) hoặc bị từ chối (REJECTED)
CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_active_team_per_leader 
ON campaign_teams (leader_id) 
WHERE status <> 'DISBANDED' AND approval_status <> 'REJECTED';