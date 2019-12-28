package net.smiguel.stellar.support.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Configuration extends BaseModel {

    private String stellarServerUrl;
    private boolean stellarPublicNetwork;
    private String stellarAccountInitFund;
    private String stellarAccountFirstDeposit;
    private BigDecimal stellarLumenRateInEuro;
    private StellarAccount stellarSourceAccount;
    private StellarAccount stellarInflationAccount;
    private StellarAccount stellarCbbDistributionAccount;
    private StellarAccount stellarCbbIssuerAccount;
    private StellarAccount stellarCbbEscrowAccount;
    private List<StellarAccount> stellarChannelAccounts;
}
