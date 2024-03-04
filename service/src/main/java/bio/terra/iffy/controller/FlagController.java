package bio.terra.iffy.controller;

import bio.terra.common.iam.SamUserFactory;
import bio.terra.iffy.api.FlagApi;
import bio.terra.iffy.config.SamConfiguration;
import bio.terra.iffy.model.EvaluationRequest;
import bio.terra.iffy.model.EvaluationResult;
import bio.terra.iffy.model.EvaluationResultBool;
import bio.terra.iffy.model.EvaluationResultDouble;
import bio.terra.iffy.model.EvaluationResultInteger;
import bio.terra.iffy.model.EvaluationResultObject;
import bio.terra.iffy.model.EvaluationResultString;
import bio.terra.iffy.service.FlagService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class FlagController implements FlagApi {
  private final FlagService flagService;
  private final SamUserFactory samUserFactory;
  private final SamConfiguration samConfiguration;
  private final HttpServletRequest request;

  public FlagController(
      FlagService flagService,
      SamUserFactory samUserFactory,
      SamConfiguration samConfiguration,
      HttpServletRequest request) {
    this.flagService = flagService;
    this.samUserFactory = samUserFactory;
    this.samConfiguration = samConfiguration;
    this.request = request;
  }

  @Override
  public ResponseEntity<EvaluationResultBool> evaluateBool(
      String flagName, EvaluationRequest body) {
    ensureAuthenticated();
    EvaluationResultBool result = this.flagService.evaluateBool(flagName, body.getContext());
    return ResponseEntity.ok(result);
  }

  @Override
  public ResponseEntity<EvaluationResultInteger> evaluateInteger(
      String flagName, EvaluationRequest body) {
    ensureAuthenticated();
    EvaluationResultInteger result = this.flagService.evaluateInteger(flagName, body.getContext());
    return ResponseEntity.ok(result);
  }

  @Override
  public ResponseEntity<EvaluationResultDouble> evaluateDouble(
      String flagName, EvaluationRequest body) {
    ensureAuthenticated();
    EvaluationResultDouble result = this.flagService.evaluateDouble(flagName, body.getContext());
    return ResponseEntity.ok(result);
  }

  @Override
  public ResponseEntity<EvaluationResultString> evaluateString(
      String flagName, EvaluationRequest body) {
    ensureAuthenticated();
    EvaluationResultString result = this.flagService.evaluateString(flagName, body.getContext());
    return ResponseEntity.ok(result);
  }

  @Override
  public ResponseEntity<EvaluationResultObject> evaluateObject(
      String flagName, EvaluationRequest body) {
    ensureAuthenticated();
    EvaluationResultObject result = this.flagService.evaluateObject(flagName, body.getContext());
    return ResponseEntity.ok(result);
  }

  @Override
  public ResponseEntity<Map<String, Object>> evaluateAll(EvaluationRequest body) {
    ensureAuthenticated();
    Map<String, EvaluationResult> result =
        this.flagService.evaluateAllForContext(body.getContext());

    Map<String, Object> m = new HashMap<>();
    for (String k : result.keySet()) {
      m.put(k, result.get(k));
    }

    return ResponseEntity.ok(m);
  }

  private void ensureAuthenticated() {
    // TODO easy way to move this to an interceptor / middleware?
    this.samUserFactory.from(request, samConfiguration.basePath());
  }
}
