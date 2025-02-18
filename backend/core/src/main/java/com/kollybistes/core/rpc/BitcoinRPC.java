package com.kollybistes.core.rpc;


import com.kollybistes.common.models.BitcoinWallet;
import com.kollybistes.common.models.User;
import com.kollybistes.core.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class BitcoinRPC {
    @Value("${bitcoin.rpc.user}")
    private String username;
    @Value("${bitcoin.rpc.password}")
    private String password;
    private AuthService authService;
    private final RestTemplate restTemplate;

    private String sendRequest(String method, Object params, String walletName) {
        String RPC_URL = "http://localhost:18443";

        if (walletName != null && !walletName.isEmpty()) {
            RPC_URL += "/wallet/" + walletName; // Append wallet name to URL
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + new String(encodedAuth);
        headers.set("Authorization", authHeader);

        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put("jsonrpc", "1.0");
        jsonRequest.put("id", "1");
        jsonRequest.put("method", method);
        jsonRequest.put("params", params);

        HttpEntity<String> requestEntity = new HttpEntity<>(jsonRequest.toString(), headers);

        ResponseEntity<String> response = restTemplate.exchange(
                RPC_URL,
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        return response.getBody();
    }

    // Create a new wallet
    public BitcoinWallet createWallet() {
        User user = authService.getCurrentUser();

        String method = "createwallet";

//      Creates a Legacy wallet; using descriptor wallets is just a lot of work
        Object[] params = { user.getUsername(), false, false, "", false, false };

        sendRequest(method, params, null);

        String username = user.getUsername();

//         Step 2: Generate a New Address
        String addressResponse = sendRequest("getnewaddress", new Object[]{username},
                username);
        String address = new JSONObject(addressResponse).getString("result");

        // Step 3: Get Public Key
        String addressInfoResponse = sendRequest("getaddressinfo", new Object[]{address}, username);
        JSONObject addressInfo = new JSONObject(addressInfoResponse).getJSONObject("result");
        String publicKey = addressInfo.getString("pubkey");

        // Step 4: Get Private Key
        String privateKeyResponse = sendRequest("dumpprivkey", new Object[]{address}, username);
        String privateKey = new JSONObject(privateKeyResponse).getString("result");

        // Step 5: Create and Save Wallet Entity
        BitcoinWallet wallet = new BitcoinWallet();
        wallet.setAddress(address);
        wallet.setBalance(BigDecimal.valueOf(0));  // Initial balance
        wallet.setPrivateKey(privateKey);
        wallet.setPublicKey(publicKey);
        wallet.setCreatedAt(Date.from(Instant.now()));
        wallet.setUser(user);

        return wallet;
    }

    public BigDecimal getTrustedAddressBalance(String walletName) {
        String method = "getbalances";
        String response = sendRequest(method, new Object[]{}, walletName);

        // Parse JSON response
        JSONObject jsonResponse = new JSONObject(response);
        JSONObject mine = jsonResponse.getJSONObject("result").getJSONObject("mine");

        // Extract the "trusted" balance
        return new BigDecimal(mine.get("trusted").toString());
    }

    public String sendBitcoin(String fromWallet, String toAddress, BigDecimal amount, BigDecimal feeRate) {
        String method = "sendtoaddress";
        Object[] params = { toAddress, amount, "Trade Transfer", "Trade Transfer", false, false, 1, "unset", feeRate };

        String response = sendRequest(method, params, fromWallet);

        JSONObject jsonResponse = new JSONObject(response);

        if (jsonResponse.has("error") && !jsonResponse.isNull("error")) {
            throw new RuntimeException("Bitcoin transfer failed: " + jsonResponse.getJSONObject("error").toString());
        }

        return jsonResponse.getString("result"); // Transaction ID
    }

}
