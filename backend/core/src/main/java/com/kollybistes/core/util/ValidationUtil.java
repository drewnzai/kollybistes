package com.kollybistes.core.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ValidationUtil {

    private static final String ETHEREUM_ADDRESS_REGEX = "^0x[a-fA-F0-9]{40}$";
    private static final String BITCOIN_ADDRESS_REGEX = "^(bc1|bcrt1|[13])[a-zA-HJ-NP-Z0-9]{25,39}$";

    public static boolean isValidEthereumAddress(String address) {
        return address != null && address.matches(ETHEREUM_ADDRESS_REGEX);
    }

    public static boolean isValidBitcoinAddress(String address) {
        return address != null && address.matches(BITCOIN_ADDRESS_REGEX);
    }
}
