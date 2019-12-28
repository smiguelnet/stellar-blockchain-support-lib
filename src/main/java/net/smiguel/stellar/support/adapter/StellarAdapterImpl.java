package net.smiguel.stellar.support.adapter;


import net.smiguel.stellar.support.constants.ApplicationConstants;
import net.smiguel.stellar.support.enumerator.OfferStatus;
import net.smiguel.stellar.support.enumerator.StellarAccountType;
import net.smiguel.stellar.support.enumerator.StellarAssetType;
import net.smiguel.stellar.support.enumerator.StellarTransactionType;
import net.smiguel.stellar.support.exception.AdapterException;
import net.smiguel.stellar.support.exception.ServiceException;
import net.smiguel.stellar.support.helper.DataHelper;
import net.smiguel.stellar.support.helper.HttpHelper;
import net.smiguel.stellar.support.model.MarketDataItem;
import net.smiguel.stellar.support.model.StellarAccount;
import net.smiguel.stellar.support.model.StellarAccountBalance;
import net.smiguel.stellar.support.model.StellarAssetInfo;
import net.smiguel.stellar.support.model.StellarOffer;
import net.smiguel.stellar.support.model.StellarOperation;
import net.smiguel.stellar.support.model.StellarOperationItem;
import net.smiguel.stellar.support.model.StellarTransaction;
import net.smiguel.stellar.support.model.StellarTransactionItem;
import net.smiguel.stellar.support.service.ConfigurationService;
import net.smiguel.stellar.support.util.Util;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.stellar.sdk.Asset;
import org.stellar.sdk.AssetTypeCreditAlphaNum;
import org.stellar.sdk.AssetTypeCreditAlphaNum12;
import org.stellar.sdk.AssetTypeCreditAlphaNum4;
import org.stellar.sdk.AssetTypeNative;
import org.stellar.sdk.ChangeTrustOperation;
import org.stellar.sdk.CreateAccountOperation;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.ManageOfferOperation;
import org.stellar.sdk.Memo;
import org.stellar.sdk.Operation;
import org.stellar.sdk.PaymentOperation;
import org.stellar.sdk.Server;
import org.stellar.sdk.SetOptionsOperation;
import org.stellar.sdk.Transaction;
import org.stellar.sdk.requests.AssetsRequestBuilder;
import org.stellar.sdk.requests.EffectsRequestBuilder;
import org.stellar.sdk.requests.ErrorResponse;
import org.stellar.sdk.requests.OffersRequestBuilder;
import org.stellar.sdk.requests.RequestBuilder;
import org.stellar.sdk.requests.TradeAggregationsRequestBuilder;
import org.stellar.sdk.requests.TradesRequestBuilder;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.AssetResponse;
import org.stellar.sdk.responses.OfferResponse;
import org.stellar.sdk.responses.Page;
import org.stellar.sdk.responses.SubmitTransactionResponse;
import org.stellar.sdk.responses.SubmitTransactionTimeoutResponseException;
import org.stellar.sdk.responses.TradeAggregationResponse;
import org.stellar.sdk.responses.TradeResponse;
import org.stellar.sdk.responses.effects.AccountCreatedEffectResponse;
import org.stellar.sdk.responses.effects.AccountCreditedEffectResponse;
import org.stellar.sdk.responses.effects.AccountDebitedEffectResponse;
import org.stellar.sdk.responses.effects.EffectResponse;
import org.stellar.sdk.responses.effects.TradeEffectResponse;
import org.stellar.sdk.xdr.OperationResult;
import org.stellar.sdk.xdr.TransactionResult;
import org.stellar.sdk.xdr.XdrDataInputStream;
import shadow.okhttp3.HttpUrl;
import shadow.okhttp3.OkHttpClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class StellarAdapterImpl implements StellarAdapter {

    private volatile Server stellarServer;

    private volatile List<StellarAccount> stellarAccounts;

    private static final int MAX_MEMO_LENGTH = 28;
    private static final String LINK_NEXT = "next";
    private final static String LINK_CURRENT = "self";
    private final static String LINK_PREVIOUS = "prev";

    //Used fake data for demonstration purpose
    private ConfigurationService configurationService;

    private OkHttpClient okHttpClient;

    private Server getStellarServer() throws ServiceException {
        if (stellarServer == null) {
            stellarServer = new Server(configurationService.getConfiguration().getStellarServerUrl());
            if (configurationService.getConfiguration().isStellarPublicNetwork()) {
                //Deprecated from 0.3.3
                //Network.usePublicNetwork();
            } else {
                //Deprecated from 0.3.3
                //Network.useTestNetwork();
            }
        }
        return stellarServer;
    }

    public KeyPair getStellarSourceAccount() throws ServiceException {
        return KeyPair.fromSecretSeed(configurationService.getConfiguration().getStellarSourceAccount().getAccountSeed());
    }

    public KeyPair getStellarInflationAccount() throws ServiceException {
        return KeyPair.fromSecretSeed(configurationService.getConfiguration().getStellarInflationAccount().getAccountSeed());
    }

    public KeyPair getStellarCbbDistributionAccount() throws ServiceException {
        return KeyPair.fromSecretSeed(configurationService.getConfiguration().getStellarCbbDistributionAccount().getAccountSeed());
    }

    public KeyPair getStellarCbbIssuerAccount() throws ServiceException {
        return KeyPair.fromSecretSeed(configurationService.getConfiguration().getStellarCbbIssuerAccount().getAccountSeed());
    }

    public KeyPair getStellarCbbEscrowAccount() throws ServiceException {
        return KeyPair.fromSecretSeed(configurationService.getConfiguration().getStellarCbbEscrowAccount().getAccountSeed());
    }

    private void setupChannelAccounts() {
        if (CollectionUtils.isEmpty(configurationService.getConfiguration().getStellarChannelAccounts())) {
            throw new AdapterException("There is no channel account available", null);
        }
        stellarAccounts = Collections.unmodifiableList(configurationService.getConfiguration().getStellarChannelAccounts());
    }

    public static Map<Long, StellarAccount> channelIdPerThread = new ConcurrentHashMap<>();

    @Override
    public long getChannelAccount() throws AdapterException {
        if (stellarAccounts == null) {
            setupChannelAccounts();
        }
        Random random = new Random();
        List<StellarAccount> accountsAvailable = stellarAccounts
                .stream()
                .filter(item -> !channelIdPerThread.containsKey(item.getId()))
                .collect(Collectors.toList());

        StellarAccount stellarAccount = accountsAvailable.get(random.nextInt(accountsAvailable.size()));
        channelIdPerThread.put(stellarAccount.getId(), stellarAccount);
        return stellarAccount.getId();
    }

    @Override
    public void releaseChannelAccount(long channelId) throws AdapterException {
        StellarAccount stellarAccount = channelIdPerThread.get(channelId);
        channelIdPerThread.remove(channelId);
    }

    private KeyPair getChannelKeyPair(long channelId) {
        return KeyPair.fromSecretSeed(channelIdPerThread.get(channelId).getAccountSeed());
    }
    //endregion

    private String addChannelAccountId(long channelId) {
        String hostAddress = HttpHelper.getHostAddress();
        return " [ CID: " + (channelId) + " - TID: " + Thread.currentThread().getId() + " - SRV: " + hostAddress + "] ";
    }

    @Override
    public StellarAccount createAccount(String accountName, String descriptionMemo) throws AdapterException, IOException {
        String operation = "createAccount";
        long channelAccountId = getChannelAccount();

        try {
            KeyPair newAccountKeyPair = KeyPair.random();
            AccountResponse sourceAccount = getStellarServer().accounts().account(getStellarSourceAccount());

            // CHANNEL ACCOUNT
            KeyPair channelKeyPair = getChannelKeyPair(channelAccountId);
            AccountResponse channelAccount = getStellarServer().accounts().account(channelKeyPair);

            Transaction.Builder createAccountTransactionBuilder = new Transaction.Builder(channelAccount);

            CreateAccountOperation createAccountOperation = new CreateAccountOperation.Builder(
                    newAccountKeyPair,
                    configurationService.getConfiguration().getStellarAccountInitFund())
                    .setSourceAccount(getStellarSourceAccount())
                    .build();

            createAccountTransactionBuilder.addOperation(createAccountOperation);

            descriptionMemo = descriptionMemo.trim();
            if (descriptionMemo.length() > MAX_MEMO_LENGTH) {
                descriptionMemo = descriptionMemo.substring(0, (MAX_MEMO_LENGTH - 1));
            }
            createAccountTransactionBuilder.addMemo(Memo.text(descriptionMemo));

            Transaction createAccountTransaction = createAccountTransactionBuilder.build();

            // SIGN: SOURCE + CHANNEL ACCOUNT
            createAccountTransaction.sign(getStellarSourceAccount());
            createAccountTransaction.sign(channelKeyPair);

            SubmitTransactionResponse createAccountResponse = getStellarServer().submitTransaction(createAccountTransaction);
            logTransactionResultData(operation, createAccountResponse.getResultXdr());

            if (createAccountResponse.isSuccess()) {
                StellarAccount stellarAccount = new StellarAccount();
                stellarAccount.setAccountId(newAccountKeyPair.getAccountId());
                stellarAccount.setAccountSeed(new String(newAccountKeyPair.getSecretSeed()));
                stellarAccount.setLedger(createAccountResponse.getLedger());

                stellarAccount.setParentAccountId(configurationService.getConfiguration().getStellarSourceAccount().getId());
                stellarAccount.setAccountType(StellarAccountType.USER_ACCOUNT);
                stellarAccount.setActive(true);
                stellarAccount.setActivationDate(Util.getCurrentDate());
                stellarAccount.setName(accountName);
                stellarAccount.setInsertDate(Util.getCurrentDate());
                stellarAccount.setInsertUsername(ApplicationConstants.APP_USERNAME);

                return stellarAccount;

            } else {
                throw logTransactionResponseAndPrepareAdapterException(createAccountResponse);
            }
        } catch (ServiceException e) {
            throw new AdapterException(e.getMessage(), e);

        } catch (ErrorResponse e) {
            throw new AdapterException(e.getMessage(), e);

        } catch (SubmitTransactionTimeoutResponseException e) {
            throw new AdapterException(ApplicationConstants.STELLAR_TIMEOUT_ERROR, e);

        } finally {
            releaseChannelAccount(channelAccountId);
        }
    }

    @Override
    public void createTrustLine(StellarAccount destination, StellarAssetType stellarAssetType, String amount) throws AdapterException, IOException {
        try {
            createTrustLine(configurationService.getConfiguration().getStellarCbbIssuerAccount(), destination, stellarAssetType, amount);

        } catch (ServiceException e) {
            throw new AdapterException(e.getMessage(), e);
        }
    }

    @Override
    public void createTrustLine(StellarAccount source, StellarAccount destination, StellarAssetType stellarAssetType, String amount) throws AdapterException, IOException {
        createTrustLine(destination, Arrays.asList(
                StellarOperationItem
                        .builder()
                        .stellarAssetType(stellarAssetType)
                        .stellarAssetIssuer(source)
                        .amount(amount)
                        .build()));
    }

    private Asset createNonNativeAsset(StellarOperationItem stellarOperation) {
        return Asset.createNonNativeAsset(
                StellarAssetType.CRYPTOBILIA.getSymbol().equals(stellarOperation.getStellarAssetType().getSymbol())
                        ? stellarOperation.getAssetCode()
                        : stellarOperation.getStellarAssetType().getSymbol(),
                KeyPair.fromSecretSeed(stellarOperation.getStellarAssetIssuer().getAccountSeed()));
    }

    @Override
    public void createTrustLine(StellarAccount destination, List<StellarOperationItem> stellarOperations) throws AdapterException, IOException {
        String operation = "createTrustLine";

        try {
            if (destination == null) {
                destination = stellarOperations.stream().findFirst().get().getStellarAssetDistribution();
            }

            KeyPair destinationKeyPair = KeyPair.fromSecretSeed(destination.getAccountSeed());

            String assetsSymbols = stellarOperations
                    .stream()
                    .filter(item -> item.getAssetCode() != null)
                    .map(item -> item.getAssetCode())
                    .collect(Collectors.joining(", "));

            AccountResponse destinationAccount = getStellarServer().accounts().account(destinationKeyPair);

            Transaction.Builder builder = new Transaction.Builder(destinationAccount);

            stellarOperations
                    .stream()
                    .filter(item ->
                            Arrays
                                    .stream(destinationAccount.getBalances())
                                    .filter(balance ->
                                            (balance != null
                                                    && balance.getAssetCode() != null
                                                    && balance.getAssetCode().equalsIgnoreCase(item.getAssetCode())
                                                    && balance.getAssetIssuer().getAccountId().equalsIgnoreCase(item.getStellarAssetIssuer().getAccountId())))
                                    .count() == 0
                    )
                    .forEach(item -> builder.addOperation(
                            new ChangeTrustOperation
                                    .Builder(createNonNativeAsset(item), item.getStellarAssetType().getLimit())
                                    .setSourceAccount(destinationKeyPair)
                                    .build()));

            if (builder.getOperationsCount() == 0) {
                return;
            }

            builder.addMemo(Memo.text("TL OPS: " + stellarOperations.size()));
            Transaction changeTrustOperation = builder.build();
            changeTrustOperation.sign(destinationKeyPair);

            SubmitTransactionResponse changeTrustOperationResponse = getStellarServer().submitTransaction(changeTrustOperation);
            logTransactionResultData(operation, changeTrustOperationResponse.getResultXdr());

            if (!changeTrustOperationResponse.isSuccess()) {
                throw logTransactionResponseAndPrepareAdapterException(changeTrustOperationResponse);
            }
        } catch (ServiceException e) {
            throw new AdapterException(e.getMessage(), e);

        } catch (ErrorResponse e) {
            throw new AdapterException(e.getMessage(), e);

        } catch (SubmitTransactionTimeoutResponseException e) {
            throw new AdapterException(ApplicationConstants.STELLAR_TIMEOUT_ERROR, e);
        }
    }

    private Asset createAsset(StellarAssetType stellarAssetType, KeyPair issuerKeyPair, String cryptobiliaCode) throws ServiceException {
        switch (stellarAssetType) {
            case CRYPTOBILIA:
                return Asset.createNonNativeAsset(cryptobiliaCode, issuerKeyPair);
            case CBB:
                return Asset.createNonNativeAsset(stellarAssetType.getSymbol(), issuerKeyPair);
            case XLM:
                return Asset.create("native", stellarAssetType.getSymbol(), getStellarSourceAccount().getAccountId());
        }
        throw new ServiceException("Invalid asset type to get distribution account");
    }

    private AdapterException logTransactionResponseAndPrepareAdapterException(SubmitTransactionResponse response) {
        String responseCode = "";
        StringBuilder responseMessage = new StringBuilder();

        if (response.getExtras() != null && response.getExtras().getResultCodes() != null) {
            if (response.getExtras().getResultCodes().getTransactionResultCode() != null) {
                responseCode = response.getExtras().getResultCodes().getTransactionResultCode();
                if (response.getExtras().getResultCodes().getOperationsResultCodes() != null) {
                    response.getExtras().getResultCodes().getOperationsResultCodes().stream().forEach(item -> responseMessage.append(item).append(" "));
                }
            }
        }

        String message = responseCode;
        if (StringUtils.isNotEmpty(responseMessage.toString())) {
            message = responseCode + " (" + responseMessage.toString() + ")";
        }

        AdapterException adapterException = new AdapterException(message, null);
        adapterException.setCode(responseCode);
        adapterException.setMessage(message);
        return adapterException;
    }

    @Override
    public List<StellarAccountBalance> getBalance(StellarAccount stellarAccount) throws AdapterException, ServiceException, IOException {
        try {
            KeyPair pair = KeyPair.fromAccountId(stellarAccount.getAccountId());
            AccountResponse account = getStellarServer().accounts().account(pair);

            List<StellarAccountBalance> stellarAccountBalanceList = Arrays.stream(account.getBalances())
                    .filter(Objects::nonNull)
                    .map(this::mapBalanceResponse)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            final int signers = account.getSigners() != null && account.getSigners().length > 0 ? account.getSigners().length : 0;
            stellarAccountBalanceList.forEach(item -> item.setSigners(signers));

            return stellarAccountBalanceList;

        } catch (ErrorResponse e) {
            throw new AdapterException(e.getMessage(), e);
        }
    }

    private StellarAccountBalance mapBalanceResponse(AccountResponse.Balance balanceResponse) {
        if (balanceResponse == null) {
            return null;
        }
        StellarAssetType stellarAssetType;
        String assetDescription;
        if ("credit_alphanum12".equalsIgnoreCase(balanceResponse.getAssetType())) {
            stellarAssetType = StellarAssetType.CRYPTOBILIA;
            assetDescription = balanceResponse.getAssetCode();

        } else if ("credit_alphanum4".equalsIgnoreCase(balanceResponse.getAssetType())) {
            stellarAssetType = StellarAssetType.CBB;
            assetDescription = balanceResponse.getAssetCode();

        } else if ("native".equalsIgnoreCase(balanceResponse.getAssetType())) {
            stellarAssetType = StellarAssetType.XLM;
            assetDescription = StellarAssetType.XLM.getSymbol();

        } else {
            assetDescription = !StringUtils.isEmpty(balanceResponse.getAssetCode()) ? balanceResponse.getAssetCode() : balanceResponse.getAssetType();
            stellarAssetType = StellarAssetType.fromName(assetDescription);
        }

        StellarAccountBalance stellarAccountBalance = new StellarAccountBalance();
        stellarAccountBalance.setAssetType(stellarAssetType);
        stellarAccountBalance.setIssuerAccountId(!StellarAssetType.XLM.getName().equals(balanceResponse.getAssetType()) ? balanceResponse.getAssetIssuer().getAccountId() : null);
        stellarAccountBalance.setAssetTypeDescription(assetDescription);
        stellarAccountBalance.setBalance(balanceResponse.getBalance());
        return stellarAccountBalance;
    }

    @Override
    public void payment(StellarAccount source, StellarAccount destination, StellarAssetType stellarAssetType, String amount, boolean firstDeposit) throws AdapterException, ServiceException, IOException {
        String operation = "payment";
        try {
            KeyPair sourceKeyPair = KeyPair.fromSecretSeed(source.getAccountSeed());
            KeyPair destinationKeyPair = KeyPair.fromAccountId(destination.getAccountId());

            AccountResponse sourceAccount = getStellarServer().accounts().account(sourceKeyPair);
            Transaction.Builder builder;

            Asset asset;
            if (stellarAssetType.equals(StellarAssetType.XLM)) {
                //XLM FROM SOURCE ACCOUNT
                asset = Asset.create(stellarAssetType.getName(), stellarAssetType.getSymbol(), configurationService.getConfiguration().getStellarSourceAccount().getAccountSeed());

            } else if (stellarAssetType.equals(StellarAssetType.CBB)) {
                // CBB FROM DIST ACCOUNT
                asset = Asset.createNonNativeAsset(stellarAssetType.getSymbol(), getStellarCbbIssuerAccount());

                //TODO if CBB Distribution Account has no funds, then get from ISSUER Account
                //region funding CBB DIST ACCOUNT
                Double cbbDistributionAccountBalance = Arrays.stream(sourceAccount.getBalances())
                        .filter(item ->
                                StellarAssetType.CBB.getSymbol().equalsIgnoreCase(item.getAssetCode())
                                        && item.getAssetIssuer().getAccountId().equalsIgnoreCase(getStellarCbbIssuerAccount().getAccountId()))
                        .map(item -> item.getBalance())
                        .filter(Objects::nonNull)
                        .map(Double::valueOf)
                        .findFirst().orElse(0d);

                if (cbbDistributionAccountBalance < Double.valueOf(amount)) {

                    if (!hasTrustlineForAsset(sourceAccount,
                            stellarAssetType.getSymbol(),
                            configurationService.getConfiguration().getStellarCbbIssuerAccount().getAccountId())) {
                        createTrustLine(source,
                                Arrays.asList(
                                        StellarOperationItem
                                                .builder()
                                                .stellarAssetType(stellarAssetType)
                                                .assetCode(stellarAssetType.getSymbol())
                                                .stellarAssetIssuer(configurationService.getConfiguration().getStellarCbbIssuerAccount())
                                                .amount(amount)
                                                .build()));
                    }

                    KeyPair issuerPair = KeyPair.fromSecretSeed(configurationService.getConfiguration().getStellarCbbIssuerAccount().getAccountSeed());
                    AccountResponse issuerAccount = getStellarServer().accounts().account(issuerPair);

                    builder = new Transaction.Builder(issuerAccount);
                    builder.addOperation(new PaymentOperation.Builder(sourceKeyPair, asset, amount).build());
                    builder.addMemo(Memo.text("PA RCH " + amount));

                    Transaction paymentOperation = builder.build();
                    paymentOperation.sign(issuerPair);

                    SubmitTransactionResponse paymentOperationResponse = getStellarServer().submitTransaction(paymentOperation);
                    logTransactionResultData(operation, paymentOperationResponse.getResultXdr());
                    if (!paymentOperationResponse.isSuccess()) {
                        throw logTransactionResponseAndPrepareAdapterException(paymentOperationResponse);
                    }
                }
                //endregion

            } else {
                asset = Asset.createNonNativeAsset(stellarAssetType.getSymbol(), getStellarCbbIssuerAccount());
            }

            sourceAccount = getStellarServer().accounts().account(sourceKeyPair);
            builder = new Transaction.Builder(sourceAccount);

            if (firstDeposit) {
                Asset firstLumenDeposit = Asset.create(StellarAssetType.XLM.getName(), StellarAssetType.XLM.getSymbol(), configurationService.getConfiguration().getStellarSourceAccount().getAccountSeed());
                String firstLumenDepositValue = configurationService.getConfiguration().getStellarAccountFirstDeposit();
                builder.addOperation(new PaymentOperation.Builder(destinationKeyPair, firstLumenDeposit, firstLumenDepositValue).build());
            }

            builder.addOperation(new PaymentOperation.Builder(destinationKeyPair, asset, amount).setSourceAccount(sourceKeyPair).build());
            builder.addMemo(Memo.text("PA " + amount));

            Transaction paymentOperation = builder.build();
            paymentOperation.sign(sourceKeyPair);

            SubmitTransactionResponse paymentOperationResponse = getStellarServer().submitTransaction(paymentOperation);
            logTransactionResultData(operation, paymentOperationResponse.getResultXdr());
            if (!paymentOperationResponse.isSuccess()) {
                throw logTransactionResponseAndPrepareAdapterException(paymentOperationResponse);
            }

        } catch (ServiceException e) {
            throw new AdapterException(e.getMessage(), e);

        } catch (ErrorResponse e) {
            throw new AdapterException(e.getMessage(), e);

        } catch (IOException e) {
            throw new AdapterException(e.getMessage(), e);

        } catch (SubmitTransactionTimeoutResponseException e) {
            throw new AdapterException(ApplicationConstants.STELLAR_TIMEOUT_ERROR, e);
        }
    }

    private String getOperationsData(Operation[] operations) {
        if (operations == null) {
            return "";
        }
        return Arrays.stream(operations).map(item -> item.toXdr().toString()).collect(Collectors.joining(" - "));
    }

    @Override
    public void payment(StellarAccount destination, StellarAssetType stellarAssetType, String amount, boolean firstDeposit) throws AdapterException {
        try {
            payment(configurationService.getConfiguration().getStellarCbbDistributionAccount(), destination, stellarAssetType, amount, firstDeposit);

        } catch (ServiceException e) {
            throw new AdapterException(e.getMessage(), e);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void payment(StellarAccount destination, List<StellarOperationItem> stellarOperations, boolean useIssuerAccount) throws AdapterException, IOException {
        String operation = "payment";

        try {
            if (destination == null) {
                destination = stellarOperations.stream().findFirst().get().getStellarAssetDistribution();
            }

            Set<String> accountSeeds = stellarOperations.stream().map(item ->
                    useIssuerAccount ? item.getStellarAssetIssuer().getAccountSeed() : item.getStellarAssetDistribution().getAccountSeed()).collect(Collectors.toSet());

            Set<KeyPair> sourceKeyPairSet = accountSeeds
                    .stream()
                    .map(item -> KeyPair.fromSecretSeed(item))
                    .collect(Collectors.toSet());

            KeyPair sourceKeyPair = sourceKeyPairSet.stream().findFirst().get();


            KeyPair destinationKeyPair = KeyPair.fromAccountId(destination.getAccountId());

            String assetsSymbols = stellarOperations
                    .stream()
                    .filter(item -> item.getAssetCode() != null)
                    .map(item -> item.getAssetCode())
                    .collect(Collectors.joining(", "));

            AccountResponse sourceAccount = getStellarServer().accounts().account(sourceKeyPair);

            Transaction.Builder paymentBuilder = new Transaction.Builder(sourceAccount);

            stellarOperations.forEach(item ->
                    paymentBuilder.addOperation(
                            new PaymentOperation.Builder(destinationKeyPair, createNonNativeAsset(item), item.getAmount())
                                    .setSourceAccount(sourceKeyPair)
                                    .build()));

            paymentBuilder.addMemo(Memo.text("PA OPS: " + stellarOperations.size()));

            // ADD SIGNER - SOURCE ACCOUNT
            //paymentBuilder.addOperation(getOptionsOperation(sourceKeyPair, channelKeyPair));

            Transaction paymentOperation = paymentBuilder.build();

            //TODO Add multisignature
            //sourceKeyPairSet.forEach(item -> paymentOperation.sign(item));
            paymentOperation.sign(sourceKeyPair);

            SubmitTransactionResponse paymentOperationResponse = getStellarServer().submitTransaction(paymentOperation);
            logTransactionResultData(operation, paymentOperationResponse.getResultXdr());
            if (!paymentOperationResponse.isSuccess()) {
                throw logTransactionResponseAndPrepareAdapterException(paymentOperationResponse);
            }

        } catch (ServiceException e) {
            throw new AdapterException(e.getMessage(), e);

        } catch (ErrorResponse e) {
            throw new AdapterException(e.getMessage(), e);

        } catch (IOException e) {
            throw new AdapterException(e.getMessage(), e);

        } catch (SubmitTransactionTimeoutResponseException e) {
            throw new AdapterException(ApplicationConstants.STELLAR_TIMEOUT_ERROR, e);
        }
    }

    private SetOptionsOperation getOptionsOperation(KeyPair sourcekeyPair, KeyPair signerkeyPair) {
        return new SetOptionsOperation.Builder()
                .setSourceAccount(sourcekeyPair)
                .setSigner(signerkeyPair.getXdrSignerKey(), 1)
                .setMasterKeyWeight(2)
                .setLowThreshold(1)
                .setMediumThreshold(2)
                .setHighThreshold(2)
                .build();
    }

    @Override
    public StellarTransaction getOffers(StellarAccount stellarAccount, StellarTransaction transactionDTO) throws AdapterException, ServiceException {
        try {
            Page<OfferResponse> offerResponsePage;

            if (transactionDTO == null) {
                KeyPair accountKeyPair = KeyPair.fromAccountId(stellarAccount.getAccountId());
                OffersRequestBuilder offersRequestBuilder = getStellarServer().offers().forAccount(accountKeyPair);
                offersRequestBuilder.order(RequestBuilder.Order.DESC);
                offersRequestBuilder.limit(ApplicationConstants.STELLAR_API_SEARCH_LIMIT);
                offerResponsePage = offersRequestBuilder.execute();

            } else {
                String url = transactionDTO.getPageCurrent();
                if (transactionDTO.isMoveBack() && !StringUtils.isEmpty(transactionDTO.getPagePrevious())) {
                    url = transactionDTO.getPagePrevious();
                }
                if (transactionDTO.isMoveForward() && !StringUtils.isEmpty(transactionDTO.getPageNext())) {
                    url = transactionDTO.getPageNext();
                }
                offerResponsePage = OffersRequestBuilder.execute(okHttpClient, HttpUrl.parse(url));
            }

            String pageCurrent = getLinksFromOfferHistory(offerResponsePage, LINK_CURRENT);
            String pageNext = getLinksFromOfferHistory(offerResponsePage, LINK_NEXT);
            String pagePrevious = getLinksFromOfferHistory(offerResponsePage, LINK_PREVIOUS);

            if (pageNext.equalsIgnoreCase(pageCurrent)) {
                pageNext = null;
            }

            if (pagePrevious.equalsIgnoreCase(pageCurrent)) {
                pagePrevious = null;
            }

            if (CollectionUtils.isEmpty(offerResponsePage.getRecords()) || offerResponsePage.getRecords().size() < ApplicationConstants.STELLAR_API_SEARCH_LIMIT) {
                pageNext = null;
            }

            int requestPerAccount = transactionDTO != null ? transactionDTO.getRequestPerAccount() : 0;

            return StellarTransaction.builder()
                    .pageCurrent(pageCurrent)
                    .pageNext(pageNext)
                    .hasNext(pageNext != null && !pageCurrent.equals(pageNext))
                    .pagePrevious(pagePrevious)
                    .items(getItemsFromOfferHistory(offerResponsePage))
                    .requestPerAccount(++requestPerAccount)
                    .build();

        } catch (ErrorResponse e) {
            throw new AdapterException(e.getMessage(), e);

        } catch (IOException e) {
            throw new AdapterException(e.getMessage(), e);

        } catch (SubmitTransactionTimeoutResponseException e) {
            throw new AdapterException(ApplicationConstants.STELLAR_TIMEOUT_ERROR, e);
        }
    }

    @Override
    public boolean hasTrades(Long stellarOfferId) throws AdapterException, ServiceException {
        final String operation = "hasTrades";
        try {
            TradesRequestBuilder tradesRequestBuilder = getStellarServer().trades().offerId(stellarOfferId.toString());
            tradesRequestBuilder.limit(ApplicationConstants.STELLAR_API_SEARCH_LIMIT);

            Page<TradeResponse> tradeResponsePage = tradesRequestBuilder.execute();
            return tradeResponsePage != null && !CollectionUtils.isEmpty(tradeResponsePage.getRecords());

        } catch (ErrorResponse e) {
            throw new AdapterException(e.getMessage(), e);

        } catch (IOException e) {
            throw new AdapterException(e.getMessage(), e);

        } catch (SubmitTransactionTimeoutResponseException e) {
            throw new AdapterException(ApplicationConstants.STELLAR_TIMEOUT_ERROR, e);
        }
    }

    @Override
    public StellarTransaction getTrades(StellarAccount stellarAccount, StellarTransaction transactionDTO) throws AdapterException, ServiceException {
        try {
            Page<TradeResponse> tradeResponsePage;

            if (transactionDTO == null) {
                KeyPair accountKeyPair = KeyPair.fromAccountId(stellarAccount.getAccountId());
                TradesRequestBuilder tradesRequestBuilder = getStellarServer().trades().forAccount(accountKeyPair);
                tradesRequestBuilder.order(RequestBuilder.Order.DESC);
                tradesRequestBuilder.limit(ApplicationConstants.STELLAR_API_SEARCH_LIMIT);
                tradeResponsePage = tradesRequestBuilder.execute();

            } else {
                String url = transactionDTO.getPageCurrent();
                if (transactionDTO.isMoveBack() && !StringUtils.isEmpty(transactionDTO.getPagePrevious())) {
                    url = transactionDTO.getPagePrevious();
                }
                if (transactionDTO.isMoveForward() && !StringUtils.isEmpty(transactionDTO.getPageNext())) {
                    url = transactionDTO.getPageNext();
                }
                tradeResponsePage = TradesRequestBuilder.execute(okHttpClient, HttpUrl.parse(url));
            }

            String pageCurrent = getLinksFromTradeHistory(tradeResponsePage, LINK_CURRENT);
            String pageNext = getLinksFromTradeHistory(tradeResponsePage, LINK_NEXT);
            String pagePrevious = getLinksFromTradeHistory(tradeResponsePage, LINK_PREVIOUS);

            if (pageNext.equalsIgnoreCase(pageCurrent)) {
                pageNext = null;
            }

            if (pagePrevious.equalsIgnoreCase(pageCurrent)) {
                pagePrevious = null;
            }

            if (CollectionUtils.isEmpty(tradeResponsePage.getRecords()) || tradeResponsePage.getRecords().size() < ApplicationConstants.STELLAR_API_SEARCH_LIMIT) {
                pageNext = null;
            }

            int requestPerAccount = transactionDTO != null ? transactionDTO.getRequestPerAccount() : 0;

            return StellarTransaction.builder()
                    .pageCurrent(pageCurrent)
                    .pageNext(pageNext)
                    .hasNext(pageNext != null && !pageCurrent.equals(pageNext))
                    .pagePrevious(pagePrevious)
                    .items(getItemsFromTradeHistory(tradeResponsePage, stellarAccount.getAccountId()))
                    .requestPerAccount(++requestPerAccount)
                    .build();

        } catch (ErrorResponse e) {
            throw new AdapterException(e.getMessage(), e);

        } catch (IOException e) {
            throw new AdapterException(e.getMessage(), e);

        } catch (SubmitTransactionTimeoutResponseException e) {
            throw new AdapterException(ApplicationConstants.STELLAR_TIMEOUT_ERROR, e);
        }
    }

    @Override
    public StellarAssetInfo getAssetInformation(String assetCode, String assetIssuerAccountId) throws AdapterException, IOException {
        AssetsRequestBuilder assetsRequestBuilder = getStellarServer()
                .assets()
                .assetCode(assetCode)
                .assetIssuer(assetIssuerAccountId);

        Page<AssetResponse> execute = assetsRequestBuilder.execute();
        if (execute == null) {
            return null;
        }

        return execute.getRecords()
                .stream()
                .filter(Objects::nonNull)
                .filter(item -> NumberUtils.isCreatable(item.getAmount()))
                .map(item ->
                        StellarAssetInfo.builder()
                                .amount(DataHelper.valueStropInCbb(DataHelper.valueStrop(item.getAmount())).toPlainString())
                                .accounts(item.getNumAccounts())
                                .build()
                ).findFirst()
                .orElse(null);
    }

    @Override
    public List<StellarAssetInfo> getAssetInformation(String assetIssuerAccountId) throws AdapterException, IOException {
        throw new NotImplementedException("Method is still not implemented");
    }

    @Override
    public List<MarketDataItem> getTradeAggregations(String assetCode, StellarAccount assetIssuer, Date startTime, Date endTime) throws AdapterException, IOException {
        String operation = "getTradeAggregations";
        try {
            final long timeScale1h = 3600000;

            List<MarketDataItem> candleChartDataList = new ArrayList<>();

            //CBB
            Asset assetCBB = createAsset(StellarAssetType.CBB, getStellarCbbIssuerAccount(), StellarAssetType.CBB.getSymbol());

            //ICON
            KeyPair issuerKeyPair = KeyPair.fromAccountId(assetIssuer.getAccountId());
            Asset assetICON = createAsset(StellarAssetType.CRYPTOBILIA, issuerKeyPair, assetCode);

            TradeAggregationsRequestBuilder tradeAggregationsRequestBuilder = getStellarServer()
                    .tradeAggregations(assetICON, assetCBB, startTime.getTime(), endTime.getTime(), timeScale1h, 100);

            Page<TradeAggregationResponse> execute = tradeAggregationsRequestBuilder.execute();

            if (execute != null && !CollectionUtils.isEmpty(execute.getRecords())) {
                candleChartDataList = execute
                        .getRecords()
                        .stream()
                        .map(item -> MarketDataItem.builder()
                                .volume(DataHelper.valueStropInCbb(DataHelper.valueStrop(item.getBaseVolume())).longValue())
                                .date(Util.getDate(item.getTimestamp()))
                                .closePrice(DataHelper.valueCbb(item.getClose()))
                                .openPrice(DataHelper.valueCbb(item.getOpen()))
                                .lowPrice(DataHelper.valueCbb(item.getLow()))
                                .highPrice(DataHelper.valueCbb(item.getHigh()))
                                .count(item.getTradeCount())
                                .build()).collect(Collectors.toList());
            }

            return candleChartDataList;

        } catch (ErrorResponse e) {
            throw new AdapterException(e.getMessage(), e);

        } catch (IOException e) {
            throw new AdapterException(e.getMessage(), e);

        } catch (SubmitTransactionTimeoutResponseException e) {
            throw new AdapterException(ApplicationConstants.STELLAR_TIMEOUT_ERROR, e);
        }
    }

    @Override
    public StellarTransaction getTransactionHistory(final StellarAccount stellarAccount, final StellarTransaction transactionDTO) throws AdapterException, ServiceException {
        try {
            Page<EffectResponse> effectResponsePage;

            if (transactionDTO == null) {
                KeyPair accountKeyPair = KeyPair.fromAccountId(stellarAccount.getAccountId());
                EffectsRequestBuilder effectsRequestBuilder = getStellarServer().effects().forAccount(accountKeyPair);
                effectsRequestBuilder.order(RequestBuilder.Order.DESC);
                effectsRequestBuilder.limit(ApplicationConstants.STELLAR_API_SEARCH_LIMIT);
                effectResponsePage = effectsRequestBuilder.execute();

            } else {
                String url = transactionDTO.getPageCurrent();
                if (transactionDTO.isMoveBack() && !StringUtils.isEmpty(transactionDTO.getPagePrevious())) {
                    url = transactionDTO.getPagePrevious();
                }
                if (transactionDTO.isMoveForward() && !StringUtils.isEmpty(transactionDTO.getPageNext())) {
                    url = transactionDTO.getPageNext();
                }
                effectResponsePage = EffectsRequestBuilder.execute(okHttpClient, HttpUrl.parse(url));
            }

            String pageCurrent = getLinksFromTransactionHistory(effectResponsePage, LINK_CURRENT);
            String pageNext = getLinksFromTransactionHistory(effectResponsePage, LINK_NEXT);
            String pagePrevious = getLinksFromTransactionHistory(effectResponsePage, LINK_PREVIOUS);

            if (pageNext.equalsIgnoreCase(pageCurrent)) {
                pageNext = null;
            }

            if (pagePrevious.equalsIgnoreCase(pageCurrent)) {
                pagePrevious = null;
            }

            if (CollectionUtils.isEmpty(effectResponsePage.getRecords()) || effectResponsePage.getRecords().size() < ApplicationConstants.STELLAR_API_SEARCH_LIMIT) {
                pageNext = null;
            }

            int requestPerAccount = transactionDTO != null ? transactionDTO.getRequestPerAccount() : 0;

            return StellarTransaction.builder()
                    .pageCurrent(pageCurrent)
                    .pageNext(pageNext)
                    .hasNext(pageNext != null && !pageCurrent.equals(pageNext))
                    .pagePrevious(pagePrevious)
                    .items(getItemsFromTransactionHistory(effectResponsePage))
                    .requestPerAccount(++requestPerAccount)
                    .build();

        } catch (ErrorResponse e) {
            throw new AdapterException(e.getMessage(), e);

        } catch (IOException e) {
            throw new AdapterException(e.getMessage(), e);

        } catch (SubmitTransactionTimeoutResponseException e) {
            throw new AdapterException(ApplicationConstants.STELLAR_TIMEOUT_ERROR, e);
        }
    }

    private List<StellarTransactionItem> getItemsFromOfferHistory(Page<OfferResponse> offerResponsePage) {
        if (offerResponsePage == null || offerResponsePage.getRecords() == null) {
            return null;
        }
        return offerResponsePage
                .getRecords()
                .stream()
                .filter(Objects::nonNull)
                .map(this::getItemFromOfferHistory)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private StellarTransactionItem getItemFromOfferHistory(OfferResponse offerResponseItem) {
        StellarTransactionItem stellarTransactionItem = StellarTransactionItem.builder()
                .id(String.valueOf(offerResponseItem.getId()))
                .transactionType(StellarTransactionType.OFFER)
                .offerAmount(offerResponseItem.getAmount())
                .offerPrice(offerResponseItem.getPrice())
                .offerBuyingAsset(getAssetCode(offerResponseItem.getBuying()))
                .offerSellingAsset(getAssetCode(offerResponseItem.getSelling()))
                .offerSellerAccount(offerResponseItem.getSeller() != null ? offerResponseItem.getSeller().getAccountId() : null)
                .pagingToken(offerResponseItem.getPagingToken())
                .build();
        return stellarTransactionItem;
    }

    private List<StellarTransactionItem> getItemsFromTradeHistory(Page<TradeResponse> tradeResponsePage, String accountId) {
        if (tradeResponsePage == null || tradeResponsePage.getRecords() == null) {
            return null;
        }
        return tradeResponsePage
                .getRecords()
                .stream()
                .filter(Objects::nonNull)
                .map(item -> getItemFromTradeHistory(item, accountId))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }


    private List<StellarTransactionItem> getItemsFromTransactionHistory(Page<EffectResponse> effectResponsePage) {
        if (effectResponsePage == null || effectResponsePage.getRecords() == null) {
            return null;
        }
        return effectResponsePage
                .getRecords()
                .stream()
                .filter(Objects::nonNull)
                .map(this::getItemFromTransactionHistory)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private StellarTransactionItem getItemFromTradeHistory(TradeResponse tradeResponseItem, String accountId) {
        StellarTransactionItem stellarTransactionItem = StellarTransactionItem.builder()
                .id(String.valueOf(tradeResponseItem.getId()))
                .transactionType(StellarTransactionType.TRADE)
                .timestamp(Util.getDateFromStellarApi(tradeResponseItem.getLedgerCloseTime()))
                .pagingToken(tradeResponseItem.getPagingToken())
                .tradeOfferId(tradeResponseItem.getOfferId())
                .build();

        if (stellarTransactionItem.getTimestamp() != null) {
            stellarTransactionItem.setDate(Util.getDateFromStellarDate(stellarTransactionItem.getTimestamp()));
            stellarTransactionItem.setTime(Util.getTimeFromStellarDate(stellarTransactionItem.getTimestamp()));
        }

        if (tradeResponseItem.getBaseAccount().getAccountId().equals(accountId)) {
            // FROM
            stellarTransactionItem.setOfferSellerAccount(tradeResponseItem.getBaseAccount().getAccountId());
            stellarTransactionItem.setTradeSeller(tradeResponseItem.getCounterAccount().getAccountId());

            stellarTransactionItem.setOfferBuyingAsset(getAssetCode(tradeResponseItem.getBaseAsset()));
            stellarTransactionItem.setTradeSoldAmount(tradeResponseItem.getBaseAmount());
            stellarTransactionItem.setTradeSoldAsset(getAssetCode(tradeResponseItem.getBaseAsset()));

            stellarTransactionItem.setOfferSellingAsset(getAssetCode(tradeResponseItem.getCounterAsset()));
            stellarTransactionItem.setTradeBoughtAmount(tradeResponseItem.getCounterAmount());
            stellarTransactionItem.setTradeBoughtAsset(getAssetCode(tradeResponseItem.getCounterAsset()));

            stellarTransactionItem.setOfferAmount(tradeResponseItem.getBaseAmount());
            stellarTransactionItem.setOfferPrice(DataHelper.valueStropInCbb(DataHelper.valueStrop(tradeResponseItem.getBaseAmount())).toPlainString());

        } else {
            // TO
            stellarTransactionItem.setOfferSellerAccount(tradeResponseItem.getCounterAccount().getAccountId());
            stellarTransactionItem.setTradeSeller(tradeResponseItem.getBaseAccount().getAccountId());

            stellarTransactionItem.setOfferBuyingAsset(getAssetCode(tradeResponseItem.getCounterAsset()));
            stellarTransactionItem.setTradeSoldAmount(tradeResponseItem.getCounterAmount());
            stellarTransactionItem.setTradeSoldAsset(getAssetCode(tradeResponseItem.getCounterAsset()));

            stellarTransactionItem.setOfferSellingAsset(getAssetCode(tradeResponseItem.getBaseAsset()));
            stellarTransactionItem.setTradeBoughtAmount(tradeResponseItem.getBaseAmount());
            stellarTransactionItem.setTradeBoughtAsset(getAssetCode(tradeResponseItem.getBaseAsset()));

            stellarTransactionItem.setOfferAmount(tradeResponseItem.getCounterAmount());
            stellarTransactionItem.setOfferPrice(DataHelper.valueStropInCbb(DataHelper.valueStrop(tradeResponseItem.getCounterAmount())).toPlainString());
        }
        return stellarTransactionItem;
    }

    private StellarTransactionItem getItemFromTransactionHistory(EffectResponse effectResponseItem) {
        StellarTransactionItem stellarTransactionItem = StellarTransactionItem.builder()
                .id(effectResponseItem.getId())
                .transactionType(StellarTransactionType.fromName(effectResponseItem.getType()))
                .accountId(effectResponseItem.getAccount().getAccountId())
                .timestamp(Util.getDateFromStellarApi(effectResponseItem.getCreatedAt()))
                .pagingToken(effectResponseItem.getPagingToken())
                .build();

        if (stellarTransactionItem.getTimestamp() != null) {
            stellarTransactionItem.setDate(Util.getDateFromStellarDate(stellarTransactionItem.getTimestamp()));
            stellarTransactionItem.setTime(Util.getTimeFromStellarDate(stellarTransactionItem.getTimestamp()));
        }

        if (effectResponseItem instanceof AccountCreatedEffectResponse) {
            AccountCreatedEffectResponse accountCreatedEffectResponse = (AccountCreatedEffectResponse) effectResponseItem;
            stellarTransactionItem.setStartingBalance(accountCreatedEffectResponse.getStartingBalance());
            stellarTransactionItem.setAssetType(StellarAssetType.XLM.getName());
            stellarTransactionItem.setAssetCode(StellarAssetType.XLM.getSymbol());
        }

        if (effectResponseItem instanceof AccountCreditedEffectResponse) {
            AccountCreditedEffectResponse accountCreditedEffectResponse = (AccountCreditedEffectResponse) effectResponseItem;
            stellarTransactionItem.setAssetType(getAssetType(accountCreditedEffectResponse.getAsset()));
            stellarTransactionItem.setAssetCode(getAssetCode(accountCreditedEffectResponse.getAsset()));
            stellarTransactionItem.setAmount(accountCreditedEffectResponse.getAmount());
        }

        if (effectResponseItem instanceof AccountDebitedEffectResponse) {
            AccountDebitedEffectResponse accountDebitedEffectResponse = (AccountDebitedEffectResponse) effectResponseItem;
            stellarTransactionItem.setAssetType(getAssetType(accountDebitedEffectResponse.getAsset()));
            stellarTransactionItem.setAssetCode(getAssetCode(accountDebitedEffectResponse.getAsset()));
            stellarTransactionItem.setAmount(accountDebitedEffectResponse.getAmount());
        }

        if (effectResponseItem instanceof TradeEffectResponse) {
            TradeEffectResponse tradeEffectResponse = (TradeEffectResponse) effectResponseItem;
            stellarTransactionItem.setTradeBoughtAsset(getAssetCode(tradeEffectResponse.getBoughtAsset()));
            stellarTransactionItem.setAmount(tradeEffectResponse.getBoughtAmount());
            stellarTransactionItem.setTradeOfferId(tradeEffectResponse.getOfferId() != null ? String.valueOf(tradeEffectResponse.getOfferId()) : null);
            stellarTransactionItem.setTradeSeller(tradeEffectResponse.getSeller().getAccountId());
            stellarTransactionItem.setTradeSoldAsset(getAssetCode(tradeEffectResponse.getSoldAsset()));
            stellarTransactionItem.setTradeSoldAmount(tradeEffectResponse.getSoldAmount());

            stellarTransactionItem.setAssetType(getAssetType(tradeEffectResponse.getSoldAsset()));
            stellarTransactionItem.setAssetCode(getAssetCode(tradeEffectResponse.getSoldAsset()));
        }
        return stellarTransactionItem;
    }

    private String getAssetType(Asset asset) {
        if (asset == null) {
            return null;
        }
        if (asset instanceof AssetTypeCreditAlphaNum4) {
            return getAssetCode(asset);
        }
        if (asset instanceof AssetTypeCreditAlphaNum12) {
            return StellarAssetType.CRYPTOBILIA.getSymbol();
        }
        return asset.getType();
    }

    private String getAssetCode(Asset asset) {
        if (asset == null) {
            return null;
        }
        if (asset instanceof AssetTypeCreditAlphaNum) {
            return ((AssetTypeCreditAlphaNum) asset).getCode();
        }

        if (asset instanceof AssetTypeNative) {
            return StellarAssetType.XLM.getSymbol();
        }
        return asset.getType();
    }

    private String getLinksFromTradeHistory(Page<TradeResponse> tradeResponsePage, String type) {
        if (tradeResponsePage == null || tradeResponsePage.getLinks() == null) {
            return null;
        }
        return getResponse(type, tradeResponsePage.getLinks());
    }

    private String getLinksFromOfferHistory(Page<OfferResponse> offerResponsePage, String type) {
        if (offerResponsePage == null || offerResponsePage.getLinks() == null) {
            return null;
        }
        return getResponse(type, offerResponsePage.getLinks());
    }

    private String getLinksFromTransactionHistory(Page<EffectResponse> effectResponsePage, String type) {
        if (effectResponsePage == null || effectResponsePage.getLinks() == null) {
            return null;
        }
        return getResponse(type, effectResponsePage.getLinks());
    }

    private String getResponse(String type, Page.Links links) {
        switch (type) {
            case LINK_NEXT:
                return links.getNext().getHref();
            case LINK_PREVIOUS:
                return links.getPrev().getHref();
            case LINK_CURRENT:
                return links.getSelf().getHref();
        }
        return null;
    }

    private boolean hasTrustlineForAsset(AccountResponse account, String assetCode, String assetIssuer) {
        return Arrays.stream(account.getBalances())
                .filter(item -> assetCode.equals(item.getAssetCode())
                        && assetIssuer.equals(item.getAssetIssuer().getAccountId()))
                .anyMatch(Objects::nonNull);
    }

    @Override
    public StellarOffer manageOffer(StellarAccount stellarAccount, StellarOffer offerDTO) throws AdapterException, IOException {
        try {
            KeyPair accountKeyPair = KeyPair.fromSecretSeed(stellarAccount.getAccountSeed());

            Long offerId = offerDTO.getOfferId() != null ? offerDTO.getOfferId() : 0;

            if (offerDTO.getSellingAsset() == null || offerDTO.getAcceptingAsset() == null) {
                throw new AdapterException("Invalid Manage Offer Operation. No SELL or BUY operation are present.", null);
            }

            if (StringUtils.isEmpty(offerDTO.getSellingAmount())) {
                offerDTO.setSellingAmount("0");
            }

            // SELLING ASSET
            Asset assetSelling = createAsset(offerDTO.getAcceptingAsset().getStellarAssetType(),
                    KeyPair.fromAccountId(offerDTO.getAcceptingAsset().getStellarAssetIssuer().getAccountId()),
                    offerDTO.getAcceptingAsset().getAssetCode());

            // BUYING ASSET
            Asset assetAccepted = createAsset(offerDTO.getSellingAsset().getStellarAssetType(),
                    KeyPair.fromAccountId(offerDTO.getSellingAsset().getStellarAssetIssuer().getAccountId()),
                    offerDTO.getSellingAsset().getAssetCode());

            // SELLING - VALIDATE ASSET ISSUER AND DISTRIBUTION
            if (offerDTO.getSellingAsset().getStellarAssetType().equals(StellarAssetType.CRYPTOBILIA)) {

                List<StellarOperation> stellarDistAssetOperations = new ArrayList<>();

                KeyPair distributionKeyPair = KeyPair.fromSecretSeed(offerDTO.getSellingAsset().getStellarAssetDistribution().getAccountSeed());
                AccountResponse distribution = getStellarServer().accounts().account(distributionKeyPair);

                if (!hasTrustlineForAsset(distribution,
                        offerDTO.getSellingAsset().getAssetCode(),
                        offerDTO.getSellingAsset().getStellarAssetIssuer().getAccountId())) {

                    StellarOperationItem assetTrustline = offerDTO.getSellingAsset();

                    if (assetTrustline.getStellarAssetIssuer() == null) {
                        throw new ServiceException("No issuer account for the asset: " + offerDTO.getSellingAsset().getAssetCode());
                    }
                    // TRUSTLINE (STANDALONE)
                    createTrustLine(
                            offerDTO.getSellingAsset().getStellarAssetDistribution(),
                            Arrays.asList(
                                    StellarOperationItem
                                            .builder()
                                            .stellarAssetType(assetTrustline.getStellarAssetType())
                                            .assetCode(assetTrustline.getAssetCode())
                                            .stellarAssetIssuer(assetTrustline.getStellarAssetIssuer())
                                            .amount(assetTrustline.getStellarAssetType().getLimit())
                                            .build()));
                }
            }

            List<StellarOperation> stellarTrustlineOperations = new ArrayList<>();
            AccountResponse account = getStellarServer().accounts().account(accountKeyPair);

            // BUYING - CHECK TRUSTLINE
            if (!hasTrustlineForAsset(
                    account,
                    offerDTO.getSellingAsset().getAssetCode(),
                    offerDTO.getSellingAsset().getStellarAssetIssuer().getAccountId())) {
                StellarOperationItem assetTrustline = offerDTO.getSellingAsset();

                stellarTrustlineOperations.addAll(
                        prepareCreateTrustLine(
                                stellarAccount, Arrays.asList(
                                        StellarOperationItem
                                                .builder()
                                                .stellarAssetType(assetTrustline.getStellarAssetType())
                                                .assetCode(assetTrustline.getAssetCode())
                                                .stellarAssetIssuer(assetTrustline.getStellarAssetIssuer())
                                                .amount(assetTrustline.getStellarAssetType().getLimit())
                                                .build()), 1));
            }

            ManageOfferOperation manageOfferOperation = new ManageOfferOperation.Builder(assetSelling, assetAccepted, offerDTO.getSellingAmount(), offerDTO.getSellingPrice())
                    .setSourceAccount(accountKeyPair)
                    .setOfferId(offerId)
                    .build();

            Transaction.Builder transactionBuilder = new Transaction.Builder(account);

            if (!CollectionUtils.isEmpty(stellarTrustlineOperations)) {
                stellarTrustlineOperations.forEach(item -> {
                    transactionBuilder.addOperation(item.getOperation());
                });
            }

            transactionBuilder.addOperation(manageOfferOperation);
            transactionBuilder.addMemo(Memo.text("MO " + offerDTO.getOfferType().getName().toUpperCase() + " ID: " + offerId));

            Transaction manageOfferTransaction = transactionBuilder.build();
            manageOfferTransaction.sign(accountKeyPair);
            SubmitTransactionResponse manageOfferOperationResponse = getStellarServer().submitTransaction(manageOfferTransaction);

            if (!manageOfferOperationResponse.isSuccess()) {
                throw logTransactionResponseAndPrepareAdapterException(manageOfferOperationResponse);
            }
            // OFFER ID FROM SINGLE/COMPOSED TRANSACTION
            Long stellarOfferId;
            int indexSingleOperationWithOfferId = 0;

            if (manageOfferOperationResponse.getOfferIdFromResult(indexSingleOperationWithOfferId) != null) {
                stellarOfferId = manageOfferOperationResponse.getOfferIdFromResult(indexSingleOperationWithOfferId);
                offerDTO.setOfferId(stellarOfferId);
                offerDTO.setOfferStatus(OfferStatus.IN_PROGRESS);

            } else {
                offerDTO = getOfferIdFromTransactionResult(manageOfferOperationResponse.getResultXdr(), offerDTO);
            }
            if (offerDTO.getOfferId() == null || offerDTO.getOfferId() == 0) {
                String offerErrorMessage = "Offer has been placed but there is no valid Offer ID.";
                throw new AdapterException(offerErrorMessage, null);
            }
            return offerDTO;

        } catch (ServiceException e) {
            throw new AdapterException(e.getMessage(), e);

        } catch (ErrorResponse e) {
            throw new AdapterException(e.getMessage(), e);

        } catch (SubmitTransactionTimeoutResponseException e) {
            throw new AdapterException(ApplicationConstants.STELLAR_TIMEOUT_ERROR, e);
        }
    }

    private StellarOffer getOfferIdFromTransactionResult(String transactionResultXdr, StellarOffer offerDTO) throws IOException {
        String operation = "getOfferIdFromTransactionResult";
        if (StringUtils.isEmpty(transactionResultXdr)) {
            return offerDTO;
        }
        XdrDataInputStream dataInputStreamFromBase64 = DataHelper.getDataInputStreamFromBase64(transactionResultXdr);
        TransactionResult transactionResult = TransactionResult.decode(dataInputStreamFromBase64);

        if (transactionResult == null) {
            return offerDTO;
        }

        OperationResult[] results = transactionResult.getResult().getResults();
        if (results == null) {
            return offerDTO;
        }
        return Arrays
                .stream(results)
                .filter(item -> item.getTr() != null)
                .filter(item -> item.getTr().getManageOfferResult() != null)
                .filter(item -> item.getTr().getManageOfferResult().getSuccess() != null)
                .map(item -> item.getTr().getManageOfferResult().getSuccess())
                .map(item -> {
                    if (item.getOffer() != null
                            && item.getOffer().getOffer() != null
                            && item.getOffer().getOffer().getOfferID() != null) {
                        return StellarOffer.builder()
                                .offerId(item.getOffer().getOffer().getOfferID().getUint64())
                                .offerStatus(OfferStatus.IN_PROGRESS)
                                .build();
                    } else {
                        if (item.getOffersClaimed() != null) {
                            Long offerId = Arrays.stream(item.getOffersClaimed())
                                    .filter(offer -> offer.getOfferID() != null)
                                    .map(offer -> offer.getOfferID().getUint64())
                                    .findFirst()
                                    .orElse(0L);

                            return StellarOffer.builder()
                                    .offerId(offerId)
                                    .offerStatus(OfferStatus.ACCEPTED)
                                    .build();
                        } else {
                            return null;
                        }
                    }
                }).findFirst().orElse(null);
    }

    private void logTransactionResultData(String operation, String transactionResultXdr) throws IOException {
        if (StringUtils.isEmpty(transactionResultXdr)) {
            //Invalid content for transactionResultXdr
            return;
        }
        try {
            XdrDataInputStream dataInputStreamFromBase64 = DataHelper.getDataInputStreamFromBase64(transactionResultXdr);
            TransactionResult transactionResult = TransactionResult.decode(dataInputStreamFromBase64);

            if (transactionResult != null && transactionResult.getFeeCharged() != null) {
                //LogHelper.logInfo(logger, operation, "STELLAR TRANSACTION COST: " + transactionResult.getFeeCharged().getInt64());
            }
        } catch (Exception e) {
            //LogHelper.logError(logger, operation, "Error while getting transactionResult data", e);
        }
    }

    @Override
    public void deleteOffer(StellarAccount stellarAccount, StellarOffer offerDTO) throws AdapterException, IOException {
        try {
            KeyPair accountKeyPair = KeyPair.fromSecretSeed(stellarAccount.getAccountSeed());

            if (offerDTO == null) {
                throw new AdapterException("Invalid Offer to delete. Offer not set", null);
            }

            if (offerDTO.getOfferId() == null || offerDTO.getOfferId() == 0) {
                throw new AdapterException("Invalid Offer Id to delete. Invalid ID: " + offerDTO.getOfferId(), null);
            }

            // DELETE OFFER PARAM
            String amount = "0";
            String price = "1";

            // SELLING ASSET
            Asset assetSelling = createAsset(offerDTO.getAcceptingAsset().getStellarAssetType(),
                    KeyPair.fromAccountId(offerDTO.getAcceptingAsset().getStellarAssetDistribution().getAccountId()),
                    offerDTO.getAcceptingAsset().getAssetCode());

            // BUYING ASSET
            Asset assetAccepted = createAsset(offerDTO.getSellingAsset().getStellarAssetType(),
                    KeyPair.fromAccountId(offerDTO.getSellingAsset().getStellarAssetDistribution().getAccountId()),
                    offerDTO.getSellingAsset().getAssetCode());

            ManageOfferOperation manageOfferOperation = new ManageOfferOperation.Builder(assetSelling, assetAccepted, amount, price)
                    .setSourceAccount(accountKeyPair)
                    .setOfferId(offerDTO.getOfferId())
                    .build();

            AccountResponse account = getStellarServer().accounts().account(accountKeyPair);

            Transaction manageOfferTransaction = new Transaction.Builder(account)
                    .addOperation(manageOfferOperation)
                    .addMemo(Memo.text("MO-DEL ID: " + offerDTO.getOfferId()))
                    .build();

            manageOfferTransaction.sign(accountKeyPair);

            SubmitTransactionResponse manageOfferOperationResponse = getStellarServer().submitTransaction(manageOfferTransaction);

            if (!manageOfferOperationResponse.isSuccess()) {
                throw logTransactionResponseAndPrepareAdapterException(manageOfferOperationResponse);
            }

        } catch (ServiceException e) {
            throw new AdapterException(e.getMessage(), e);

        } catch (ErrorResponse e) {
            throw new AdapterException(e.getMessage(), e);

        } catch (SubmitTransactionTimeoutResponseException e) {
            throw new AdapterException(ApplicationConstants.STELLAR_TIMEOUT_ERROR, e);
        }
    }

    //region Prepare Operations
    @Override
    public List<StellarOperation> prepareCreateTrustLine(StellarAccount destination, StellarAssetType stellarAssetType, String amount, int order) throws AdapterException, IOException {
        return prepareCreateTrustLine(configurationService.getConfiguration().getStellarCbbIssuerAccount(), destination, stellarAssetType, amount, order);
    }

    @Override
    public List<StellarOperation> prepareCreateTrustLine(StellarAccount source, StellarAccount destination, StellarAssetType stellarAssetType, String amount, int order) throws AdapterException, IOException {
        return prepareCreateTrustLine(destination, Arrays.asList(
                StellarOperationItem
                        .builder()
                        .stellarAssetType(stellarAssetType)
                        .stellarAssetIssuer(source)
                        .amount(amount)
                        .build())
                , order);
    }

    @Override
    public List<StellarOperation> prepareCreateTrustLine(StellarAccount destination, List<StellarOperationItem> stellarOperations, int order) throws AdapterException, IOException {
        String operation = "prepareCreateTrustLine";

        List<StellarOperation> operations = new ArrayList<>();

        KeyPair destinationKeyPair = KeyPair.fromSecretSeed(destination.getAccountSeed());

        String assetsSymbols = stellarOperations
                .stream()
                .filter(item -> item.getAssetCode() != null)
                .map(item -> item.getAssetCode())
                .collect(Collectors.joining(", "));

        AccountResponse destinationAccount = getStellarServer().accounts().account(destinationKeyPair);
        stellarOperations
                .stream()
                .filter(item ->
                        Arrays
                                .stream(destinationAccount.getBalances())
                                .filter(balance ->
                                        (balance != null
                                                && balance.getAssetCode() != null
                                                && balance.getAssetCode().equalsIgnoreCase(item.getAssetCode())
                                                && balance.getAssetIssuer().getAccountId().equalsIgnoreCase(item.getStellarAssetIssuer().getAccountId())))
                                .count() == 0
                )
                .forEach(item -> operations.add(
                        StellarOperation.builder()
                                .order(order)
                                .description("TRUSTLINE - " + item.getAssetCode())
                                .operation(
                                        new ChangeTrustOperation
                                                .Builder(createNonNativeAsset(item), item.getStellarAssetType().getLimit())
                                                .setSourceAccount(destinationKeyPair)
                                                .build()).build()));
        return operations;
    }

    @Override
    public List<StellarOperation> prepareRevokeTrustLine(StellarAccount destination, List<StellarOperationItem> stellarOperations, int order) throws AdapterException, IOException {

        List<StellarOperation> operations = new ArrayList<>();
        KeyPair destinationKeyPair = KeyPair.fromSecretSeed(destination.getAccountSeed());

        String assetsSymbols = stellarOperations
                .stream()
                .filter(item -> item.getAssetCode() != null)
                .map(item -> item.getAssetCode())
                .collect(Collectors.joining(", "));

        AccountResponse destinationAccount = getStellarServer().accounts().account(destinationKeyPair);

        stellarOperations
                .stream()
                .forEach(item -> operations.add(
                        StellarOperation.builder()
                                .order(order)
                                .description("REVOKE TL - " + item.getAssetCode())
                                .operation(
                                        new ChangeTrustOperation
                                                .Builder(Asset.createNonNativeAsset(item.getAssetCode(), KeyPair.fromAccountId(item.getStellarAssetIssuer().getAccountId())), "0")
                                                .setSourceAccount(destinationKeyPair)
                                                .build()).build()));
        return operations;
    }

    @Override
    public List<StellarOperation> preparePayment(StellarAccount source, StellarAccount destination, StellarAssetType stellarAssetType, String amount, int order) throws AdapterException, ServiceException, IOException {
        List<StellarOperation> operations = new ArrayList<>();

        KeyPair sourceKeyPair = KeyPair.fromSecretSeed(source.getAccountSeed());
        KeyPair destinationKeyPair = KeyPair.fromAccountId(destination.getAccountId());

        AccountResponse sourceAccount = getStellarServer().accounts().account(sourceKeyPair);
        Asset asset;
        if (stellarAssetType.equals(StellarAssetType.XLM)) {
            //XLM FROM SOURCE ACCOUNT
            asset = Asset.create(stellarAssetType.getName(), stellarAssetType.getSymbol(), configurationService.getConfiguration().getStellarSourceAccount().getAccountSeed());

        } else if (stellarAssetType.equals(StellarAssetType.CBB)) {
            // CBB FROM DIST ACCOUNT
            asset = Asset.createNonNativeAsset(stellarAssetType.getSymbol(), getStellarCbbIssuerAccount());

            //region funding CBB DIST ACCOUNT
            Double cbbDistributionAccountBalance = Arrays.stream(sourceAccount.getBalances())
                    .filter(item ->
                            StellarAssetType.CBB.getSymbol().equalsIgnoreCase(item.getAssetCode())
                                    && item.getAssetIssuer().getAccountId().equalsIgnoreCase(getStellarCbbIssuerAccount().getAccountId()))
                    .map(item -> item.getBalance())
                    .filter(Objects::nonNull)
                    .map(Double::valueOf)
                    .findFirst()
                    .orElse(0d);

            if (cbbDistributionAccountBalance < Double.valueOf(amount)) {
                if (!hasTrustlineForAsset(sourceAccount,
                        stellarAssetType.getSymbol(),
                        configurationService.getConfiguration().getStellarCbbIssuerAccount().getAccountId())) {
                    operations.addAll(prepareCreateTrustLine(source,
                            Arrays.asList(
                                    StellarOperationItem
                                            .builder()
                                            .stellarAssetType(stellarAssetType)
                                            .assetCode(stellarAssetType.getSymbol())
                                            .stellarAssetIssuer(configurationService.getConfiguration().getStellarCbbIssuerAccount())
                                            .amount(amount)
                                            .build()),
                            1));
                }
                KeyPair issuerPair = KeyPair.fromSecretSeed(configurationService.getConfiguration().getStellarCbbIssuerAccount().getAccountSeed());
                operations.add(StellarOperation
                        .builder()
                        .order(order)
                        .description("PAYMENT")
                        .operation(new PaymentOperation.Builder(sourceKeyPair, asset, amount)
                                .setSourceAccount(issuerPair)
                                .build())
                        .build());
            }
            //endregion
        } else {
            asset = Asset.createNonNativeAsset(stellarAssetType.getSymbol(), getStellarCbbIssuerAccount());
        }
        operations.add(StellarOperation
                .builder()
                .order((order + 1))
                .description("PAYMENT")
                .operation(
                        new PaymentOperation.Builder(destinationKeyPair, asset, amount)
                                .setSourceAccount(sourceKeyPair)
                                .build())
                .build());
        return operations;
    }

    @Override
    public List<StellarOperation> preparePayment(StellarAccount destination, List<StellarOperationItem> stellarOperations, boolean useIssuerAccount, int order) throws AdapterException, IOException {

        List<StellarOperation> operations = new ArrayList<>();
        KeyPair destinationKeyPair = KeyPair.fromAccountId(destination.getAccountId());
        String assetsSymbols = stellarOperations
                .stream()
                .filter(item -> item.getAssetCode() != null)
                .map(item -> item.getAssetCode())
                .collect(Collectors.joining(", "));

        stellarOperations.forEach(item ->
                operations.add(
                        StellarOperation
                                .builder()
                                .order(order)
                                .description("PAYMENT FOR: " + assetsSymbols)
                                .operation(
                                        new PaymentOperation.Builder(destinationKeyPair, createNonNativeAsset(item), item.getAmount())
                                                .setSourceAccount(KeyPair.fromSecretSeed(useIssuerAccount ? item.getStellarAssetIssuer().getAccountSeed() : item.getStellarAssetDistribution().getAccountSeed()))
                                                .build())
                                .build()));
        return operations;
    }

    @Override
    public List<StellarOperation> preparePayment(StellarAccount source, StellarAccount destination, StellarAssetType stellarAssetType, String amount, boolean firstDeposit, int order) throws AdapterException, ServiceException, IOException {
        List<StellarOperation> operations = new ArrayList<>();

        KeyPair sourceKeyPair = KeyPair.fromSecretSeed(source.getAccountSeed());
        KeyPair destinationKeyPair = KeyPair.fromAccountId(destination.getAccountId());

        AccountResponse sourceAccount = getStellarServer().accounts().account(sourceKeyPair);

        AccountResponse destinationAccount = getStellarServer().accounts().account(destinationKeyPair);

        Transaction.Builder builder;

        Asset asset;
        if (stellarAssetType.equals(StellarAssetType.XLM)) {
            //XLM FROM SOURCE ACCOUNT
            asset = Asset.create(stellarAssetType.getName(), stellarAssetType.getSymbol(), configurationService.getConfiguration().getStellarSourceAccount().getAccountSeed());

        } else if (stellarAssetType.equals(StellarAssetType.CBB)) {
            // CBB FROM DIST ACCOUNT
            asset = Asset.createNonNativeAsset(stellarAssetType.getSymbol(), getStellarCbbIssuerAccount());

            //region funding CBB DIST ACCOUNT
            Double cbbDistributionAccountBalance = Arrays.stream(sourceAccount.getBalances())
                    .filter(item ->
                            StellarAssetType.CBB.getSymbol().equalsIgnoreCase(item.getAssetCode())
                                    && item.getAssetIssuer().getAccountId().equalsIgnoreCase(getStellarCbbIssuerAccount().getAccountId()))
                    .map(item -> item.getBalance())
                    .filter(Objects::nonNull)
                    .map(Double::valueOf)
                    .findFirst().orElse(0d);

            if (cbbDistributionAccountBalance < Double.valueOf(amount)) {
                if (!hasTrustlineForAsset(sourceAccount,
                        stellarAssetType.getSymbol(),
                        configurationService.getConfiguration().getStellarCbbIssuerAccount().getAccountId())) {
                    operations.addAll(prepareCreateTrustLine(source,
                            Arrays.asList(
                                    StellarOperationItem
                                            .builder()
                                            .stellarAssetType(stellarAssetType)
                                            .assetCode(stellarAssetType.getSymbol())
                                            .stellarAssetIssuer(configurationService.getConfiguration().getStellarCbbIssuerAccount())
                                            .amount(amount)
                                            .build()), 1));
                }

                KeyPair issuerPair = KeyPair.fromSecretSeed(configurationService.getConfiguration().getStellarCbbIssuerAccount().getAccountSeed());
                operations.add(StellarOperation
                        .builder()
                        .order(order)
                        .description("PAYMENT")
                        .operation(new PaymentOperation.Builder(sourceKeyPair, asset, amount).setSourceAccount(issuerPair).build())
                        .build());
            }
            //endregion

            // TRUSTLINE CBB CHECK
            if (!hasTrustlineForAsset(destinationAccount,
                    stellarAssetType.getSymbol(),
                    configurationService.getConfiguration().getStellarCbbIssuerAccount().getAccountId())) {
                operations.addAll(prepareCreateTrustLine(destination,
                        Arrays.asList(
                                StellarOperationItem
                                        .builder()
                                        .stellarAssetType(stellarAssetType)
                                        .assetCode(stellarAssetType.getSymbol())
                                        .stellarAssetIssuer(configurationService.getConfiguration().getStellarCbbIssuerAccount())
                                        .amount(amount)
                                        .build()), 2));
            }

        } else {
            asset = Asset.createNonNativeAsset(stellarAssetType.getSymbol(), getStellarCbbIssuerAccount());
        }

        //sourceAccount = getStellarServer().accounts().account(sourceKeyPair);

        if (firstDeposit) {
            Asset firstLumenDeposit = Asset.create(StellarAssetType.XLM.getName(), StellarAssetType.XLM.getSymbol(), configurationService.getConfiguration().getStellarSourceAccount().getAccountSeed());
            String firstLumenDepositValue = configurationService.getConfiguration().getStellarAccountFirstDeposit();

            operations.add(StellarOperation
                    .builder()
                    .order(order)
                    .description("PAYMENT")
                    .operation(new PaymentOperation.Builder(destinationKeyPair, firstLumenDeposit, firstLumenDepositValue).setSourceAccount(sourceKeyPair).build())
                    .build());
        }
        operations.add(StellarOperation
                .builder()
                .order(order)
                .description("PAYMENT")
                .operation(new PaymentOperation.Builder(destinationKeyPair, asset, amount).setSourceAccount(sourceKeyPair).build())
                .build());

        return operations;
    }

    @Override
    public void submitStellarOperations(List<StellarOperation> stellarOperations) throws AdapterException, IOException {
        long channelAccountId = getChannelAccount();
        String operation = "submitStellarOperations AD" + addChannelAccountId(channelAccountId);
        try {
            KeyPair channelKeyPair = getChannelKeyPair(channelAccountId);
            AccountResponse channelAccount = getStellarServer().accounts().account(channelKeyPair);

            Transaction.Builder transactionBuilder = new Transaction.Builder(channelAccount);
            stellarOperations.forEach(item -> {
                transactionBuilder.addOperation(item.getOperation());
            });

            Transaction transaction = transactionBuilder.build();

            Set<String> accountIDs = stellarOperations
                    .stream()
                    .map(item -> item.getOperation().getSourceAccount().getAccountId())
                    .collect(Collectors.toSet());

            accountIDs.stream().map(accountId ->
                    stellarOperations
                            .stream()
                            .filter(item -> item.getOperation().getSourceAccount().getAccountId().equals(accountId))
                            .map(item -> item.getOperation().getSourceAccount())
                            .findFirst()
                            .get()
            ).forEach(item -> transaction.sign(item));

            transaction.sign(channelKeyPair);

            SubmitTransactionResponse paymentOperationResponse = getStellarServer().submitTransaction(transaction);
            logTransactionResultData(operation, paymentOperationResponse.getResultXdr());
            if (!paymentOperationResponse.isSuccess()) {
                throw logTransactionResponseAndPrepareAdapterException(paymentOperationResponse);
            }

        } catch (ServiceException e) {
            throw new AdapterException(e.getMessage(), e);

        } catch (ErrorResponse e) {
            throw new AdapterException(e.getMessage(), e);

        } catch (SubmitTransactionTimeoutResponseException e) {
            throw new AdapterException(ApplicationConstants.STELLAR_TIMEOUT_ERROR, e);

        } finally {
            releaseChannelAccount(channelAccountId);
        }
    }
}

