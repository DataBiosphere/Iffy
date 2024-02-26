package bio.terra.iffy.service;

import bio.terra.iffy.config.StatusCheckConfiguration;
import bio.terra.iffy.iam.SamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StatusService extends BaseStatusService {
  private static final Logger logger = LoggerFactory.getLogger(StatusService.class);

  @Autowired
  public StatusService(StatusCheckConfiguration configuration, SamService samService) {
    super(configuration);
    // TODO - check status of flagd sidecar
    registerStatusCheck("Sam", samService::status);
  }
}
