package com.kollybistes.core.rpc;

import com.kollybistes.core.models.BitcoinWallet;
import com.kollybistes.core.models.User;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class BitcoinRPC {
    @Value("${bitcoin.rpc.user}")
    private String username;
    @Value("${bitcoin.rpc.password}")
    private String password;
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
    public BitcoinWallet createWallet(User user) {
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
        wallet.setBalance(0L);  // Initial balance
        wallet.setPrivateKey(privateKey);
        wallet.setPublicKey(publicKey);
        wallet.setUser(user);

        return wallet;
    }

    public String getAddressBalance(String address, String walletName) {
        String method = "getaddressbalance";
        Object[] params = { address };  // The address for which you want to get the balance
        return sendRequest(method, params, walletName);
    }
}
