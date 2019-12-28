package net.smiguel.stellar.support.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.smiguel.stellar.support.enumerator.StellarAccountType;

import java.util.Calendar;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StellarAccount extends BaseModel {

    private StellarAccountType accountType;
    private String name;
    private String accountId;
    private String accountSeed;
    private Long ledger;
    private Calendar activationDate;
    private Calendar deactivationDate;
    private Long parentAccountId;
    private String appHostAddress;
    private List<StellarAccountBalance> balance;
    private StellarBalance balanceSummary;
}
