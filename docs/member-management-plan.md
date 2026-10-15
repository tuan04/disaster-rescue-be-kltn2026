# KẾ HOẠCH TRIỂN KHAI VÀ TỐI ƯU HÓA CHỨC NĂNG QUẢN LÝ THÀNH VIÊN ĐỘI CỨU HỘ (CAMPAIGN MEMBER)

**Ngày cập nhật:** 14/10/2026  
**Dịch vụ phụ trách:** `resource-management-service`  
**Dịch vụ phối hợp:** `user-service` (qua gRPC)  
**Tài liệu tham chiếu:** `docs/team-creation-and-member-flow-notes.md`

---

## I. MỤC TIÊU & PHẠM VI

### 1. Mục tiêu
- Hoàn thiện nghiệp vụ xây dựng đội hình sau khi Đội cứu hộ đã được Admin phê duyệt (`APPROVED`).
- Phân biệt rõ ràng giữa **Cứu hộ viên có tài khoản (`REGISTERED`)** và **Người đi cùng (`GUEST`)**.
- Triển khai vòng đời Lời mời tham gia đội: `INVITED` $\rightarrow$ `ACCEPTED` hoặc `DECLINED`.
- Đảm bảo tính toàn vẹn dữ liệu: **Một Cứu hộ viên tại một thời điểm chỉ có tối đa 1 tư cách thành viên chính thức (`ACCEPTED`) ở các đội đang hoạt động trên toàn bộ hệ thống.**
- Quy định rõ ràng trạng thái thành viên: Khi Đội giải tán (`team.status = DISBANDED`), thành viên **giữ nguyên `ACCEPTED`** để lưu vết lịch sử; khi bị loại khỏi đội (`removeMember`), trạng thái chuyển sang **`CANCELLED`** (đảm bảo cả hai trường hợp đều được tự do tham gia đội khác trong tương lai).
- Tối ưu hóa triệt để hiệu năng: Loại bỏ N+1 query, gọi gRPC ngoài Database Transaction.

### 2. Phạm vi
- Quản lý danh sách thành viên đội cứu hộ (`campaign_members`).
- API thêm/mời thành viên hàng loạt (Batch Add/Invite).
- API phản hồi lời mời dành cho Cứu hộ viên (Accept / Decline).
- API xóa thành viên và các ràng buộc khi đội đang làm nhiệm vụ.

---

## II. PHÂN TÍCH HIỆN TRẠNG & CÁC ĐIỂM CẦN XỬ LÝ

| STT | Vấn đề hiện tại | Hậu quả | Giải pháp xử lý |
| :---: | :--- | :--- | :--- |
| **1** | Đội `PENDING` hoặc `REJECTED` vẫn add được member | Leader thêm người khi đội chưa được cấp phép hoạt động | Chặn nếu `team.approvalStatus != APPROVED` hoặc Campaign không `ACTIVE` |
| **2** | Không chặn gán vai trò `LEADER` trong API add member | Có thể tạo ra nhiều Leader trong 1 đội | Từ chối request nếu có `roleInTeam == LEADER` |
| **3** | Chưa kiểm tra vai trò hệ thống `RESCUER` của User | Tài khoản người dân (`CITIZEN`) bị thêm vào đội cứu hộ | Kiểm tra `userInfo.getRole() == "RESCUER"` |
| **4** | Mọi member bị lưu thẳng thành `ACCEPTED` | Cứu hộ viên bị "khóa cứng" vào đội khi chưa đồng ý | Cứu hộ viên có tài khoản tạo ở dạng `INVITED`, cần bấm Accept |
| **5** | Ràng buộc SĐT của Guest Member | Guest không nhập SĐT có thể gây thiếu thông tin liên lạc khẩn cấp | **Bắt buộc nhập số điện thoại hợp lệ cho Guest**, giữ cột `phone` NOT NULL |
| **6** | Lặp $2 \times N$ queries kiểm tra trong `addMembers` | Hiệu năng kém khi thêm nhiều thành viên (N+1 query) | Gom kiểm tra hàng loạt bằng `IN (:memberIds)` trong 1 câu SQL |
| **7** | gRPC `getUsersByIds` nằm trong `@Transactional` | Giữ kết nối DB Connection Pool trong lúc chờ mạng | Gọi gRPC và validate dữ liệu trước khi mở DB Transaction |
| **8** | Nguy cơ Race condition khi Rescuer Accept lời mời | Rescuer có thể bấm Accept song song vào 2 đội khác nhau | Sử dụng Redis Distributed Lock theo `userId` khi Accept lời mời |

---

## III. THIẾT KẾ CHI TIẾT

### 1. Phân loại Thành viên & Vòng đời Lời mời

```text
[Leader gửi danh sách Add Members]
            │
            ├── Nếu memberId == null (GUEST):
            │     ├── Bắt buộc có: fullName, phone (SĐT liên hệ khẩn cấp)
            │     └── Lưu thẳng: membershipType = GUEST, membershipStatus = ACCEPTED
            │
            └── Nếu memberId != null (REGISTERED):
                  ├── Kiểm tra Role gRPC == "RESCUER"
                  ├── Kiểm tra chưa ACCEPTED ở đội đang hoạt động khác
                  └── Lưu: membershipType = REGISTERED, membershipStatus = INVITED
                                │
                                ├── User gọi API Accept ──> Chuyển ACCEPTED (Tự hủy các lời mời ở đội khác)
                                └── User gọi API Decline ──> Chuyển DECLINED
```

#### Quy tắc nghiệp vụ chi tiết:
1. **Đội trưởng (LEADER):**
   - Tự động sinh khi tạo đội, `membershipType = REGISTERED`, `membershipStatus = ACCEPTED`.
   - Không thể bị xóa qua API xóa member.
2. **Thành viên có tài khoản (REGISTERED):**
   - Bắt buộc có `memberId` hợp lệ.
   - Họ tên, SĐT, Giới tính, Avatar lấy snapshot trực tiếp từ `user-service` qua gRPC.
   - Bắt đầu ở trạng thái `INVITED`.
   - Được nhận lời mời từ nhiều đội, nhưng chỉ được `ACCEPT` duy nhất 1 đội tại một thời điểm.
3. **Thành viên đi cùng (GUEST):**
   - `memberId = null`.
   - **Bắt buộc nhập họ tên (`fullName`) và số điện thoại (`phone`)** để phục vụ liên lạc khẩn cấp ngoài thực địa.
   - Vào thẳng trạng thái `ACCEPTED`.

---

### 2. Trạng thái Thành viên khi Đội Giải tán hoặc Bị loại khỏi Đội

#### A. Khi Đội Cứu hộ Giải tán (`disbandTeam`)
- **Trạng thái Đội:** `team.status` chuyển sang `DISBANDED`.
- **Trạng thái Member:** Các bản ghi `CampaignMember` của đội **vẫn giữ nguyên `ACCEPTED`** nhằm lưu giữ lịch sử: *"Họ đã từng là thành viên chính thức tham gia chiến dịch cứu nạn này"*.
- **Quyền tham gia đội mới:** 
  - Điều kiện kiểm tra thành viên hợp lệ trong hệ thống là:
    ```sql
    WHERE cm.memberId = :memberId 
      AND cm.membershipStatus = 'ACCEPTED' 
      AND cm.campaignTeam.status <> 'DISBANDED'
    ```
  - Vì đội cũ đã là `DISBANDED`, nên **tất cả thành viên và Đội trưởng của đội giải tán ĐỀU ĐƯỢC TỰ DO tham gia đội khác hoặc tự tạo đội mới ngay lập tức**.

#### B. Khi Thành viên bị loại / đá khỏi Đội (`removeMember`)
- **Ràng buộc an toàn:** Không được phép loại thành viên khi đội đang làm nhiệm vụ cứu nạn (`team.status == BUSY`).
- **Trạng thái Member sau khi loại:**
  - Thay vì xóa cứng (`delete`), chuyển `membershipStatus = CANCELLED` (Bị hủy tư cách thành viên).
  - Bản ghi vẫn còn trong DB để phục vụ kiểm toán (audit log).
- **Quyền tham gia đội mới:**
  - Do trạng thái không còn là `ACCEPTED`, Cứu hộ viên bị loại **HOÀN TOÀN CÓ THỂ được mời và tham gia một đội cứu hộ khác trong tương lai**.

#### C. Khi Thành viên tham gia lại (Rejoining Flow)
- **Nếu tham gia một ĐỘI KHÁC:**
  - Hệ thống **tạo mới một bản ghi `CampaignMember`** độc lập ở đội mới.
  - Bản ghi cũ ở đội trước vẫn giữ `CANCELLED` để lưu vết lịch sử.
- **Nếu được mời lại vào CHÍNH ĐỘI CŨ:**
  - Database có ràng buộc `uq_campaign_team_member(campaign_team_id, member_id)`.
  - Thay vì cố `INSERT` dòng mới gây lỗi vi phạm Unique Constraint, hệ thống sẽ **TÁI KÍCH HOẠT (Reactivate)** bản ghi cũ:
    - Chuyển `membershipStatus` từ `CANCELLED` (hoặc `DECLINED`) quay lại **`INVITED`**.
    - Cập nhật lại vai trò mới `roleInTeam` nếu Leader có thay đổi.
    - Cập nhật `modifiedAt = LocalDateTime.now()`.
  - Giữ database luôn sạch sẽ, không sinh dữ liệu rác hay vi phạm khóa duy nhất.

---

### 3. Tối ưu Hiệu năng & Ràng buộc Concurrency

#### A. Gom Batch Query (Tránh N+1)
Thay vì lặp từng `memberId` để gọi `existsByCampaignTeam_IdAndMemberId` và `existsActiveAcceptedMembership`, viết 2 query gom nhóm:
1. `findExistingMemberIdsInTeam(teamId, memberIds)`: Kiểm tra ai đã có trong đội này.
2. `findActiveAcceptedMemberIds(memberIds)`: Kiểm tra ai đã gia nhập chính thức một đội khác đang hoạt động.

#### B. Đưa gRPC ra ngoài Database Transaction
- **Bước 1:** Query kiểm tra đội, chiến dịch, và các `memberId` trùng lặp (chỉ đọc).
- **Bước 2:** Gọi gRPC batch `userGrpcClient.getUsersByIds(memberIdsToFetch)` ngoài Transaction.
- **Bước 3:** Kiểm tra role `RESCUER`.
- **Bước 4:** Dùng `transactionTemplate.execute(...)` để lưu toàn bộ danh sách `CampaignMember` vào PostgreSQL trong 1 Transaction ngắn gọn.

#### C. Chống Race Condition khi Rescuer Accept Lời Mời
- **Tại sao không dùng DB Partial Unique Index:** Vì PostgreSQL Partial Index không thể JOIN với bảng `campaign_teams` để kiểm tra điều kiện `team.status <> 'DISBANDED'`. Nếu tạo Unique Index trên `(member_id) WHERE membership_status = 'ACCEPTED'`, cứu hộ viên sẽ vĩnh viễn không thể gia nhập đội mới ở các chiến dịch sau dù đội cũ đã giải tán.
- **Giải pháp:** Sử dụng **Redis Distributed Lock** theo `userId` khi gọi API `acceptInvitation`:
  - Khóa: `lock:rescuer:membership:${userId}` (thời hạn 5 giây).
  - Đảm bảo trong cùng 1 tích tắc, một Rescuer chỉ có thể xử lý 1 thao tác chấp nhận lời mời.
  - Sau khi lấy lock, kiểm tra `findActiveAcceptedMemberIds`: nếu đã có đội `ACCEPTED` đang hoạt động thì từ chối ngay. Sau đó cập nhật `ACCEPTED` và tự động `DECLINE` các lời mời còn lại trong Transaction.


---

## IV. ĐẶC TẢ DANH SÁCH API

### 1. `POST /api/v1/campaign-members/team/{teamId}` - Thêm/Mời thành viên vào đội
- **Header:** `X-User-Id: <UUID_LEADER>`
- **Path Variable:** `teamId` (UUID)
- **Request Body:**
  ```json
  {
    "members": [
      {
        "memberId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        "roleInTeam": "MEMBER"
      },
      {
        "fullName": "Nguyễn Văn Lái Thuyền",
        "phone": "0987654321",
        "sex": "MALE",
        "roleInTeam": "MEMBER"
      }
    ]
  }
  ```
- **Xử lý:**
  - Kiểm tra `team.approvalStatus == APPROVED` và `campaign.status == ACTIVE`.
  - Kiểm tra người gọi API có phải là Leader của đội không.
  - Từ chối nếu có phần tử chứa `roleInTeam == LEADER`.
  - Guest: Bắt buộc có `fullName` và `phone`.
  - Lưu `INVITED` cho registered, `ACCEPTED` cho guest.
- **Response:** `201 Created` kèm danh sách `MemberResponse`.

---

### 2. `GET /api/v1/campaign-members/team/{teamId}` - Lấy danh sách thành viên của đội
- **Path Variable:** `teamId`
- **Response:** `200 OK` danh sách thành viên kèm role, trạng thái tham gia, thông tin cá nhân.

---

### 3. `POST /api/v1/campaign-members/invitations/{membershipId}/accept` - Cứu hộ viên chấp nhận lời mời
- **Header:** `X-User-Id: <UUID_RESCUER>`
- **Path Variable:** `membershipId` (ID bản ghi `campaign_members`)
- **Xử lý:**
  - Kiểm tra bản ghi thuộc về `X-User-Id` và đang ở trạng thái `INVITED`.
  - Kiểm tra đội chưa giải tán (`status != DISBANDED`) và chiến dịch đang `ACTIVE`.
  - Kiểm tra user chưa có membership `ACCEPTED` ở đội khác đang hoạt động.
  - Chuyển `membershipStatus = ACCEPTED`.
  - Tự động hủy (`CANCELLED`) các lời mời `INVITED` khác của user này (nếu có).
- **Response:** `200 OK: "Đã tham gia đội cứu hộ thành công"`.

---

### 4. `POST /api/v1/campaign-members/invitations/{membershipId}/decline` - Cứu hộ viên từ chối lời mời
- **Header:** `X-User-Id: <UUID_RESCUER>`
- **Path Variable:** `membershipId`
- **Xử lý:**
  - Chuyển `membershipStatus = DECLINED`.
- **Response:** `200 OK: "Đã từ chối lời mời tham gia đội cứu hộ"`.

---

### 5. `GET /api/v1/campaign-members/my-invitations` - Xem danh sách lời mời của tôi
- **Header:** `X-User-Id: <UUID_RESCUER>`
- **Response:** `200 OK` danh sách các lời mời đang chờ (`INVITED`) kèm thông tin tên đội, tên chiến dịch, tên đội trưởng, SĐT đội trưởng.

---

### 6. `DELETE /api/v1/campaign-members/team/{teamId}/{memberId}` - Loại thành viên khỏi đội
- **Header:** `X-User-Id: <UUID_LEADER>`
- **Xử lý:**
  - Không cho phép xóa `LEADER`.
  - Chặn xóa nếu đội đang làm nhiệm vụ: `team.status == TeamStatus.BUSY`.
  - Cập nhật `membershipStatus = CANCELLED` (hoặc hard delete theo cấu hình).
- **Response:** `200 OK: "Xóa thành viên khỏi đội cứu hộ thành công"`.

---

## V. CÁC BƯỚC THỰC HIỆN (CHECKLIST)

### Giai đoạn 1: Chuẩn hóa Model & Validation
- [x] Bắt buộc số điện thoại cho Guest Member (`@NotBlank` và regex SĐT hợp lệ), giữ `phone` NOT NULL.
- [x] Chuẩn bị cơ chế Redis Distributed Lock `lock:rescuer:membership:${userId}` để bảo vệ khi Accept lời mời.

### Giai đoạn 2: Tối ưu & Bổ sung logic API Add Member (ĐÃ HOÀN THÀNH & COMPILE THÀNH CÔNG)
- [x] Viết query kiểm tra hàng loạt trong `CampaignMemberRepository` (loại bỏ N+1 query):
  - `findByCampaignTeam_IdAndMemberIdIn`
  - `findActiveAcceptedMemberIds`
  - `findExistingPhonesInTeam`
- [x] Cập nhật `CampaignMemberService.addMembers`:
  - [x] Xác thực người gọi bằng header `X-User-Id`: Chỉ Đội trưởng mới có quyền thêm/mời thành viên (`ErrorCode.FORBIDDEN`).
  - [x] Chặn khi `team.approvalStatus != APPROVED` hoặc Campaign không `ACTIVE`.
  - [x] Chặn `roleInTeam == LEADER`.
  - [x] Kiểm tra `userInfo.getRole() == "RESCUER"` qua gRPC.
  - [x] Đặt `INVITED` cho thành viên có tài khoản, `ACCEPTED` cho guest.
  - [x] Tái kích hoạt bản ghi cũ (`CANCELLED`/`DECLINED` -> `INVITED`) nếu mời lại người cũ trong đội.
  - [x] Di chuyển lệnh gọi gRPC ra ngoài Database Transaction.
  - [x] Dùng `TransactionTemplate` lưu dữ liệu nguyên tử.
- [x] Chuẩn hóa `MemberResponse` bổ sung `membershipStatus`, `membershipType`, `modifiedAt`.
- [x] Controller `POST /api/v1/teams/{teamId}/members` trả về HTTP `201 CREATED`.

### Giai đoạn 3: Triển khai luồng Lời mời (Accept / Decline) (ĐÃ HOÀN THÀNH & COMPILE THÀNH CÔNG)
- [x] Thêm API `GET /api/v1/campaign-teams/my-invitations` để Cứu hộ viên xem các đội đang mời mình.
- [x] Thêm API `POST /api/v1/campaign-teams/invitations/{membershipId}/accept`.
- [x] Thêm API `POST /api/v1/campaign-teams/invitations/{membershipId}/decline`.
- [x] Khi Accept: Sử dụng Redis Lock theo `userId`, kiểm tra điều kiện một người một đội (`existsActiveAcceptedMembership`), và tự động hủy (`CANCELLED`) các lời mời còn lại bằng câu SQL update hàng loạt.

### Giai đoạn 4: Hoàn thiện API Remove Member & Test Postman
- [ ] Thêm ràng buộc chặn xóa thành viên khi đội đang `BUSY`.
- [ ] Chuyển `membershipStatus = CANCELLED` khi thành viên bị loại.
- [ ] Biên dịch kiểm thử toàn bộ dự án (`mvn compile`).
- [ ] Viết kịch bản test Postman cho toàn bộ luồng Member.
