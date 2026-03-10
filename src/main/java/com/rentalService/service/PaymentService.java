package com.rentalService.service;

import com.rentalService.model.Payment;
import com.rentalService.repository.PaymentRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.transaction.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/**
 * PaymentService with an automatic mock fallback when Razorpay credentials are not configured.
 *
 * How it works:
 *  - If razorpay.key_id and razorpay.key_secret are provided via properties, the service uses RazorpayClient.
 *  - If either property is missing/blank, the service uses an internal mock implementation so your endpoints work for testing.
 *
 * IMPORTANT: Mock mode is for local testing only. Remove or disable mock mode before production.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final boolean useMock;
    private final RazorpayClient razorpayClient; // null in mock mode
    private final String razorpayKeyId;
    private final String razorpayKeySecret;
    private final String razorpayWebhookSecret;

    public PaymentService(
            PaymentRepository paymentRepository,
            @Value("${razorpay.key_id:}") String razorpayKeyId,
            @Value("${razorpay.key_secret:}") String razorpayKeySecret,
            @Value("${razorpay.webhook_secret:}") String razorpayWebhookSecret
    ) throws RazorpayException {
        this.paymentRepository = paymentRepository;
        this.razorpayKeyId = razorpayKeyId != null ? razorpayKeyId.trim() : "";
        this.razorpayKeySecret = razorpayKeySecret != null ? razorpayKeySecret.trim() : "";
        this.razorpayWebhookSecret = razorpayWebhookSecret != null ? razorpayWebhookSecret.trim() : "";

        if (!this.razorpayKeyId.isEmpty() && !this.razorpayKeySecret.isEmpty()) {
            // real mode
            this.useMock = false;
            this.razorpayClient = new RazorpayClient(this.razorpayKeyId, this.razorpayKeySecret);
            log.info("PaymentService configured in REAL Razorpay mode with key id: {}", this.razorpayKeyId);
        } else {
            // mock mode
            this.useMock = true;
            this.razorpayClient = null;
            log.warn("PaymentService configured in MOCK mode (razorpay.key_id / key_secret missing).");
        }
    }

    // -------------------------
    // Public API methods
    // -------------------------

    /**
     * Create a local Payment record and an order (Razorpay order in real mode, fake order in mock).
     */
    @Transactional
    public Map<String, Object> createOrderForPayment(String rentalOrderId,
                                                     Long userId,
                                                     Long amount,
                                                     String currency,
                                                     String receipt,
                                                     Map<String, String> notes) throws Exception {
        // create local DB record
        Payment payment = new Payment();
        payment.setRentalOrderId(rentalOrderId);
        payment.setUserId(userId);
        payment.setAmount(amount);
        payment.setCurrency(currency != null ? currency : "INR");
        payment.setStatus("CREATED");
        if (notes != null && !notes.isEmpty()) {
            payment.setNotes(new JSONObject(notes).toString());
        }
        payment = paymentRepository.save(payment);

        JSONObject orderJson;
        if (useMock) {
            orderJson = createMockOrder(amount, payment.getCurrency(), receipt, notes);
        } else {
            // real Razorpay call
            JSONObject options = new JSONObject();
            options.put("amount", amount);
            options.put("currency", payment.getCurrency());
            options.put("receipt", receipt != null ? receipt : ("rental_" + rentalOrderId));
            options.put("payment_capture", 0);
            if (notes != null && !notes.isEmpty()) options.put("notes", new JSONObject(notes));

            com.razorpay.Order order = razorpayClient.orders.create(options);
            orderJson = new JSONObject(order.toString());
        }

        payment.setRazorpayOrderId(orderJson.optString("id", null));
        paymentRepository.save(payment);

        Map<String, Object> result = new HashMap<>();
        result.put("order", orderJson);
        result.put("payment", payment);
        return result;
    }

    /**
     * Verify checkout signature (in mock mode this performs a simple check and marks AUTHORIZED/CAPTURED).
     */
    @Transactional
    public Payment verifyCheckoutSignature(String razorpayOrderId,
                                           String razorpayPaymentId,
                                           String razorpaySignature) throws Exception {
        if (useMock) {
            // mock verification: signature must equal "mock-signature" or be blank -> allow for tests
            boolean ok = "mock-signature".equals(razorpaySignature)
                    || razorpaySignature == null
                    || razorpaySignature.trim().isEmpty();   // Java 8

            if (!ok) {
                throw new IllegalArgumentException("Invalid mock signature");
            }

            Optional<Payment> maybe = paymentRepository.findByRazorpayOrderId(razorpayOrderId);
            Payment payment = maybe.orElseGet(new java.util.function.Supplier<Payment>() {
                @Override
                public Payment get() {
                    Payment p = new Payment();
                    p.setRentalOrderId("unknown");
                    p.setUserId(-1L);
                    p.setAmount(0L);
                    p.setCurrency("INR");
                    p.setStatus("UNKNOWN");
                    p.setRazorpayOrderId(razorpayOrderId);
                    return p;
                }
            });

            payment.setRazorpayPaymentId(razorpayPaymentId);
            payment.setStatus("CAPTURED");
            paymentRepository.save(payment);
            return payment;

        } else {
            boolean ok = verifySignatureHmacSha256(
                    razorpayOrderId + "|" + razorpayPaymentId,
                    razorpaySignature,
                    this.razorpayKeySecret
            );
            if (!ok) {
                throw new IllegalArgumentException("Invalid razorpay signature");
            }

            com.razorpay.Payment rpPayment = razorpayClient.payments.fetch(razorpayPaymentId);

            Optional<Payment> maybe = paymentRepository.findByRazorpayOrderId(razorpayOrderId);
            Payment payment = maybe.orElseGet(new java.util.function.Supplier<Payment>() {
                @Override
                public Payment get() {
                    Payment p = new Payment();
                    p.setRentalOrderId(razorpayOrderId);
                    p.setUserId(-1L);
                    p.setAmount(0L);
                    p.setCurrency("INR");
                    p.setStatus("UNKNOWN");
                    p.setRazorpayOrderId(razorpayOrderId);
                    return p;
                }
            });

            payment.setRazorpayPaymentId(razorpayPaymentId);
            String rpStatus = String.valueOf(rpPayment.get("status"));
            if ("captured".equalsIgnoreCase(rpStatus)) {
                payment.setStatus("CAPTURED");
            } else if ("authorized".equalsIgnoreCase(rpStatus)) {
                payment.setStatus("AUTHORIZED");
            } else if ("failed".equalsIgnoreCase(rpStatus)) {
                payment.setStatus("FAILED");
            } else {
                payment.setStatus(rpStatus != null ? rpStatus.toUpperCase() : "UNKNOWN");
            }

            paymentRepository.save(payment);
            return payment;
        }
    }


    /**
     * Capture an authorized payment. In mock mode returns fake captured object.
     */
    @Transactional
    public JSONObject capturePayment(String razorpayPaymentId, Long amount) throws Exception {
        if (useMock) {
            JSONObject captured = createMockCapturedPayment(razorpayPaymentId, amount);
            // update local payment record if exists
            Optional<Payment> maybe = paymentRepository.findByRazorpayPaymentId(razorpayPaymentId);
            if (maybe.isPresent()) {
                Payment payment = maybe.get();
                payment.setStatus("CAPTURED");
                paymentRepository.save(payment);
            }
            return captured;
        } else {
            JSONObject captureRequest = new JSONObject();
            captureRequest.put("amount", amount);
            com.razorpay.Payment captured = razorpayClient.payments.capture(razorpayPaymentId, captureRequest);
            Optional<Payment> maybe = paymentRepository.findByRazorpayPaymentId(razorpayPaymentId);
            if (maybe.isPresent()) {
                Payment payment = maybe.get();
                String rpStatus = String.valueOf(captured.get("status"));
                payment.setRazorpayPaymentId(razorpayPaymentId);
                payment.setStatus("captured".equalsIgnoreCase(rpStatus) ? "CAPTURED" : rpStatus.toUpperCase());
                paymentRepository.save(payment);
            }
            return new JSONObject(captured.toString());
        }
    }

    public JSONObject fetchRazorpayPayment(String razorpayPaymentId) throws Exception {
        if (useMock) {
            return createMockFetchedPayment(razorpayPaymentId);
        } else {
            com.razorpay.Payment rpPayment = razorpayClient.payments.fetch(razorpayPaymentId);
            return new JSONObject(rpPayment.toString());
        }
    }

    @Transactional
    public JSONObject initiateRefund(String razorpayPaymentId, Long amount, Map<String, String> notes) throws Exception {
        if (useMock) {
            JSONObject refundJson = createMockRefund(razorpayPaymentId, amount, notes);
            Optional<Payment> maybe = paymentRepository.findByRazorpayPaymentId(razorpayPaymentId);
            if (maybe.isPresent()) {
                Payment p = maybe.get();
                p.setStatus("REFUND_INITIATED");
                paymentRepository.save(p);
            }
            return refundJson;
        } else {
            JSONObject req = new JSONObject();
            if (amount != null) req.put("amount", amount);
            if (notes != null && !notes.isEmpty()) req.put("notes", new JSONObject(notes));
            com.razorpay.Refund refund = razorpayClient.payments.refund(razorpayPaymentId, req);
            Optional<Payment> maybe = paymentRepository.findByRazorpayPaymentId(razorpayPaymentId);
            if (maybe.isPresent()) {
                Payment p = maybe.get();
                p.setStatus("REFUND_INITIATED");
                paymentRepository.save(p);
            }
            return new JSONObject(refund.toString());
        }
    }

    public boolean verifyWebhookSignature(String payloadBody, String signature, String webhookSecret) {
        if (useMock) {
            // in mock mode accept any signature or match a fixed test signature
            return signature == null
                    || signature.trim().isEmpty()
                    || "mock-webhook-signature".equals(signature);
        }
        try {
            return verifySignatureHmacSha256(payloadBody, signature, webhookSecret);
        } catch (Exception e) {
            log.error("Webhook signature verification exception", e);
            return false;
        }
    }

    // -------------------------
    // Mock helpers (local deterministic data)
    // -------------------------

    private JSONObject createMockOrder(Long amount, String currency, String receipt, Map<String, String> notes) {
        String orderId = "order_test_" + Instant.now().toEpochMilli();
        JSONObject o = new JSONObject();
        o.put("id", orderId);
        o.put("entity", "order");
        o.put("amount", amount);
        o.put("amount_paid", 0);
        o.put("amount_due", amount);
        o.put("currency", currency);
        o.put("receipt", receipt != null ? receipt : ("rcpt_" + orderId));
        o.put("status", "created");
        o.put("notes", notes != null ? new JSONObject(notes) : new JSONObject());
        o.put("created_at", Instant.now().getEpochSecond());
        return o;
    }

    private JSONObject createMockCapturedPayment(String paymentId, Long amount) {
        JSONObject p = new JSONObject();
        p.put("id", paymentId != null ? paymentId : ("pay_test_" + Instant.now().toEpochMilli()));
        p.put("entity", "payment");
        p.put("status", "captured");
        p.put("amount", amount != null ? amount : 0);
        p.put("currency", "INR");
        p.put("method", "card");
        p.put("created_at", Instant.now().getEpochSecond());
        return p;
    }

    private JSONObject createMockFetchedPayment(String paymentId) {
        JSONObject p = new JSONObject();
        p.put("id", paymentId != null ? paymentId : ("pay_test_" + Instant.now().toEpochMilli()));
        p.put("entity", "payment");
        p.put("status", "captured");
        p.put("amount", 100L);
        p.put("currency", "INR");
        p.put("method", "card");
        p.put("created_at", Instant.now().getEpochSecond());
        return p;
    }

    private JSONObject createMockRefund(String paymentId, Long amount, Map<String, String> notes) {
        JSONObject r = new JSONObject();
        r.put("id", "refund_test_" + Instant.now().toEpochMilli());
        r.put("entity", "refund");
        r.put("payment_id", paymentId);
        r.put("status", "processed");
        r.put("amount", amount != null ? amount : 0);
        r.put("notes", notes != null ? new JSONObject(notes) : new JSONObject());
        r.put("created_at", Instant.now().getEpochSecond());
        return r;
    }

    // -------------------------
    // Crypto helpers (real mode)
    // -------------------------

    private boolean verifySignatureHmacSha256(String payload, String signature, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        String computed = Hex.encodeHexString(digest);
        return constantTimeEquals(computed, signature);
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) result |= a.charAt(i) ^ b.charAt(i);
        return result == 0;
    }
}
