package bio.terra.features;

import bio.terra.common.iam.BearerToken;
import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.Value;
import java.util.Map;
import java.util.Set;

public class EvaluationContextWithBearerToken implements EvaluationContext {
  private final EvaluationContext context;
  private final BearerToken bearerToken;

  public EvaluationContextWithBearerToken(EvaluationContext context, BearerToken bearerToken) {
    this.context = context;
    this.bearerToken = bearerToken;
  }

  public EvaluationContext getContext() {
    return this.context;
  }

  public BearerToken getBearerToken() {
    return this.bearerToken;
  }

  @Override
  public String getTargetingKey() {
    return this.context.getTargetingKey();
  }

  @Override
  public EvaluationContext merge(EvaluationContext evaluationContext) {
    return new EvaluationContextWithBearerToken(
        this.context.merge(evaluationContext), this.bearerToken);
  }

  @Override
  public Set<String> keySet() {
    return this.context.keySet();
  }

  @Override
  public Value getValue(String s) {
    return this.context.getValue(s);
  }

  @Override
  public Map<String, Value> asMap() {
    return this.context.asMap();
  }

  @Override
  public Map<String, Object> asObjectMap() {
    return this.context.asObjectMap();
  }
}
