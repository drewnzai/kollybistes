package com.kollybistes.core.services;

import com.google.common.base.Joiner;
import com.kollybistes.core.dtos.WalletDto;
import com.kollybistes.core.models.BitcoinWallet;
import com.kollybistes.core.models.User;
import com.kollybistes.core.repositories.BitcoinWalletRepository;
import com.kollybistes.core.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.bitcoinj.core.*;
import org.bitcoinj.script.Script;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.MemoryBlockStore;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.Wallet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final BitcoinWalletRepository bitcoinWalletRepository;
    private final UserRepository userRepository;
    private final AuthService authService;

    @Value("${bitcoin.network}")
    private String bitcoinNetwork;

    @Transactional
    public WalletDto createWallet(){
        final NetworkParameters networkParameters = NetworkParameters.fromID(bitcoinNetwork);

        Wallet wallet = Wallet.createDeterministic(networkParameters, Script.ScriptType.P2PKH);
        DeterministicSeed keyChainSeed = wallet.getKeyChainSeed();
        String seedWords = Joiner.on(" ").join(Objects.requireNonNull(keyChainSeed.getMnemonicCode()));

        BitcoinWallet bitcoinWallet = new BitcoinWallet();

        Address address = wallet.currentReceiveAddress();
        ECKey keyFromAddress = wallet.findKeyFromAddress(address);

        String bitcoinAddress = address.toString();
        String privateKey = keyFromAddress.getPrivKey().toString();
        String publicKey = keyFromAddress.getPubKey().toString();

        bitcoinWallet.setAddress(bitcoinAddress);
        bitcoinWallet.setBalance(wallet.getBalance().getValue());
        bitcoinWallet.setPrivateKey(privateKey);
        bitcoinWallet.setPublicKey(publicKey);
        bitcoinWallet.setSeedWords(seedWords);
        bitcoinWallet.setUser(authService.getCurrentUser());

        bitcoinWalletRepository.save(bitcoinWallet);

        return WalletDto.builder()
                .address(bitcoinAddress)
                .balance(bitcoinWallet.getBalance())
                .build();
    }

    public WalletDto getBalance(Long userId) throws BlockStoreException {
        User user = userRepository.findById(userId).get();
        BitcoinWallet bitcoinWallet = bitcoinWalletRepository.findByUser(user);

        NetworkParameters networkParameters = NetworkParameters.fromID(bitcoinNetwork);
        Context.propagate(new Context(networkParameters));
        BigInteger privateKey = new BigInteger(bitcoinWallet.getPrivateKey());
        ECKey ecKey = ECKey.fromPrivate(privateKey);Context.propagate(new Context(networkParameters));
        Wallet wallet = Wallet.fromKeys(networkParameters, List.of(ecKey));
        BlockStore blockStore = new MemoryBlockStore(networkParameters);
        BlockChain chain = new BlockChain(networkParameters, wallet, blockStore);
        PeerGroup peerGroup = new PeerGroup(networkParameters, chain);
        peerGroup.addWallet(wallet);
        peerGroup.startAsync();
        peerGroup.downloadBlockChain();

        bitcoinWallet.setBalance(wallet.getBalance().getValue());

        bitcoinWalletRepository.save(bitcoinWallet);

        return WalletDto.builder()
                .address(bitcoinWallet.getAddress())
                .balance(bitcoinWallet.getBalance())
                .build();
    }

}
