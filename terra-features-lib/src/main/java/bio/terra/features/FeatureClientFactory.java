package bio.terra.features;

import bio.terra.common.iam.BearerToken;
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.ImmutableContext;
import dev.openfeature.sdk.OpenFeatureAPI;
import org.springframework.stereotype.Component;

@Component
public class FeatureClientFactory {
  public FeatureClientFactory(IffyClient iffyClient, FeaturesConfiguration config) {
    OpenFeatureAPI api = OpenFeatureAPI.getInstance();
    // TODO we could set default system context here. (eg. "environment")
    IffyProvider provider = new IffyProvider(iffyClient, config);
    api.setProviderAndWait(provider);
  }

  public Client getClient(BearerToken bearerToken) {
    EvaluationContext ctx =
        new EvaluationContextWithBearerToken(new ImmutableContext(), bearerToken);
    Client client = OpenFeatureAPI.getInstance().getClient();
    client.setEvaluationContext(ctx);
    return client;
  }
}
