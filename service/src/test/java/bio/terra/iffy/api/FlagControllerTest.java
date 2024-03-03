package bio.terra.iffy.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import bio.terra.common.iam.BearerToken;
import bio.terra.common.iam.BearerTokenFactory;
import bio.terra.common.iam.SamUser;
import bio.terra.common.iam.SamUserFactory;
import bio.terra.iffy.config.SamConfiguration;
import bio.terra.iffy.controller.FlagController;
import bio.terra.iffy.iam.SamService;
import bio.terra.iffy.service.FlagService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@ContextConfiguration(classes = FlagController.class)
@WebMvcTest
public class FlagControllerTest {
  @MockBean FlagService serviceMock;
  @MockBean SamUserFactory samUserFactoryMock;
  @MockBean BearerTokenFactory bearerTokenFactory;
  @MockBean SamConfiguration samConfiguration;
  @MockBean SamService samService;

  @Autowired private MockMvc mockMvc;

  private SamUser testUser =
      new SamUser(
          "test@email",
          UUID.randomUUID().toString(),
          new BearerToken(UUID.randomUUID().toString()));

  @BeforeEach
  void beforeEach() {
    when(samUserFactoryMock.from(any(HttpServletRequest.class), any())).thenReturn(testUser);
  }

  @Test
  @Disabled("TODO")
  void testIncrementCounter() throws Exception {
    //    var meterRegistry = new SimpleMeterRegistry();
    //    Metrics.globalRegistry.add(meterRegistry);
    //
    //    try {
    //      final String tagValue = "tag_value";
    //      mockMvc
    //          .perform(
    //              post("/api/example/v1/counter")
    //                  .contentType(MediaType.APPLICATION_JSON)
    //                  .content(tagValue))
    //          .andExpect(status().isNoContent());
    //
    //      var counter =
    //          meterRegistry
    //              .find(FlagController.)
    //              .tags(FlagController.EXAMPLE_COUNTER_TAG, tagValue)
    //              .counter();
    //
    //      assertNotNull(counter);
    //      assertEquals(counter.count(), 1);
    //
    //    } finally {
    //      Metrics.globalRegistry.remove(meterRegistry);
    //    }
  }
}
