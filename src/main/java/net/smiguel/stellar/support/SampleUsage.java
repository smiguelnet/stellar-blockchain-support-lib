package net.smiguel.stellar.support;

import net.smiguel.stellar.support.adapter.StellarAdapter;
import net.smiguel.stellar.support.adapter.StellarAdapterImpl;

import java.io.IOException;

public class SampleUsage {
    public static void main(String[] args) throws IOException {
        System.out.println("Sample stellar support library usage");

        //Sample account creation
        StellarAdapter stellarAdapter = new StellarAdapterImpl();
        stellarAdapter.createAccount("smiguelnet", "account description");

        // TODO: 28/12/2019 Add other methods utilization
    }
}
