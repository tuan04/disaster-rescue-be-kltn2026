# Ghi chú thiết kế luồng tạo đội và thêm thành viên

Ngày ghi nhận: 12/10/2026

## Phạm vi đang thảo luận

Hiện tại chỉ tập trung vào:

- Tạo đội cứu hộ.
- Tự động tạo trưởng đội trong danh sách thành viên.
- Thêm thành viên có tài khoản và người không có tài khoản.
- Lời mời và việc thành viên chấp nhận tham gia.
- Quy trình gửi yêu cầu và admin duyệt thành lập đội.

Chưa xử lý trong đợt này:

- Giải tán đội.
- Cập nhật đội nói chung.
- Phân quyền API và bảo mật Gateway.

## Các quyết định đã thống nhất

1. Request tạo đội không chứa `initialMembers`.
2. Không có giai đoạn `DRAFT`. Cứu hộ viên tự tạo đội thì chờ admin duyệt; admin tạo đội thì được duyệt ngay.
3. API tạo đội chỉ tạo `CampaignTeam` và một `CampaignMember` cho trưởng đội.
4. Admin chỉ định trưởng đội qua `CreateTeamByAdminRequest.leaderId`. Cứu hộ viên tự tạo đội thì chính họ là trưởng đội, lấy ID từ `X-User-Id`.
5. Backend tự tạo member của trưởng đội với `roleInTeam = LEADER`; client không thêm leader qua API add member.
6. API add member không được nhận `roleInTeam = LEADER` cho một người khác.
7. Thành viên có tài khoản bắt buộc phải có role hệ thống `RESCUER`.
8. Cho phép nhập người không có tài khoản dưới dạng guest. Người này không nhất thiết sử dụng app và không thể chấp nhận lời mời trong app.
9. Một người có tài khoản chỉ được là thành viên đã chấp nhận của một đội tại một thời điểm, kể cả các đội thuộc chiến dịch khác nhau.
10. Thành viên có tài khoản phải nhận lời mời và tự chấp nhận hoặc từ chối. Trưởng đội là ngoại lệ: được tạo với `ACCEPTED` cả khi tự tạo đội lẫn khi admin chỉ định.
11. Admin duyệt yêu cầu do cứu hộ viên tự tạo dựa trên thông tin đội và trưởng đội; tại thời điểm duyệt chưa có thành viên nào khác ngoài trưởng đội.
12. Nếu yêu cầu thành lập đội bị từ chối, giữ bản ghi đội để xem lịch sử, hủy membership của trưởng đội và cho phép tạo yêu cầu đội mới.

## Luồng được đề xuất

```text
Cứu hộ viên tự tạo -> team PENDING -> admin APPROVE hoặc REJECT
Admin tạo và chỉ định trưởng đội -> team APPROVED ngay
Cả hai luồng -> backend tự tạo leader với membership ACCEPTED
    -> Khi team APPROVED, leader mới được thêm guest và mời các tài khoản RESCUER
    -> Các RESCUER chấp nhận hoặc từ chối
    -> Đội đã duyệt vẫn bắt đầu ở trạng thái vận hành OFFLINE
    -> Khi đủ điều kiện có thể chuyển READY
```

Admin chỉ xét duyệt yêu cầu do cứu hộ viên tự tạo. Đội do admin tạo được `APPROVED` ngay; `reviewedAt` ghi thời điểm tạo và `reviewedBy` ghi ID admin tạo đội. Việc xây dựng đội hình là luồng riêng và chỉ bắt đầu sau khi đội được duyệt. `createTeam()` không xử lý hàng loạt thành viên trong transaction tạo đội.

## Mô hình trạng thái dự kiến

Không trộn trạng thái xét duyệt với trạng thái vận hành.

```java
enum TeamApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED
}

enum TeamStatus {
    OFFLINE,
    READY,
    BUSY,
    DISBANDED
}

enum MembershipStatus {
    INVITED,
    ACCEPTED,
    DECLINED,
    CANCELLED
}

enum MembershipType {
    REGISTERED,
    GUEST
}
```

Tên enum và tên giá trị có thể điều chỉnh khi triển khai, nhưng phải giữ việc tách trạng thái duyệt khỏi trạng thái vận hành.

## Quy tắc thêm thành viên

### Thành viên có tài khoản

- `memberId` bắt buộc có giá trị.
- Tra cứu user qua gRPC.
- User phải tồn tại và có role `RESCUER`.
- Không dùng tên và số điện thoại do client gửi; lấy snapshot từ user-service.
- Tạo membership ở trạng thái `INVITED`.
- Khi user chấp nhận, kiểm tra lại trong transaction rằng user chưa có membership `ACCEPTED` ở đội khác.
- Sau khi chấp nhận thành công, nên hủy hoặc làm hết hạn các lời mời còn lại của user.

### Guest member

- `memberId = null`.
- Bắt buộc nhập họ tên và thông tin liên hệ phù hợp; cần thống nhất việc số điện thoại có bắt buộc hay không.
- Không có luồng chấp nhận trên app.
- Có thể tạo trực tiếp với `MembershipType.GUEST` và `MembershipStatus.ACCEPTED`, hiểu là trưởng đội chịu trách nhiệm khai báo.
- Guest không thể áp dụng chắc chắn quy tắc một người chỉ thuộc một đội nếu không có định danh ổn định.

### Trưởng đội

- Không nằm trong request add member.
- Được backend tự tạo khi tạo đội.
- Có `roleInTeam = LEADER` và `membershipStatus = ACCEPTED`.
- API add member phải từ chối `TeamRole.LEADER`.

## Những điểm code hiện tại cần thay đổi

### Request tạo đội

- `CreateTeamByLeaderRequest` không có `leaderId` hoặc `initialMembers`.
- `CreateTeamByAdminRequest` bắt buộc có `leaderId`, không có `initialMembers`.

### `CampaignTeamService.createTeam`

- Không gọi `initTeamMembers(..., initialMembers)` nữa.
- Sau khi lưu team, chỉ gọi một hàm chuyên tạo leader member.
- Team do cứu hộ viên tự tạo bắt đầu ở `PENDING`; team do admin tạo bắt đầu ở `APPROVED`.

### `CampaignMemberService`

- Đổi `initTeamMembers` thành hàm chỉ tạo leader, ví dụ `createLeaderMember`.
- Với registered member, kiểm tra role `RESCUER` qua kết quả gRPC.
- Không lưu registered member là thành viên chính thức ngay; tạo lời mời `INVITED`.
- Từ chối request add member có `roleInTeam = LEADER`.
- Thêm nghiệp vụ accept/decline invitation.
- Thay kiểm tra hiện tại dựa trên team chưa `DISBANDED` bằng kiểm tra membership `ACCEPTED`.
- Cần bảo vệ trường hợp hai lời mời được chấp nhận đồng thời bằng transaction và ràng buộc dữ liệu phù hợp.

### `CampaignMember`

- Bổ sung `membershipStatus`.
- Bổ sung `membershipType` hoặc một cách xác định rõ registered/guest.
- Cân nhắc các trường `invitedAt`, `respondedAt`, `invitedBy`.
- Hiện entity bắt buộc `phone`, trong khi DTO cho guest có thể không truyền phone; cần thống nhất quy tắc.

### `CampaignTeam`

- Bổ sung trạng thái duyệt độc lập, cùng dữ liệu audit cần thiết như `requestedAt`, `reviewedAt`, `reviewedBy`, `rejectionReason`.

### API dự kiến

```text
POST /api/v1/campaign-teams
POST /api/v1/campaign-teams/admin
POST /api/v1/teams/{teamId}/members
POST /api/v1/teams/{teamId}/members/{membershipId}/accept
POST /api/v1/teams/{teamId}/members/{membershipId}/decline
POST /api/v1/campaign-teams/{teamId}/approve
POST /api/v1/campaign-teams/{teamId}/reject
```

Đường dẫn cụ thể sẽ được rà lại theo convention hiện có trước khi code.

## Các điểm cần chốt khi tiếp tục

1. Guest có bắt buộc số điện thoại không? Nếu không, phải cho phép `CampaignMember.phone` nullable hoặc dùng thông tin liên hệ khác.
2. Có yêu cầu số thành viên `ACCEPTED` tối thiểu trước khi chuyển `READY` không?
3. Đội đang `PENDING` không được thêm thành viên; cần xác nhận đây có phải quy tắc cuối cùng không.
4. Guest được tính như thành viên chính thức khi xét điều kiện chuyển `READY` hay chỉ là người đi cùng?
5. Lời mời có thời hạn hay không?

## Tiến độ triển khai

- [x] Bỏ `initialMembers` khỏi request; tạo đội chỉ lưu team và trưởng đội.
- [x] Tách hàm `createLeaderMember` để tự tạo trưởng đội.
- [x] Thêm enum và trường dữ liệu cho trạng thái duyệt đội, loại/trạng thái thành viên.
- [x] Thêm API admin approve/reject cho yêu cầu thành lập đội; chỉ chuyển từ `PENDING`, lưu người và thời điểm duyệt, cùng lý do nếu từ chối.
- [x] Sau khi từ chối, giữ lịch sử đội, hủy membership của trưởng đội và cho phép người đó tạo yêu cầu mới.
- [x] Tách request/API tạo đội của admin và cứu hộ viên; dùng chung service, admin tạo `APPROVED`, cứu hộ viên tạo `PENDING`.
- [ ] Sửa logic add member: chỉ cho phép sau khi đội được duyệt; registered user phải có role `RESCUER` và được tạo lời mời `INVITED`; từ chối thêm `LEADER`.
- [ ] Thêm accept/decline invitation và bảo vệ quy tắc một người chỉ có membership `ACCEPTED` ở một đội tại một thời điểm.
- [ ] Kiểm tra các chuyển trạng thái và tình huống chấp nhận đồng thời.

Không triển khai migration trong phạm vi này.
