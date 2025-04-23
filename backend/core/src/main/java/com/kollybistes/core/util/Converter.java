package com.kollybistes.core.util;

import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

public class Converter {

    private Converter(){
        // Utility class
    }

    public static BigDecimal convertWeiToEth(BigInteger wei){
        return Convert.fromWei(wei.toString(), Convert.Unit.ETHER);
    }

    public static BigInteger convertEthToWei(BigDecimal eth){
        return Convert.toWei(eth, Convert.Unit.ETHER).toBigInteger();
    }

    public static BigInteger convertBtcToSats(BigDecimal btc) {
        return btc.multiply(BigDecimal.valueOf(100_000_000L)).toBigInteger();
    }

    public static BigDecimal convertSatsToBtc(BigInteger sats) {
        return new BigDecimal(sats).divide(BigDecimal.valueOf(100_000_000L), new MathContext(19));
    }

}
