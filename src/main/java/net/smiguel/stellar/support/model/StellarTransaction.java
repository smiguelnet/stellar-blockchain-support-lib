package net.smiguel.stellar.support.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StellarTransaction {

    private List<StellarTransactionItem> items;
    private String pagePrevious;
    private String pageCurrent;
    private String pageNext;
    private boolean moveBack;
    private boolean moveForward;
    private int requestPerAccount;
    private boolean hasNext;
}
