package com.rentalService.controller;

import com.rentalService.model.Payment;
import com.rentalService.repository.PaymentRepository;
import com.rentalService.service.PaymentService;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;

    @Value("${razorpay.key_id}")
    private String razorpayKeyId;

    @Value("${razorpay.webhook_secret:}")
    private String webhookSecret;

    public PaymentController(PaymentService paymentService,
                             PaymentRepository paymentRepository) {
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
    }

    @PostMapping(
            path = "/payments/orders",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> body) {
        try {
            String rentalOrderId = Objects.toString(body.get("rentalOrderId"), null);
            if (!StringUtils.hasText(rentalOrderId)) {
                Map<String, String> error = new HashMap<String, String>();
                error.put("error", "rentalOrderId is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            Object userIdObj = body.get("userId");
            if (userIdObj == null) {
                Map<String, String> error = new HashMap<String, String>();
                error.put("error", "userId is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            Long userId;
            try {
                userId = Long.parseLong(userIdObj.toString());
            } catch (NumberFormatException ex) {
                Map<String, String> error = new HashMap<String, String>();
                error.put("error", "userId must be numeric");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            Object amountObj = body.get("amount");
            if (amountObj == null) {
                Map<String, String> error = new HashMap<String, String>();
                error.put("error", "amount is required (smallest currency unit)");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            Long amount;
            try {
                amount = Long.parseLong(amountObj.toString());
            } catch (NumberFormatException ex) {
                Map<String, String> error = new HashMap<String, String>();
                error.put("error", "amount must be numeric (smallest currency unit)");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            String currency = Objects.toString(body.get("currency"), "INR");
            String receipt = Objects.toString(body.get("receipt"), null);

            Map<String, String> notes = new HashMap<String, String>();
            Object notesObj = body.get("notes");
            if (notesObj instanceof Map) {
                Map<?, ?> nm = (Map<?, ?>) notesObj;
                for (Map.Entry<?, ?> entry : nm.entrySet()) {
                    Object k = entry.getKey();
                    Object v = entry.getValue();
                    if (k != null && v != null) {
                        notes.put(k.toString(), v.toString());
                    }
                }
            } else if (notesObj instanceof String && StringUtils.hasText((String) notesObj)) {
                try {
                    JSONObject j = new JSONObject((String) notesObj);
                    for (String key : j.keySet()) {
                        notes.put(key, j.get(key).toString());
                    }
                } catch (Exception e) {
                    log.warn("Unable to parse notes JSON: {}", e.getMessage());
                }
            }

            Map<String, Object> result =
                    paymentService.createOrderForPayment(rentalOrderId, userId, amount, currency, receipt, notes);

            Map<String, Object> response = new HashMap<String, Object>();
            response.put("keyId", razorpayKeyId);
            response.put("order", result.get("order"));
            response.put("payment", result.get("payment"));
            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            log.error("Error creating order", ex);
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", "failed to create order");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping(
            path = "/payments/verify",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> body) {
        String razorpayPaymentId = body.get("razorpay_payment_id");
        String razorpayOrderId = body.get("razorpay_order_id");
        String razorpaySignature = body.get("razorpay_signature");

        if (!StringUtils.hasText(razorpayPaymentId)
                || !StringUtils.hasText(razorpayOrderId)
                || !StringUtils.hasText(razorpaySignature)) {

            Map<String, String> error = new HashMap<String, String>();
            error.put("error",
                    "razorpay_payment_id, razorpay_order_id, and razorpay_signature are required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        try {
            Payment updated =
                    paymentService.verifyCheckoutSignature(razorpayOrderId, razorpayPaymentId, razorpaySignature);

            Map<String, Object> response = new HashMap<String, Object>();
            response.put("payment", updated);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);

        } catch (Exception ex) {
            log.error("Error verifying payment signature", ex);
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", "verification failed");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping(path = "/webhooks/razorpay", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> handleWebhook(HttpServletRequest request) {
        try {
            byte[] raw = readAllBytes(request);
            String body = new String(raw, StandardCharsets.UTF_8);

            String signatureHeader = request.getHeader("X-Razorpay-Signature");
            if (!StringUtils.hasText(signatureHeader)) {
                log.warn("Missing X-Razorpay-Signature header");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("missing signature");
            }

            boolean verified = paymentService.verifyWebhookSignature(body, signatureHeader, webhookSecret);
            if (!verified) {
                log.warn("Invalid webhook signature");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("invalid signature");
            }

            JSONObject payload = new JSONObject(body);
            String event = payload.optString("event", "");
            JSONObject entity = payload.has("payload") ? payload.getJSONObject("payload") : new JSONObject();

            log.info("Received webhook event: {}", event);

            if ("payment.captured".equals(event)) {
                processPaymentCaptured(entity);
            } else if ("payment.failed".equals(event)) {
                processPaymentFailed(entity);
            } else if ("order.paid".equals(event)) {
                processOrderPaid(entity);
            } else {
                if (event != null && event.startsWith("refund")) {
                    processRefundEvent(event, entity);
                } else {
                    log.info("Unhandled webhook event: {}", event);
                }
            }

            return ResponseEntity.ok("ok");

        } catch (IOException ex) {
            log.error("IOException reading webhook body", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("io error");

        } catch (Exception ex) {
            log.error("Exception processing webhook", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("processing error");
        }
    }

    // ---- helper to read request body in Java 8 (no InputStream.readAllBytes) ----
    private byte[] readAllBytes(HttpServletRequest request) throws IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int nRead;
        java.io.InputStream is = request.getInputStream();
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    // ---- webhook processors ----

    private void processPaymentCaptured(JSONObject payloadWrapper) {
        JSONObject paymentObj = extractEntity(payloadWrapper, "payment", "entity");
        if (paymentObj == null) return;

        String razorpayPaymentId = paymentObj.optString("id", null);
        String razorpayOrderId = paymentObj.optString("order_id", null);

        if (StringUtils.hasText(razorpayPaymentId)) {
            Optional<Payment> maybe = paymentRepository.findByRazorpayPaymentId(razorpayPaymentId);
            if (maybe.isPresent()) {
                Payment payment = maybe.get();
                payment.setStatus("CAPTURED");
                paymentRepository.save(payment);
                return;
            }
        }

        if (StringUtils.hasText(razorpayOrderId)) {
            Optional<Payment> maybe = paymentRepository.findByRazorpayOrderId(razorpayOrderId);
            if (maybe.isPresent()) {
                Payment payment = maybe.get();
                payment.setRazorpayPaymentId(razorpayPaymentId);
                payment.setStatus("CAPTURED");
                paymentRepository.save(payment);
                return;
            }
        }

        log.warn("No local payment record matched for payment.captured (paymentId={}, orderId={})",
                razorpayPaymentId, razorpayOrderId);
    }

    private void processPaymentFailed(JSONObject payloadWrapper) {
        JSONObject paymentObj = extractEntity(payloadWrapper, "payment", "entity");
        if (paymentObj == null) return;

        String razorpayPaymentId = paymentObj.optString("id", null);
        String razorpayOrderId = paymentObj.optString("order_id", null);

        if (StringUtils.hasText(razorpayPaymentId)) {
            Optional<Payment> maybe = paymentRepository.findByRazorpayPaymentId(razorpayPaymentId);
            if (maybe.isPresent()) {
                Payment payment = maybe.get();
                payment.setStatus("FAILED");
                paymentRepository.save(payment);
                return;
            }
        }

        if (StringUtils.hasText(razorpayOrderId)) {
            Optional<Payment> maybe = paymentRepository.findByRazorpayOrderId(razorpayOrderId);
            if (maybe.isPresent()) {
                Payment payment = maybe.get();
                payment.setRazorpayPaymentId(razorpayPaymentId);
                payment.setStatus("FAILED");
                paymentRepository.save(payment);
                return;
            }
        }

        log.warn("No local payment record matched for payment.failed (paymentId={}, orderId={})",
                razorpayPaymentId, razorpayOrderId);
    }

    private void processOrderPaid(JSONObject payloadWrapper) {
        JSONObject orderObj = extractEntity(payloadWrapper, "order", "entity");
        if (orderObj == null) return;

        String razorpayOrderId = orderObj.optString("id", null);
        if (StringUtils.hasText(razorpayOrderId)) {
            Optional<Payment> maybe = paymentRepository.findByRazorpayOrderId(razorpayOrderId);
            if (maybe.isPresent()) {
                Payment payment = maybe.get();
                payment.setStatus("CAPTURED");
                paymentRepository.save(payment);
                return;
            }
        }

        log.warn("No local payment record matched for order.paid (orderId={})", razorpayOrderId);
    }

    private void processRefundEvent(String event, JSONObject payloadWrapper) {
        JSONObject refundObj = extractEntity(payloadWrapper, "refund", "entity");
        if (refundObj == null) return;

        String refundId = refundObj.optString("id", null);
        String paymentId = refundObj.optString("payment_id", null);

        if (StringUtils.hasText(paymentId)) {
            Optional<Payment> maybe = paymentRepository.findByRazorpayPaymentId(paymentId);
            if (maybe.isPresent()) {
                Payment payment = maybe.get();

                if ("refund.processed".equals(event)
                        || "refund.created".equals(event)
                        || "refund.updated".equals(event)) {
                    payment.setStatus("REFUNDED");
                } else if ("refund.failed".equals(event)) {
                    payment.setStatus("REFUND_FAILED");
                } else {
                    payment.setStatus("REFUND_" + event.toUpperCase());
                }

                paymentRepository.save(payment);
            }
        }

        if (refundId != null) {
            log.info("Processed refund event {} for refundId={}", event, refundId);
        }
    }

    private JSONObject extractEntity(JSONObject payloadWrapper, String key, String subKey) {
        try {
            if (payloadWrapper == null) return null;
            JSONObject obj = payloadWrapper.optJSONObject(key);
            if (obj == null) return null;
            return obj.optJSONObject(subKey);
        } catch (Exception e) {
            log.warn("Error extracting entity {}.{}", key, subKey, e);
            return null;
        }
    }
}
