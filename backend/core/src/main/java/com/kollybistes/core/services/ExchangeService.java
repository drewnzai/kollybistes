package com.kollybistes.core.services;

import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExchangeService {

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
     * It is then transformed into BTC (1 BTC = 100,000,000 satoshis) and multiplied by the normal transaction size
     * of 250vB
     */
    public BigDecimal getRecommendedBitcoinFee() {
        try{
            String response = restTemplate.getForObject(bitcoinFeeApiUrl, String.class);
            assert response != null;
            JSONObject jsonResponse = new JSONObject(response);

            return jsonResponse.getJSONObject("data")
                    .getBigDecimal("suggested_transaction_fee_per_byte_sat")
                    .divide(BigDecimal.valueOf(100000000L)).multiply(BigDecimal.valueOf(250L));
        }catch(Exception e){
            throw new RuntimeException("Failed to get the recommended BTC transaction fee");
        }
    }

    /**
     * Fetches the recommended Ethereum gas price in gwei.
     */
    public BigDecimal getRecommendedEthereumGasFee() {

        try{
            String response = restTemplate.getForObject(ethereumGasApiUrl, String.class);
            assert response != null;
            JSONObject jsonResponse = new JSONObject(response);

            return jsonResponse.getJSONObject("data")
                    .getJSONObject("suggested_transaction_fee_gwei_options")
                    .getBigDecimal("cheetah")
                    .divide(BigDecimal.valueOf(1000000000L));
        }catch(Exception e){
            throw new RuntimeException("Failed to get the recommended ETH transaction fee");
        }
    }

    public Object getAllFees(){
        Map<String, BigDecimal> allFees = new HashMap<>();
        allFees.put("BTC", getRecommendedBitcoinFee());
        allFees.put("ETH", getRecommendedEthereumGasFee());

        return allFees;
    }
}

