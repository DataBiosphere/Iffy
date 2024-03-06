package bio.terra.features;

import bio.terra.common.iam.SamUser;
import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.MutableContext;
import dev.openfeature.sdk.Structure;
import java.util.Map;

public class Contexts {
  /*
   * Generate an evaluation context for a target user
   *
   */
  public static EvaluationContext buildUserV1(SamUser samUser) {
    // TODO unclear how best to structure this yet
    MutableContext ctx = new MutableContext();
    ctx.add("kind", "user");
    ctx.add("version", "v1");
    ctx.add(
        "user",
        Structure.mapToStructure(
            Map.of(
                "email",
                samUser.getEmail(),
                "id",
                samUser.getSubjectId()
                )));
    ctx.setTargetingKey(samUser.getSubjectId());
    return ctx;
  }
}
