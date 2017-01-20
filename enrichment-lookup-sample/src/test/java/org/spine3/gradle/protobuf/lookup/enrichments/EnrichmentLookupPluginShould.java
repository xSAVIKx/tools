package org.spine3.gradle.protobuf.lookup.enrichments;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

/**
 * @author Dmytro Dashenkov
 */
public class EnrichmentLookupPluginShould {

    private static final Properties prop = new Properties();

    @BeforeClass
    public static void setUp() {
        final Properties properties = new Properties();
        try (InputStream input = new FileInputStream("generated/rest/resources/enrichments.properties")) {
            properties.load(input);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    public void find_enrichments() {
        assertFalse(prop.isEmpty());
    }

    private static void assertEnrichment(String enrichment, String event) {
        final String events = prop.getProperty(enrichment);
        assertThat(event, new IsContainedIn(events));
    }

    private static class IsContainedIn extends BaseMatcher<String> {

        private final String container;
        private String item;

        private IsContainedIn(String container) {
            this.container = container;
        }

        @Override
        public boolean matches(Object item) {
            final String value = (String) item;
            this.item = value;
            final boolean result = container.contains(value);
            return result;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(" given string does not contain ")
                       .appendText(item);
        }
    }
}
