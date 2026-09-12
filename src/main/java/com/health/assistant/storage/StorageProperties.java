package com.health.assistant.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    public static final String TYPE_SUPABASE = "supabase";
    public static final String TYPE_LOCAL = "local";

    private String type = TYPE_SUPABASE;

    private Supabase supabase = new Supabase();

    private Local local = new Local();

    @Getter
    @Setter
    public static class Supabase {

        private String url;

        private String serviceRoleKey;

        private String bucketReports = "health-reports";

        private String bucketExports = "health-exports";
    }

    @Getter
    @Setter
    public static class Local {

        private String rootDir = "./data/storage";
    }
}
