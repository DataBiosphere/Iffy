package bio.terra.features;

public class FeaturesConfiguration {
  /** If false, always return the default value passed in by the caller. */
  private boolean enabled = false;

  /** How long to cache evaluated flags for a given context key before retrying. */
  private int cacheTtlSeconds = 180;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getCacheTtlSeconds() {
    return cacheTtlSeconds;
  }

  public void setCacheTtlSeconds(int cacheTtlSeconds) {
    this.cacheTtlSeconds = cacheTtlSeconds;
  }
}
