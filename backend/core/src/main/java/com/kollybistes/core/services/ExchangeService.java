package com.kollybistes.core.services;

import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.BigInteger;

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
     */
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
     * Fetches the recommended Ethereum gas price. Comes in gwei hence divided up by 1 Billion
     * (1 ETH = 1,000,000,000 gwei).
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

}

