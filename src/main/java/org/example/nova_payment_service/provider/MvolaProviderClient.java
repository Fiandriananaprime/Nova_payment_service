package org.example.nova_payment_service.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.wso2.client.api.ApiClient;
import org.wso2.client.api.ApiException;
import org.wso2.client.api.ApiResponse;
import org.wso2.client.api.MVOLA_Merchant_Pay_API.DefaultApi;
import org.wso2.client.model.MVOLA_Merchant_Pay_API.PostRequest;
import org.wso2.client.model.MVOLA_Merchant_Pay_API.PostRequestDebitPartyInner;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * MVola Merchant Pay adapter backed by the generated WSO2 SDK.
 * Merchant Pay is customer-to-merchant collection, not seller disbursement.
 * MVOLA_BASE_URL is the provider origin, e.g. http://localhost:4010.
 */
@Component
public class MvolaProviderClient {
    private static final String API_PATH = "/mvola/mm/transactions/type/merchantpay/1.0.0";
    private static final String TOKEN_SCOPE = "EXT_INT_MVOLA_SCOPE";

    private final RestClient tokenClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String clientKey;
    private final String clientSecret;
    private final String merchantMsisdn;
    private final String partnerName;
    private final String userLanguage;
    private volatile CachedToken cachedToken;

    public MvolaProviderClient(RestClient.Builder builder, ObjectMapper objectMapper, Environment environment) {
        this.baseUrl = trimTrailingSlash(environment.getProperty("MVOLA_BASE_URL", "http://localhost:4010").trim());
        this.tokenClient = builder.clone().baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.clientKey = environment.getProperty("MVOLA_CLIENT_KEY", "local-test-key").trim();
        this.clientSecret = environment.getProperty("MVOLA_CLIENT_SECRET", "local-test-secret").trim();
        this.merchantMsisdn = environment.getProperty("MVOLA_MERCHANT_MSISDN", "0343500001").trim();
        this.partnerName = environment.getProperty("MVOLA_PARTNER_NAME", "NovaMarket").trim();
        this.userLanguage = environment.getProperty("MVOLA_USER_LANGUAGE", "FR").trim().toUpperCase();
    }

    public InitiationResponse initiatePayment(PaymentRequest request) {
        validatePaymentRequest(request);

        PostRequest sdkRequest = new PostRequest()
                .amount(request.amount().stripTrailingZeros().toPlainString())
                .currency("Ar")
                .descriptionText(request.description().trim())
                .requestingOrganisationTransactionReference(request.transactionReference().trim())
                .originalTransactionReference(request.transactionReference().trim())
                .requestDate(Instant.now().toString())
                .debitParty(List.of(party(request.customerMsisdn().trim())))
                .creditParty(List.of(party(merchantMsisdn)));

        List<PostRequestDebitPartyInner> metadata = new ArrayList<>();
        metadata.add(keyValue("partnerName", partnerName));
        if (request.metadata() != null) {
            request.metadata().stream()
                    .filter(item -> item != null && item.key() != null && item.value() != null)
                    .map(item -> keyValue(item.key(), item.value()))
                    .forEach(metadata::add);
        }
        sdkRequest.metadata(metadata);

        SdkResult result = executeSdk(api -> api.rootPostWithHttpInfo(
                "1.0", UUID.randomUUID().toString(), "no-cache", sdkRequest,
                null, null, null, null, "application/json", "UTF-8", blankToNull(request.callbackUrl())
        ));

        Map<String, Object> body = result.body();
        return new InitiationResponse(
                stringValue(body, "status"),
                stringValue(body, "serverCorrelationId"),
                stringValue(body, "notificationMethod"),
                result.statusCode()
        );
    }

    public TransactionStatus getTransactionStatus(String serverCorrelationId) {
        requireText(serverCorrelationId, "serverCorrelationId");
        SdkResult result = executeSdk(api -> api.statusServerCorrelationIdGetWithHttpInfo(
                serverCorrelationId.trim(), "1.0", UUID.randomUUID().toString(),
                "msisdn;" + merchantMsisdn, partnerName, "no-cache",
                null, null, "application/json", "UTF-8"
        ));

        Map<String, Object> body = result.body();
        return new TransactionStatus(
                stringValue(body, "status"),
                stringValue(body, "serverCorrelationId"),
                stringValue(body, "notificationMethod"),
                stringValue(body, "objectReference"),
                result.statusCode()
        );
    }

    public Map<String, Object> getTransactionDetails(String transactionReference) {
        requireText(transactionReference, "transactionReference");
        return executeSdk(api -> api.transactionReferenceGetWithHttpInfo(
                transactionReference.trim(), "1.0", UUID.randomUUID().toString(),
                "msisdn;" + merchantMsisdn, "no-cache",
                null, null, "application/json", "UTF-8"
        )).body();
    }

    private SdkResult executeSdk(SdkCall call) {
        validateConfiguration();
        ResponseCapture capture = new ResponseCapture();

        OkHttpClient httpClient = new ApiClient().getHttpClient().newBuilder()
                .addInterceptor(chain -> {
                    okhttp3.Request request = chain.request().newBuilder()
                            .header("UserLanguage", userLanguage)
                            .header("UserAccountIdentifier", "msisdn;" + merchantMsisdn)
                            .header("partnerName", partnerName)
                            .build();
                    Response response = chain.proceed(request);
                    ResponseBody body = response.body();
                    if (body != null) {
                        capture.body = response.peekBody(1_048_576L).string();
                    }
                    return response;
                })
                .build();

        ApiClient apiClient = new ApiClient().setHttpClient(httpClient);
        apiClient.setAccessToken(accessToken());
        DefaultApi api = new DefaultApi(apiClient);
        api.setCustomBaseUrl(baseUrl + API_PATH);

        try {
            ApiResponse<Void> response = call.execute(api);
            return new SdkResult(response.getStatusCode(), parseResponseBody(capture.body));
        } catch (ApiException exception) {
            String responseBody = exception.getResponseBody();
            throw new MvolaProviderException(
                    "MVola SDK request failed with HTTP " + exception.getCode()
                            + (responseBody == null || responseBody.isBlank() ? "" : ": " + responseBody),
                    exception.getCode(),
                    exception
            );
        }
    }

    private synchronized String accessToken() {
        CachedToken current = cachedToken;
        if (current != null && Instant.now().isBefore(current.expiresAt())) {
            return current.value();
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("scope", TOKEN_SCOPE);
        String credentials = clientKey + ":" + clientSecret;
        String authorization = "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        Map<?, ?> response = tokenClient.post()
                .uri("/token")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        String token = response == null || response.get("access_token") == null
                ? "" : String.valueOf(response.get("access_token"));
        long expiresIn = numberValue(response == null ? null : response.get("expires_in"), 3600L);
        if (token.isBlank()) {
            throw new IllegalStateException("MVola token endpoint returned an empty access_token");
        }
        cachedToken = new CachedToken(token, Instant.now().plusSeconds(Math.max(1L, expiresIn - 60L)));
        return token;
    }

    private Map<String, Object> parseResponseBody(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalStateException("MVola SDK response did not contain the expected JSON body");
        }
        try {
            return objectMapper.readValue(responseBody, new TypeReference<>() {});
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to parse MVola response JSON", exception);
        }
    }

    private void validatePaymentRequest(PaymentRequest request) {
        if (request == null) throw new IllegalArgumentException("request is required");
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        requireText(request.description(), "description");
        requireText(request.transactionReference(), "transactionReference");
        requireText(request.customerMsisdn(), "customerMsisdn");
        validateConfiguration();
    }

    private void validateConfiguration() {
        if (clientKey.isBlank() || clientSecret.isBlank() || merchantMsisdn.isBlank() || partnerName.isBlank()) {
            throw new IllegalStateException("MVola client credentials, merchant MSISDN and partner name must be configured");
        }
        if (!List.of("FR", "MG").contains(userLanguage)) {
            throw new IllegalStateException("MVOLA_USER_LANGUAGE must be FR or MG");
        }
    }

    private static PostRequestDebitPartyInner party(String msisdn) {
        return keyValue("msisdn", msisdn);
    }

    private static PostRequestDebitPartyInner keyValue(String key, String value) {
        return new PostRequestDebitPartyInner().key(key).value(value);
    }

    private static String stringValue(Map<String, Object> response, String key) {
        Object value = response.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static long numberValue(Object value, long defaultValue) {
        if (value instanceof Number number) return number.longValue();
        if (value != null) {
            try {
                return Long.parseLong(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private static String trimTrailingSlash(String value) {
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
    }

    @FunctionalInterface
    private interface SdkCall {
        ApiResponse<Void> execute(DefaultApi api) throws ApiException;
    }

    public record PaymentRequest(
            BigDecimal amount,
            String description,
            String transactionReference,
            String customerMsisdn,
            String callbackUrl,
            List<KeyValue> metadata
    ) {}

    public record KeyValue(String key, String value) {}

    public record InitiationResponse(String status, String serverCorrelationId, String notificationMethod, int httpStatus) {}

    public record TransactionStatus(
            String status,
            String serverCorrelationId,
            String notificationMethod,
            String objectReference,
            int httpStatus
    ) {}

    public static class MvolaProviderException extends RuntimeException {
        private final int httpStatus;

        public MvolaProviderException(String message, int httpStatus, Throwable cause) {
            super(message, cause);
            this.httpStatus = httpStatus;
        }

        public int getHttpStatus() {
            return httpStatus;
        }
    }

    private record SdkResult(int statusCode, Map<String, Object> body) {}
    private record CachedToken(String value, Instant expiresAt) {}
    private static final class ResponseCapture {
        private String body;
    }
}
