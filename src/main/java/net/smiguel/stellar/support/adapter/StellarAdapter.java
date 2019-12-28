package net.smiguel.stellar.support.adapter;


import net.smiguel.stellar.support.enumerator.StellarAssetType;
import net.smiguel.stellar.support.exception.AdapterException;
import net.smiguel.stellar.support.exception.ServiceException;
import net.smiguel.stellar.support.model.MarketDataItem;
import net.smiguel.stellar.support.model.StellarAccount;
import net.smiguel.stellar.support.model.StellarAccountBalance;
import net.smiguel.stellar.support.model.StellarAssetInfo;
import net.smiguel.stellar.support.model.StellarOffer;
import net.smiguel.stellar.support.model.StellarOperation;
import net.smiguel.stellar.support.model.StellarOperationItem;
import net.smiguel.stellar.support.model.StellarTransaction;

import java.io.IOException;
import java.util.Date;
import java.util.List;


public interface StellarAdapter {

    long getChannelAccount() throws AdapterException;

    void releaseChannelAccount(long channelId) throws AdapterException;

    StellarAccount createAccount(String accountName, String descriptionMemo) throws AdapterException, IOException;

    void createTrustLine(StellarAccount destination, StellarAssetType stellarAssetType, String amount) throws AdapterException, IOException;

    void createTrustLine(StellarAccount source, StellarAccount destination, StellarAssetType stellarAssetType, String amount) throws AdapterException, IOException;

    void createTrustLine(StellarAccount destination, List<StellarOperationItem> stellarOperations) throws AdapterException, IOException;

    void payment(StellarAccount source, StellarAccount destination, StellarAssetType stellarAssetType, String amount, boolean firstDeposit) throws AdapterException, ServiceException, IOException;

    void payment(StellarAccount destination, List<StellarOperationItem> stellarOperations, boolean useIssuerAccount) throws AdapterException, IOException;

    void payment(StellarAccount destination, StellarAssetType stellarAssetType, String amount, boolean firstDeposit) throws AdapterException;

    List<StellarAccountBalance> getBalance(StellarAccount stellarAccount) throws AdapterException, ServiceException, IOException;

    StellarTransaction getTransactionHistory(final StellarAccount stellarAccount, final StellarTransaction transactionDTO) throws AdapterException, ServiceException;

    StellarTransaction getTrades(final StellarAccount stellarAccount, final StellarTransaction transactionDTO) throws AdapterException, ServiceException;

    boolean hasTrades(final Long stellarOfferId) throws AdapterException, ServiceException;

    StellarTransaction getOffers(final StellarAccount stellarAccount, final StellarTransaction transactionDTO) throws AdapterException, ServiceException;

    StellarOffer manageOffer(StellarAccount stellarAccount, StellarOffer offerDTO) throws AdapterException, IOException;

    void deleteOffer(StellarAccount stellarAccount, StellarOffer offerDTO) throws AdapterException, IOException;

    StellarAssetInfo getAssetInformation(String assetCode, String assetIssuerAccountId) throws AdapterException, IOException;

    List<StellarAssetInfo> getAssetInformation(String assetIssuerAccountId) throws AdapterException, IOException;

    List<StellarOperation> prepareCreateTrustLine(StellarAccount destination, StellarAssetType stellarAssetType, String amount, int order) throws AdapterException, IOException;

    List<StellarOperation> prepareCreateTrustLine(StellarAccount source, StellarAccount destination, StellarAssetType stellarAssetType, String amount, int order) throws AdapterException, IOException;

    List<StellarOperation> prepareCreateTrustLine(StellarAccount destination, List<StellarOperationItem> stellarOperations, int order) throws AdapterException, IOException;

    List<StellarOperation> preparePayment(StellarAccount source, StellarAccount destination, StellarAssetType stellarAssetType, String amount, int order) throws AdapterException, ServiceException, IOException;

    List<StellarOperation> preparePayment(StellarAccount destination, List<StellarOperationItem> stellarOperations, boolean useIssuerAccount, int order) throws AdapterException, IOException;

    List<StellarOperation> preparePayment(StellarAccount source, StellarAccount destination, StellarAssetType stellarAssetType, String amount, boolean firstDeposit, int order) throws AdapterException, ServiceException, IOException;

    void submitStellarOperations(List<StellarOperation> stellarOperations) throws AdapterException, IOException;

    List<MarketDataItem> getTradeAggregations(String assetCode, StellarAccount assetIssuer, Date startTime, Date endTime) throws AdapterException, IOException;

    List<StellarOperation> prepareRevokeTrustLine(StellarAccount destination, List<StellarOperationItem> stellarOperations, int order) throws AdapterException, IOException;

}