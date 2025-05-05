package com.kollybistes.core.services;

import com.kollybistes.core.util.Converter;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.BigInteger;

@Service
@RequiredArgsConstructor
public class ExternalApiHandler {

    private final RestTemplate restTemplate;

    @Value("${exchange.api.url}")
    private String exchangeApiUrl;

    @Value("${bitcoin.fee.api.url}")
    private String bitcoinFeeApiUrl;

    @Value("${ethereum.gas.api.url}")
    private String ethereumGasApiUrl;

    /**
     * Fetches the current BTC/ETH exchange rate from an external API.
     */
    @Cacheable(
            value = "recommendations",
            key = "exchangeRate"
    )
    public BigDecimal getBtcToEthExchangeRate() {
        try{
            String response = restTemplate.getForObject(exchangeApiUrl, String.class);
            assert response != null;
            JSONObject jsonResponse = new JSONObject(response);

            return jsonResponse.getJSONObject("bitcoin").getBigDecimal("eth");
        }catch(Exception e){
            throw new RuntimeException("Failed to fetch ETH/BTC exchange rate", e);
        }
    }

    /**
     * Fetches the recommended Bitcoin transaction fee in sat/vB (satoshis per virtual byte).
     */
    @Cacheable(
            value = "recommendations",
            key = "bitcoin"
    )
    public BigInteger getRecommendedBitcoinFee() {
        try{
            String response = restTemplate.getForObject(bitcoinFeeApiUrl, String.class);
            assert response != null;
            JSONObject jsonResponse = new JSONObject(response);

            return jsonResponse.getJSONObject("data")
                    .getBigInteger("suggested_transaction_fee_per_byte_sat");
        }catch(Exception e){
            throw new RuntimeException("Failed to get the recommended BTC transaction fee");
        }
    }

    /**
     * Fetches the recommended Ethereum gas price (in gwei), converts it to wei (1 gwei = 1,000,000,000 wei),
     * and returns it as a BigInteger.
     */
    @Cacheable(
            value = "recommendations",
            key = "ethereum"
    )
    public BigInteger getRecommendedEthereumGasFee() {
        try {
            String response = restTemplate.getForObject(ethereumGasApiUrl, String.class);
            assert response != null;
            JSONObject jsonResponse = new JSONObject(response);

            BigDecimal gasPriceGwei = jsonResponse.getJSONObject("data")
                    .getJSONObject("suggested_transaction_fee_gwei_options")
                    .getBigDecimal("cheetah");

            // Convert gwei to wei: gwei * 1_000_000_000
            return Converter.convertGweiToWei(gasPriceGwei);

        } catch (Exception e) {
            throw new RuntimeException("Failed to get the recommended ETH transaction fee");
        }
    }
}

