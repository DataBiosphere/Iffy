package bio.terra.features;

import bio.terra.iffy.api.FlagApi;
import bio.terra.iffy.client.ApiClient;
import io.opentelemetry.api.OpenTelemetry;
import java.util.Optional;
import jakarta.ws.rs.client.Client;

import org.springframework.stereotype.Component;

@Component
public class IffyClient {
  private final IffyConfiguration iffyConfig;
  private final Client httpClient;

  public IffyClient(IffyConfiguration iffyConfig, Optional<OpenTelemetry> openTelemetry) {
    this.iffyConfig = iffyConfig;

    // TODO - figure out how to correctly integrate opentelemetry with jersey http client
    this.httpClient = new ApiClient().getHttpClient();
  }

  private ApiClient getApiClient(String accessToken) {
    ApiClient apiClient = getApiClient();
    apiClient.setAccessToken(accessToken);
    return apiClient;
  }

  private ApiClient getApiClient() {
    return new ApiClient()
        .setHttpClient(this.httpClient)
        .setBasePath(this.iffyConfig.getBasePath());
  }

  FlagApi flagApi(String accessToken) {
    return new FlagApi(getApiClient(accessToken));
  }
}
