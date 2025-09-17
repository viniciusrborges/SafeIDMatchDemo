package com.safeweb.matchdemo;

import com.facetec.sdk.FaceTecCustomization;

public class Config {
    // Device Key gerada no portal (a do seu SampleApp funciona aqui também)
    public static String DeviceKeyIdentifier = "dX7HSGoMWPvcLSuORz64xKFIhFZhndnc";

    // Por enquanto deixamos a Testing API
    public static String YOUR_API_OR_FACETEC_TESTING_API_ENDPOINT =
            "https://api.facetec.com/api/v4/biometrics/process-request";

    // Customização opcional (pode ficar default no POC)
    public static FaceTecCustomization retrieveConfigurationWizardCustomization() {
        return new FaceTecCustomization();
    }
}
