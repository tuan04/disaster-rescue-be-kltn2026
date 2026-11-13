# Facebook Collector

API Express dùng Playwright để lấy bài viết từ các group Facebook đã cấu hình.

## Cấu trúc

```
config/groups.json              Danh sách group cần đọc
src/
  server.js                     Khởi động HTTP server
  app.js                        Khởi tạo Express và gắn router
  routes/
    index.js                    Gắn các nhóm route và health check
    facebook.routes.js          Route GET /api/facebook/posts
  controllers/
    facebook.controller.js      Gọi service, trả JSON thành công hoặc lỗi
    health.controller.js        Trả trạng thái hoạt động
  services/
    facebook.service.js         Quản lý browser, context, batch và crawl group
  config/
    facebook.config.js          Giới hạn crawl, selector và đường dẫn dữ liệu
  utils/
    facebook-post.util.js       Trích xuất postId và tạo ID từ nội dung
  login.js                      Đăng nhập thủ công và lưu phiên
test/
  api.test.js                   Kiểm thử HTTP và đóng browser khi có lỗi
```

## Chạy ứng dụng

Tại thư mục facebook-collector:

```powershell
npm run login
npm start
```

Chỉ cần chạy login khi chưa có phiên hoặc phiên không còn dùng được. Đăng nhập
Facebook trong cửa sổ trình duyệt, sau đó nhấn Enter ở terminal để lưu phiên.
Server và login cùng dùng đường dẫn .auth/facebook.json tính từ gốc dự án.

## API

- GET /health: trả { "status": "ok" }.
- GET /api/facebook/posts: trả { "total": 2, "posts": [...] } khi lấy được bài.
- Khi crawl thất bại: HTTP 500 với error = FACEBOOK_COLLECT_FAILED và message.

Luồng xử lý: routes/index.js -> facebook.routes.js -> getPosts() trong
facebook.controller.js -> collectPosts()/collectGroup() trong facebook.service.js.
Service luôn đóng browser trong finally; controller chuyển lỗi thành JSON.

Giữ cấu hình hiện tại: cổng 3000, tối đa 2 bài/group, 30 lần cuộn, 2 group song song,
trình duyệt hiển thị khi crawl. Sửa các hằng số trong src/config/facebook.config.js
để thay đổi giới hạn; sửa config/groups.json để thay danh sách group.

## Kiểm thử

```powershell
npm test
```

Các test dùng dữ liệu giả, không gọi Facebook và không cần đăng nhập.
