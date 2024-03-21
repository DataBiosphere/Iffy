package bio.terra.iffy.service;

import bio.terra.iffy.config.FlagdConfiguration;
import bio.terra.iffy.model.EvaluationResult;
import bio.terra.iffy.model.EvaluationResultBool;
import bio.terra.iffy.model.EvaluationResultDouble;
import bio.terra.iffy.model.EvaluationResultInteger;
import bio.terra.iffy.model.EvaluationResultObject;
import bio.terra.iffy.model.EvaluationResultString;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Struct;
import com.google.protobuf.util.JsonFormat;
import dev.openfeature.contrib.providers.flagd.FlagdOptions;
import dev.openfeature.contrib.providers.flagd.resolver.common.ChannelBuilder;
import dev.openfeature.flagd.grpc.Schema;
import dev.openfeature.flagd.grpc.ServiceGrpc;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import java.io.Serial;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseStatus;

// FlagService evaluates feature flags using an in-process flagd resolver
//   https://github.com/open-feature/java-sdk-contrib/tree/main/providers/flagd#in-process-resolver
@Service
public class FlagService {

  @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR, reason = "Protobuf conversion failed")
  public static class ProtobufConversionException extends RuntimeException {
    @Serial private static final long serialVersionUID = 1L;

    public ProtobufConversionException(String message, Exception e) {
      super(message, e);
    }
  }

  private static final Logger logger = LoggerFactory.getLogger(FlagService.class);

  private final FlagEvaluationMetrics metrics = new FlagEvaluationMetrics();

  /*
   * Auto-wire the default Spring object mapper into this component
   * to ensure that we serialize and deserialize JSON with the same settings as
   * the spring boot API controllers
   * eg.
   * https://www.baeldung.com/spring-boot-customize-jackson-objectmapper#default-configuration
   *
   */
  private final ObjectMapper globalSpringObjectMapper;

  /*
   * synchronous flagd gRPC client
   */
  private final ServiceGrpc.ServiceBlockingStub flagdService;

  @Autowired
  public FlagService(FlagdConfiguration config, ObjectMapper globalSpringObjectMapper) {
    this.globalSpringObjectMapper = globalSpringObjectMapper;

    FlagdOptions options =
        FlagdOptions.builder().host(config.host()).port(config.port()).tls(false).build();

    io.grpc.ManagedChannel flagdChannel = ChannelBuilder.nettyChannel(options);
    this.flagdService = ServiceGrpc.newBlockingStub(flagdChannel);
  }

  public EvaluationResultBool evaluateBool(String flagName, Map<String, Object> ctx) {
    return instrumentFlagdRequest(
        flagName, FlagEvaluationMetrics.FlagType.BOOL, () -> this.doEvaluateBool(flagName, ctx));
  }

  public EvaluationResultInteger evaluateInteger(String flagName, Map<String, Object> ctx) {
    return instrumentFlagdRequest(
        flagName, FlagEvaluationMetrics.FlagType.INT, () -> this.doEvaluateInteger(flagName, ctx));
  }

  public EvaluationResultDouble evaluateDouble(String flagName, Map<String, Object> ctx) {
    return instrumentFlagdRequest(
        flagName,
        FlagEvaluationMetrics.FlagType.DOUBLE,
        () -> this.doEvaluateDouble(flagName, ctx));
  }

  public EvaluationResultString evaluateString(String flagName, Map<String, Object> ctx) {
    return instrumentFlagdRequest(
        flagName,
        FlagEvaluationMetrics.FlagType.STRING,
        () -> this.doEvaluateString(flagName, ctx));
  }

  public EvaluationResultObject evaluateObject(String flagName, Map<String, Object> ctx) {
    return instrumentFlagdRequest(
        flagName,
        FlagEvaluationMetrics.FlagType.OBJECT,
        () -> this.doEvaluateObject(flagName, ctx));
  }

  public Map<String, EvaluationResult> evaluateAllForContext(Map<String, Object> ctx) {
    Schema.ResolveAllRequest request =
        Schema.ResolveAllRequest.newBuilder().setContext(mapToStruct(ctx)).build();

    Schema.ResolveAllResponse response = this.flagdService.resolveAll(request);

    Map<String, EvaluationResult> result = new HashMap<>();
    for (Map.Entry<String, Schema.AnyFlag> entry : response.getFlagsMap().entrySet()) {
      String flagName = entry.getKey();
      Schema.AnyFlag flagValue = entry.getValue();

      logger.warn(
          "parsing flag evaluation result for "
              + flagName
              + "("
              + flagValue.getValueCase().name()
              + ")");

      EvaluationResult r;

      if (flagValue.hasBoolValue()) {
        EvaluationResultBool b = new EvaluationResultBool();
        b.setValue(flagValue.getBoolValue());
        r = b;
      } else if (flagValue.hasDoubleValue()) {
        EvaluationResultDouble d = new EvaluationResultDouble();
        d.setValue(flagValue.getDoubleValue());
        r = d;
      } else if (flagValue.hasStringValue()) {
        EvaluationResultString s = new EvaluationResultString();
        s.setValue(flagValue.getStringValue());
        r = s;
      } else if (flagValue.hasObjectValue()) {
        EvaluationResultObject o = new EvaluationResultObject();
        o.setValue(structToMap(flagValue.getObjectValue()));
        r = o;
      } else {
        // TODO make a real error type
        throw new RuntimeException("Unrecognized flag type: " + flagValue.getValueCase().name());
      }

      r.setVariant(flagValue.getVariant());
      r.setReason(flagValue.getReason());

      result.put(flagName, r);
    }

    // TODO instrument request
    return result;
  }

  private EvaluationResultBool doEvaluateBool(String flagName, Map<String, Object> ctx) {
    Schema.ResolveBooleanRequest request =
        Schema.ResolveBooleanRequest.newBuilder()
            .setFlagKey(flagName)
            .setContext(mapToStruct(ctx))
            .build();

    Schema.ResolveBooleanResponse response = this.flagdService.resolveBoolean(request);

    EvaluationResultBool result = new EvaluationResultBool();
    result.setValue(response.getValue());
    result.setVariant(response.getVariant());
    result.setReason(response.getReason());

    return result;
  }

  private EvaluationResultInteger doEvaluateInteger(String flagName, Map<String, Object> ctx) {
    Schema.ResolveIntRequest request =
        Schema.ResolveIntRequest.newBuilder()
            .setFlagKey(flagName)
            .setContext(mapToStruct(ctx))
            .build();

    Schema.ResolveIntResponse response = this.flagdService.resolveInt(request);

    EvaluationResultInteger result = new EvaluationResultInteger();
    result.setValue(Math.toIntExact(response.getValue()));
    result.setVariant(response.getVariant());
    result.setReason(response.getReason());

    return result;
  }

  private EvaluationResultDouble doEvaluateDouble(String flagName, Map<String, Object> ctx) {
    Schema.ResolveFloatRequest request =
        Schema.ResolveFloatRequest.newBuilder()
            .setFlagKey(flagName)
            .setContext(mapToStruct(ctx))
            .build();

    Schema.ResolveFloatResponse response = this.flagdService.resolveFloat(request);

    EvaluationResultDouble result = new EvaluationResultDouble();
    result.setValue(response.getValue());
    result.setVariant(response.getVariant());
    result.setReason(response.getReason());

    return result;
  }

  private EvaluationResultString doEvaluateString(String flagName, Map<String, Object> ctx) {
    Schema.ResolveStringRequest request =
        Schema.ResolveStringRequest.newBuilder()
            .setFlagKey(flagName)
            .setContext(mapToStruct(ctx))
            .build();

    Schema.ResolveStringResponse response = this.flagdService.resolveString(request);

    EvaluationResultString result = new EvaluationResultString();
    result.setValue(response.getValue());
    result.setVariant(response.getVariant());
    result.setReason(response.getReason());

    return result;
  }

  private EvaluationResultObject doEvaluateObject(String flagName, Map<String, Object> ctx) {
    Schema.ResolveObjectRequest request =
        Schema.ResolveObjectRequest.newBuilder()
            .setFlagKey(flagName)
            .setContext(mapToStruct(ctx))
            .build();

    Schema.ResolveObjectResponse response = this.flagdService.resolveObject(request);

    EvaluationResultObject result = new EvaluationResultObject();
    result.setVariant(response.getVariant());
    result.setValue(structToMap(response.getValue()));
    result.setReason(response.getReason());

    return result;
  }

  /*
   * Convert a protobuf struct to a Map<String, Object>
   */
  private Map<String, Object> structToMap(Struct struct) {
    // TODO here we serialize to JSON so we can deserialize into Java object... is there a better
    // way?
    String json;
    try {
      json = JsonFormat.printer().print(struct);
    } catch (InvalidProtocolBufferException e) {
      throw new ProtobufConversionException("failed to serialize protobuf struct to JSON", e);
    }
    TypeReference<HashMap<String, Object>> typeRef = new TypeReference<>() {};
    Map<String, Object> value;

    try {
      value = this.globalSpringObjectMapper.readValue(json, typeRef);
    } catch (JsonProcessingException e) {
      throw new ProtobufConversionException("failed to deserialize JSON into Map", e);
    }

    return value;
  }

  /*
   * Convert a Map<String, Object> to a protobuf Struct
   */
  private Struct mapToStruct(Map<String, Object> map) {
    // TODO here we serialize to JSON so we can deserialize into protobuf... is there a better way?
    String json;
    try {
      json = this.globalSpringObjectMapper.writeValueAsString(map);
    } catch (JsonProcessingException e) {
      throw new ProtobufConversionException("failed to serialize Map to JSON", e);
    }

    Struct.Builder b = Struct.newBuilder();
    try {
      JsonFormat.parser().merge(json, b);
    } catch (InvalidProtocolBufferException e) {
      throw new ProtobufConversionException("failed to deserialize JSON into protobuf struct", e);
    }
    return b.build();
  }

  private <T extends EvaluationResult> T instrumentFlagdRequest(
      String flagName, FlagEvaluationMetrics.FlagType flagType, Supplier<T> request) {
    long start = System.currentTimeMillis();

    try {
      T result = request.get();
      this.metrics.recordEvaluation(
          flagName, flagType, start - System.currentTimeMillis(), result.getVariant(), null);
      return result;
    } catch (RuntimeException e) {
      logger.error("error evaluating " + flagType.toString() + " flag " + flagName, e);
      this.metrics.recordEvaluation(flagName, flagType, start - System.currentTimeMillis(), "", e);
      throw e;
    }
  }

  private static class FlagEvaluationMetrics {
    enum Label {
      FLAG_TYPE("flag-type"),
      FLAG_NAME("flag-name"),
      FLAG_VARIANT("flag-variant"),
      OK("ok");
      private final String value;

      Label(final String s) {
        value = s;
      }

      public String toString() {
        return value;
      }
    }

    enum FlagType {
      BOOL("bool"),
      STRING("string"),
      INT("int"),
      DOUBLE("double"),
      OBJECT("object");
      private final String value;

      FlagType(final String s) {
        value = s;
      }

      public String toString() {
        return value;
      }
    }

    public static final String COUNTER_NAME = "flag.evaluation.counter";
    public static final String SUMMARY_NAME = "flag.evaluation.duration_ms";

    private void recordEvaluation(
        String flagName,
        FlagType flagType,
        long durationMillis,
        String variantName,
        RuntimeException e) {
      String okString = "true";
      if (e != null) {
        okString = "false";
      }
      List<Tag> tags =
          List.of(
              Tag.of(Label.FLAG_NAME.toString(), flagName),
              Tag.of(Label.FLAG_VARIANT.toString(), variantName),
              Tag.of(Label.FLAG_TYPE.toString(), flagType.toString()),
              Tag.of(Label.OK.toString(), okString));
      Metrics.globalRegistry.counter(COUNTER_NAME, tags).increment();
      Metrics.globalRegistry.summary(SUMMARY_NAME, tags).record(durationMillis);
    }
  }
}
