package net.smiguel.stellar.support.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.stellar.sdk.Operation;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StellarOperation {

    private Operation operation;
    private int order;
    private String description;
}
