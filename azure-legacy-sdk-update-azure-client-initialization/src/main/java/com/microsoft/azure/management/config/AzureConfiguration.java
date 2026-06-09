package com.microsoft.azure.management.config;

/**
 * Centralized configuration class that reads Azure-related settings from
 * environment variables. Centralizing all configuration reads in one place
 * makes the application portable to Azure compute environments (App Service,
 * AKS, Container Apps, Azure Functions, etc.) and simplifies testing and
 * configuration management (rule azure-system-config-01000).
 *
 * <p>All environment variable names are defined as constants here, and are
 * read once at construction time. Downstream classes receive configuration
 * values via constructor injection rather than calling {@code System.getenv}
 * or {@code System.getProperty} directly.
 */
public class AzureConfiguration {

    /** The Azure subscription ID. Set via {@code AZURE_SUBSCRIPTION_ID}. */
    public static final String ENV_AZURE_SUBSCRIPTION_ID = "AZURE_SUBSCRIPTION_ID";

    /**
     * Optional CI/CD job name for resource-group tagging. Set via
     * {@code ENV_JOB_NAME}.
     */
    public static final String ENV_JOB_NAME = "ENV_JOB_NAME";

    private final String subscriptionId;
    private final String jobName;

    /**
     * Constructs an {@code AzureConfiguration} by reading from environment
     * variables. Call {@link #getInstance()} for the shared singleton, or
     * use this constructor in tests to supply a custom environment.
     */
    public AzureConfiguration() {
        this.subscriptionId = System.getenv(ENV_AZURE_SUBSCRIPTION_ID);
        this.jobName = System.getenv(ENV_JOB_NAME);
    }

    // -------------------------------------------------------------------------
    // Singleton
    // -------------------------------------------------------------------------

    private static final class Holder {
        private static final AzureConfiguration INSTANCE = new AzureConfiguration();
    }

    /**
     * Returns the application-wide singleton instance populated from the
     * process environment variables.
     *
     * @return the shared {@code AzureConfiguration} instance
     */
    public static AzureConfiguration getInstance() {
        return Holder.INSTANCE;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /**
     * Returns the Azure subscription ID configured via the
     * {@code AZURE_SUBSCRIPTION_ID} environment variable.
     *
     * @return the subscription ID, or {@code null} if not set
     */
    public String getSubscriptionId() {
        return subscriptionId;
    }

    /**
     * Returns the CI/CD job name configured via the {@code ENV_JOB_NAME}
     * environment variable. Used for resource-group tagging in test/automation
     * pipelines.
     *
     * @return the job name, or {@code null} if not set
     */
    public String getJobName() {
        return jobName;
    }
}
