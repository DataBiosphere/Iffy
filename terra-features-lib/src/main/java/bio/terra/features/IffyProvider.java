package bio.terra.features;

import bio.terra.iffy.api.FlagApi;
import bio.terra.iffy.client.ApiException;
import bio.terra.iffy.model.EvaluationRequest;
import bio.terra.iffy.model.EvaluationResultBool;
import bio.terra.iffy.model.EvaluationResultDouble;
import bio.terra.iffy.model.EvaluationResultInteger;
import bio.terra.iffy.model.EvaluationResultObject;
import bio.terra.iffy.model.EvaluationResultString;
import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.FeatureProvider;
import dev.openfeature.sdk.Hook;
import dev.openfeature.sdk.Metadata;
import dev.openfeature.sdk.ProviderEvaluation;
import dev.openfeature.sdk.ProviderState;
import dev.openfeature.sdk.Structure;
import dev.openfeature.sdk.Value;
import java.util.List;
import org.slf4j.LoggerFactory;

public class IffyProvider implements FeatureProvider {
  private final FeaturesConfiguration config;
  private final IffyClient iffyClient;

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(IffyProvider.class);

  private static final String TARGETING_KEY_FIELD_NAME = "targetingKey";

  public static class IffyProviderException extends RuntimeException {
    public IffyProviderException(Exception cause) {
      super(cause);
    }
    public IffyProviderException(String message, Throwable e) {
      super(message, e);
    }
  }

  public IffyProvider(IffyClient iffyClient, FeaturesConfiguration config) {
    this.iffyClient = iffyClient;
    this.config = config;
  }

  @FunctionalInterface
  interface IffyEvaluation<T> {
    ProviderEvaluation<T> apply(FlagApi flagApi, EvaluationRequest request) throws ApiException;
  }

  @Override
  public ProviderEvaluation<Boolean> getBooleanEvaluation(
      String flagName, Boolean defaultValue, EvaluationContext evaluationContext) {

    return evaluate(
        flagName,
        evaluationContext,
        (flagApi, request) -> {
          EvaluationResultBool iffyResult = flagApi.evaluateBool(request, flagName);

          ProviderEvaluation<Boolean> result = new ProviderEvaluation<>();
          result.setReason(iffyResult.getReason());
          result.setVariant(iffyResult.getVariant());
          // TODO isValue() should probably be called getValue() -- maybe tweak OpenAPI config?
          result.setValue(iffyResult.isValue());
          return result;
        });
  }

  @Override
  public ProviderEvaluation<String> getStringEvaluation(
      String flagName, String defaultValue, EvaluationContext evaluationContext) {
    return evaluate(
        flagName,
        evaluationContext,
        (flagApi, request) -> {
          EvaluationResultString iffyResult = flagApi.evaluateString(request, flagName);

          ProviderEvaluation<String> result = new ProviderEvaluation<>();
          result.setReason(iffyResult.getReason());
          result.setVariant(iffyResult.getVariant());
          result.setValue(iffyResult.getValue());
          return result;
        });
  }

  @Override
  public ProviderEvaluation<Integer> getIntegerEvaluation(
      String flagName, Integer defaultValue, EvaluationContext evaluationContext) {
    return evaluate(
        flagName,
        evaluationContext,
        (flagApi, request) -> {
          EvaluationResultInteger iffyResult = flagApi.evaluateInteger(request, flagName);

          ProviderEvaluation<Integer> result = new ProviderEvaluation<>();
          result.setReason(iffyResult.getReason());
          result.setVariant(iffyResult.getVariant());
          result.setValue(iffyResult.getValue());
          return result;
        });
  }

  @Override
  public ProviderEvaluation<Double> getDoubleEvaluation(
      String flagName, Double defaultValue, EvaluationContext evaluationContext) {
    return evaluate(
        flagName,
        evaluationContext,
        (flagApi, request) -> {
          EvaluationResultDouble iffyResult = flagApi.evaluateDouble(request, flagName);

          ProviderEvaluation<Double> result = new ProviderEvaluation<>();
          result.setReason(iffyResult.getReason());
          result.setVariant(iffyResult.getVariant());
          result.setValue(iffyResult.getValue());
          return result;
        });
  }

  @Override
  public ProviderEvaluation<Value> getObjectEvaluation(
      String flagName, Value defaultValue, EvaluationContext evaluationContext) {
    return evaluate(
        flagName,
        evaluationContext,
        (flagApi, request) -> {
          EvaluationResultObject iffyResult = flagApi.evaluateObject(request, flagName);

          ProviderEvaluation<Value> result = new ProviderEvaluation<>();
          result.setReason(iffyResult.getReason());
          result.setVariant(iffyResult.getVariant());
          result.setValue(new Value(Structure.mapToStructure(iffyResult.getValue())));
          return result;
        });
  }

  @Override
  public void initialize(EvaluationContext evaluationContext) throws Exception {
    FeatureProvider.super.initialize(evaluationContext);
  }

  @Override
  public void shutdown() {
    // nothing to do
    FeatureProvider.super.shutdown();
  }

  @Override
  public ProviderState getState() {
    // TODO - do we need to be more sophisticated about returning different states?
    return ProviderState.READY;
  }

  @Override
  public List<Hook> getProviderHooks() {
    return FeatureProvider.super.getProviderHooks();
  }

  @Override
  public Metadata getMetadata() {
    return null;
  }

  private <T> ProviderEvaluation<T> evaluate(
      String flagName, EvaluationContext ctx, IffyEvaluation<T> evaluation) {
    EvaluationContextWithBearerToken contextWithToken;
    try {
      contextWithToken = (EvaluationContextWithBearerToken) ctx;
    } catch (ClassCastException e) {
      String message = "all contexts passed to IffyProvider must be an EvaluationContextWithBearerToken";
      logger.error(message, e);
      throw new IffyProviderException(message, e);
    }

    FlagApi flagApi = iffyClient.flagApi(contextWithToken.getBearerToken().getToken());

    EvaluationRequest request = buildIffyRequest(contextWithToken.getContext());

    ProviderEvaluation<T> result;
    try {
      result = evaluation.apply(flagApi, request);
    } catch (ApiException e) {
      logger.error("Iffy API Exception: " + e.getCode(), e);
      throw new IffyProviderException(e);
    }

    return result;
  }

  private EvaluationRequest buildIffyRequest(EvaluationContext ctx) {
    EvaluationRequest request = new EvaluationRequest();

    bio.terra.iffy.model.EvaluationContext iffyContext =
        new bio.terra.iffy.model.EvaluationContext();

    iffyContext.putAll(ctx.asObjectMap());
    iffyContext.put(TARGETING_KEY_FIELD_NAME, ctx.getTargetingKey());
    request.setContext(iffyContext);
    return request;
  }
}
