# RentalCarApp Live API Testing Documentation

## 1. Base Details
- Base URL: `103.14.99.198:8082/RentalCarApp`
- Health endpoint: `GET /__test/controllers/alive`
- Full health URL: `103.14.99.198:8082/RentalCarApp/__test/controllers/alive`
- Auth header format: `Authorization: Bearer <accessToken>`
- JSON content type: `Content-Type: application/json`

## 2. Postman Environment Variables
Set these once in Postman:
- `baseUrl = 103.14.99.198:8082/RentalCarApp`
- `customerMobile`
- `vendorMobile`
- `adminMobile`
- `customerAccessToken`
- `vendorAccessToken`
- `adminAccessToken`
- `customerRefreshToken`
- `vendorRefreshToken`
- `adminRefreshToken`
- `vehicleId`
- `bookingId`
- `notificationId`

## 3. Role and Token Rules
- Customer token is required for customer-protected APIs.
- Vendor token is required for vendor operations.
- Admin token is required for admin vehicle moderation endpoints.
- Public APIs do not need token.
- Notifications and bookings require authentication.

## 4. Authentication APIs
Base: `{{baseUrl}}`

### 4.1 Customer/Vendor Auth
- `POST /auth/register-customer`
- `POST /auth/register-vendor`
- `POST /auth/request-otp`
- `POST /auth/verify-otp`
- `POST /auth/refresh`
- `POST /auth/logout`

### 4.2 Admin Auth
- `POST /admin/auth/register`
- `POST /admin/auth/request-otp`
- `POST /admin/auth/verify-otp`
- `POST /admin/auth/refresh`

### 4.3 Sample Bodies
Register customer:
```json
{
  "mobile": "2222222222",
  "name": "Test Customer",
  "email": "customer@test.com",
  "address": "Address",
  "city": "City",
  "dob": "1995-01-10",
  "interests": ["CAR"]
}
```

Register vendor:
```json
{
  "mobile": "3333333333",
  "name": "Test Vendor",
  "email": "vendor@test.com",
  "address": "Address",
  "city": "City",
  "dob": "1990-05-12"
}
```

Register admin:
```json
{
  "mobile": "9999999999",
  "name": "Test Admin",
  "email": "admin@test.com"
}
```

Request OTP:
```json
{ "mobile": "2222222222" }
```

Verify OTP:
```json
{ "mobile": "2222222222", "code": "123456" }
```

Refresh token:
```json
{ "refreshToken": "<refresh-token>" }
```

Logout:
```json
{ "userId": "<uuid>" }
```

## 5. Vehicle APIs
### 5.1 Vendor Vehicle APIs
- `POST /vehicles/add` (multipart/form-data, vendor token)
- `GET /vehicles/my` (vendor token)
- `PUT /vehicles/{id}` (vendor token)
- `DELETE /vehicles/{id}` (vendor token)

### 5.2 Public Vehicle APIs
- `GET /vehicles` (active/approved list)
- `GET /vehicles/{id}`
- `GET /vehicles/images/{filename}`

### 5.3 Admin Vehicle APIs
- `GET /api/admin/vehicles?page=0&size=20&status=UNDER_REVIEW` (admin token)
- `PATCH /api/admin/vehicles/{id}/status` (admin token)
- `PATCH /api/admin/vehicles/{id}/toggle-active` (admin token)

Admin status body:
```json
{
  "status": "ACTIVE",
  "reason": "All docs verified"
}
```

## 6. Booking APIs
All booking APIs require auth token.

- `POST /bookings/create` (customer token)
- `PATCH /bookings/vendor/{bookingId}/status?status=CONFIRMED` (vendor token)
- `PATCH /bookings/vendor/{bookingId}/status?status=REJECTED` (vendor token)
- `PATCH /bookings/{bookingId}/cancel` (customer token)
- `GET /bookings/vendor/my` (vendor token)
- `GET /bookings/my` (customer token)
- `GET /bookings/{bookingId}` (customer token)
- `PATCH /bookings/{bookingId}/extend` (customer token)
- `GET /bookings/upcoming` (customer token)
- `GET /bookings/completed` (customer token)

Create booking example URL:
`{{baseUrl}}/bookings/create?vehicleId=<uuid>&startDate=2026-03-18&endDate=2026-03-20&withDriver=true`

Extend booking body:
```json
{ "endDate": "2026-03-25" }
```

## 7. Review APIs
- `POST /reviews` (customer token)
- `GET /reviews/vehicle/{vehicleId}` (public)

Review body:
```json
{
  "vehicleId": "<vehicle-uuid>",
  "rating": 5,
  "comment": "Great ride"
}
```

Rule:
- Review allowed only if customer has completed booking for that vehicle.

## 8. Notification APIs
- `GET /notifications` (auth required)
- `PATCH /notifications/{id}/read` (auth required)

How to get `id` for mark-read:
- First call `GET /notifications`
- Copy one notification item `id`
- Use it in `PATCH /notifications/{id}/read`

## 9. Banner APIs
- `POST /banners/upload` (multipart)
- `GET /banners`
- `GET /banners/files/{filename}`
- `DELETE /banners/{id}`

## 10. Card APIs
- `POST /cards` (multipart)
- `GET /cards`
- `DELETE /cards/{id}`

## 11. Users API
- `GET /users/getAllUsers`

## 12. Payment APIs
- `POST /api/payments/orders` (auth required)
- `POST /api/payments/verify` (auth required)
- `POST /api/webhooks/razorpay`

## 13. Notification Trigger Logic (Implemented)
- Booking created -> vendor gets notification.
- Booking status changed to CONFIRMED/REJECTED -> customer gets notification.
- Admin vehicle status change/toggle -> vendor gets notification.

## 14. Recommended End-to-End Testing Flow
1. Verify service health endpoint.
2. Register customer, vendor, admin.
3. Request OTP and verify OTP for each role.
4. Vendor adds vehicle.
5. Admin approves or toggles vehicle status.
6. Customer creates booking.
7. Vendor confirms or rejects booking.
8. Customer validates booking by ID, upcoming, completed.
9. Customer submits review (after completed booking).
10. Check notifications for vendor/customer.
11. Mark one notification as read.
12. Smoke test cards, banners, users list.

## 15. Common Error Cases for Testers
- `401/403`: missing, invalid, expired, or wrong-role token.
- `400`: validation/business-rule error (invalid date, invalid OTP, unauthorized ownership).
- `404`: wrong endpoint path/context or missing resource.
- `409`: duplicate/constraint conflict.
- `500`: unexpected server issue; capture request + response + timestamp for debugging.

## 16. Important Context-Path Note
All endpoints in this document assume:
- context path: `/RentalCarApp`
- host: `103.14.99.198`
- port: `8082`

So each endpoint format is:
`103.14.99.198:8082/RentalCarApp/<endpoint>`

