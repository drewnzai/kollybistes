package com.kollybistes.core.util;


import com.kollybistes.common.models.BitcoinWallet;
import com.kollybistes.common.models.User;
import com.kollybistes.core.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class BitcoinRPC {

    @Value("${bitcoin.rpc.user}")
    private String username;

    @Value("${bitcoin.rpc.password}")
    private String password;


    @Value("${system.btc.wallet-name}")
    private String systemWalletName;

    private final AuthService authService;
    private final RestTemplate restTemplate;

    private String sendRequest(String method, Object params, String walletName) {
        String RPC_URL = "http://localhost:18443";

        if (walletName != null && !walletName.isEmpty()) {
            RPC_URL += "/wallet/" + walletName;
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

    public BitcoinWallet createWallet(User user) {
        String method = "createwallet";
        String walletName = user.getUsername();

        Object[] params = { walletName, false, false, "", false, false };
        sendRequest(method, params, null);

        String addressResponse = sendRequest("getnewaddress", new Object[]{walletName}, walletName);
        String address = new JSONObject(addressResponse).getString("result");

        String addressInfoResponse = sendRequest("getaddressinfo", new Object[]{address}, walletName);
        JSONObject addressInfo = new JSONObject(addressInfoResponse).getJSONObject("result");
        String publicKey = addressInfo.getString("pubkey");

        String privateKeyResponse = sendRequest("dumpprivkey", new Object[]{address}, walletName);
        String privateKey = new JSONObject(privateKeyResponse).getString("result");

        BitcoinWallet wallet = new BitcoinWallet();
        wallet.setAddress(address);
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setPrivateKey(privateKey);
        wallet.setPublicKey(publicKey);
        wallet.setCreatedAt(Date.from(Instant.now()));
        wallet.setUser(user);

        return wallet;
    }

    public BigDecimal getTrustedAddressBalance(String walletName) {
        String method = "getbalances";
        String response = sendRequest(method, new Object[]{}, walletName);

        JSONObject jsonResponse = new JSONObject(response);
        JSONObject mine = jsonResponse.getJSONObject("result").getJSONObject("mine");

        BigDecimal balanceBTC = new BigDecimal(mine.get("trusted").toString());

        return balanceBTC;
    }

    public String sendBitcoinFromSystem(String toAddress, BigInteger amountSat, BigInteger feeRate) {
        return sendBitcoinWithCustomFee(systemWalletName, toAddress, amountSat, feeRate);
    }

    public String sendBitcoin(String fromWallet, String toAddress, BigInteger amountSat, BigInteger feeRate) {
        return sendBitcoinWithCustomFee(fromWallet, toAddress, amountSat, feeRate);
    }

    private String sendBitcoinWithCustomFee(String fromWallet, String toAddress, BigInteger amountSat, BigInteger feeRate) {
        // Convert satoshis to BTC for the RPC call (RPC only accepts decimal BTC values)
        BigDecimal amountBTC = new BigDecimal(amountSat).divide(BigDecimal.valueOf(100_000_000L));

        Map<String, Object> outputs = new HashMap<>();
        outputs.put(toAddress, amountBTC);

        String createRawTxResponse = sendRequest("createrawtransaction", new Object[]{
                new JSONArray(),
                outputs
        }, fromWallet);

        String rawTxHex = new JSONObject(createRawTxResponse).getString("result");

        JSONObject options = new JSONObject();
        options.put("fee_rate", feeRate); // fee in sat/vB

        String fundTxResponse = sendRequest("fundrawtransaction", new Object[]{rawTxHex, options}, fromWallet);
        JSONObject fundResult = new JSONObject(fundTxResponse).getJSONObject("result");
        String fundedTxHex = fundResult.getString("hex");

        String signedTxResponse = sendRequest("signrawtransactionwithwallet", new Object[]{fundedTxHex}, fromWallet);
        JSONObject signedResult = new JSONObject(signedTxResponse).getJSONObject("result");
        String signedTxHex = signedResult.getString("hex");

        String sendTxResponse = sendRequest("sendrawtransaction", new Object[]{signedTxHex}, fromWallet);
        return new JSONObject(sendTxResponse).getString("result"); // TXID
    }

    public BigDecimal updateBalance(BitcoinWallet bitcoinWallet){
        return getTrustedAddressBalance(bitcoinWallet
                .getUser()
                .getUsername());
    }

    public String estimateP2WPKHTransactionSize(int inputCount, int outputCount) {
        final int TX_OVERHEAD = 11;             // Version + locktime + input/output counts
        final int P2WPKH_INPUT_SIZE = 68;       // P2WPKH input size in vbytes
        final int P2WPKH_OUTPUT_SIZE = 31;      // P2WPKH output size in vbytes

        int totalSize = TX_OVERHEAD
                + (inputCount * P2WPKH_INPUT_SIZE)
                + (outputCount * P2WPKH_OUTPUT_SIZE);

        return String.valueOf(totalSize); // size in vbytes
    }
}
