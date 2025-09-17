package com.safeweb.matchdemo;

import com.facetec.sdk.FaceTecCustomization;

/** Config mínima só com o que precisamos no fluxo de testes. */
public class Config {
    // <<< COLOQUE A SUA Device Key (a mesma do SampleApp) >>>
    public static String DeviceKeyIdentifier = "REPLACE_WITH_YOUR_DEVICE_KEY_IDENTIFIER";

    // Em PROD troque para sua API; em DEV/POC mantenha a Testing API:
    public static String YOUR_API_OR_FACETEC_TESTING_API_ENDPOINT =
            "https://api.facetec.com/api/v4/biometrics/process-request";

    // Customization opcional — pode ser “vazia” no POC.
    public static FaceTecCustomization retrieveConfigurationWizardCustomization() {
        return new FaceTecCustomization();
    }
}
